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
import net.surguy.octopusviz.retrieve.{Consumption, Telemetry}

import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
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

  def storeTelemetry(telemetry: List[Telemetry]): Seq[UUID] = {
    telemetry.map { c =>
      sql"insert into telemetry (read_at, consumption_delta, demand) values (${c.readAt}, ${c.consumptionDelta}, ${c.demand})".
        update.withUniqueGeneratedKeys[UUID]("id")
    }.traverse(identity).transact(xa).unsafeRunSync()
  }

  def listConsumption(): Seq[Consumption] = {
    sql"select consumption, interval_start, interval_end from consumption order by interval_start desc".query[Consumption].to[List].transact(xa).unsafeRunSync()
  }
  def listConsumption(startDate: Option[LocalDateTime], endDate: Option[LocalDateTime]): Seq[Consumption] = {
    val filters = List(
      startDate.map(d => fr"interval_start >= $d"),
      endDate.map(d => fr"interval_end <= $d"),
    ).flatten
    val whereClause = filters.reduceOption((a, b) => a ++ fr"AND" ++ b)
    (fr"select consumption, interval_start, interval_end from consumption"
      ++ whereClause.fold(Fragment.empty)(fr"WHERE" ++ _)
      ++ fr"order by interval_start desc")
      .query[Consumption].to[List].transact(xa).unsafeRunSync()
  }

  def listTelemetry(): Seq[Telemetry] = {
    sql"select read_at, consumption_delta, demand from telemetry order by read_at desc".query[Telemetry].to[List].transact(xa).unsafeRunSync()
  }
  def listTelemetry(startDate: Option[LocalDateTime], endDate: Option[LocalDateTime]): Seq[Telemetry] = {
    val filters = List(
      startDate.map(d => fr"read_at >= $d"),
      endDate.map(d => fr"read_at < $d"),
    ).flatten
    val whereClause = filters.reduceOption((a, b) => a ++ fr"AND" ++ b)
    (fr"select read_at, consumption_delta, demand from telemetry"
      ++ whereClause.fold(Fragment.empty)(fr"WHERE" ++ _)
      ++ fr"order by read_at desc")
      .query[Telemetry].to[List].transact(xa).unsafeRunSync()
  }

  def averageTelemetry(startDate: LocalDateTime, endDate: LocalDateTime, bucketIntervalMinutes: Int): Seq[Telemetry] = {
    val sql = sql"""
       WITH params AS (
            SELECT
                $startDate AS period_start,
                $endDate AS period_end,
                $bucketIntervalMinutes AS bucket_interval_minutes
        ),
             buckets AS (
                 SELECT generate_series(
                          p.period_start, 
                          p.period_end - (p.bucket_interval_minutes * interval '1 minute'), 
                          p.bucket_interval_minutes * interval '1 minute')
                     AS bucket_start, p.bucket_interval_minutes
                 FROM params p
             )
        SELECT
            b.bucket_start as read_at,
            0 as consumption_delta,
            COALESCE(ROUND(AVG(t.demand)::numeric, 1), 0) AS demand
        FROM buckets b
                 LEFT JOIN telemetry t
                           ON t.read_at >= b.bucket_start
                               AND t.read_at <  b.bucket_start + (b.bucket_interval_minutes * interval '1 minute')
        GROUP BY b.bucket_start
        ORDER BY b.bucket_start;
       """
      sql.query[Telemetry].to[List].transact(xa).unsafeRunSync()
  }

  def deleteConsumption(id: UUID): Int = {
    sql"delete from consumption where id = $id".update.run.transact(xa).unsafeRunSync()
  }
  def deleteTelemetry(id: UUID): Int = {
    sql"delete from telemetry where id = $id".update.run.transact(xa).unsafeRunSync()
  }

  def findMostRecentIntervalEnd(energyType: EnergyType): Option[ZonedDateTime] = {
    sql"select interval_end from consumption where energy_type=$energyType order by interval_end desc limit 1".query[LocalDateTime].option.transact(xa).unsafeRunSync().map(_.atZone(utc))
  }

  def findMostRecentReadAt(): Option[ZonedDateTime] = {
    sql"select read_at from telemetry order by read_at desc limit 1".query[LocalDateTime].option.transact(xa).unsafeRunSync().map(_.atZone(utc))
  }

}
