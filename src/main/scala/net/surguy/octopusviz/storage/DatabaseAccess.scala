package net.surguy.octopusviz.storage

import cats.*
import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import net.surguy.octopusviz.EnergyType
import net.surguy.octopusviz.retrieve.Consumption

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID
import javax.sql.DataSource
import scala.concurrent.ExecutionContext

class DatabaseAccess(dataSource: DataSource, executionContext: ExecutionContext) {

  private val xa = Transactor.fromDataSource[IO](dataSource, executionContext)
  private val utc = ZoneId.of("UTC")

  implicit val energyTypePut: Put[EnergyType] = Put[String].tcontramap(_.name)

  def storeConsumption(energyType: EnergyType, consumption: List[Consumption]): Seq[UUID] = {
    consumption.map { c =>
      sql"insert into consumption (interval_start, interval_end, consumption, energy_type) values (${c.intervalStart}, ${c.intervalEnd}, ${c.consumption}, $energyType)".
        update.withUniqueGeneratedKeys[UUID]("id")
    }.traverse(identity).transact(xa).unsafeRunSync()
  }

  def listConsumption(): Seq[Consumption] = {
    sql"select consumption, interval_start, interval_end from consumption order by interval_start desc".query[Consumption].to[List].transact(xa).unsafeRunSync()
  }

  def deleteConsumption(id: UUID): Int = {
    sql"delete from consumption where id = $id".update.run.transact(xa).unsafeRunSync()
  }

  def findMostRecentIntervalEnd(energyType: EnergyType): Option[ZonedDateTime] = {
    sql"select interval_end from consumption where energy_type=$energyType order by interval_end desc limit 1".query[LocalDateTime].option.transact(xa).unsafeRunSync().map(_.atZone(utc))
  }

}
