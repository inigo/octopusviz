package net.surguy.octopusviz.storage

import net.surguy.octopusviz.retrieve.Consumption
import net.surguy.octopusviz.utils.Logging
import net.surguy.octopusviz.{Electricity, UsesConfig, UsesDatabase}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneId}
import java.util.UUID

class DatabaseAccessTest(implicit ee: ExecutionEnv) extends Specification with UsesDatabase with Logging with UsesConfig {

  sequential
  import scala.language.implicitConversions

  "Doing things with the database" should {
    "store, retrieve and delete data" in {
      val future = LocalDateTime.now().plusYears(100).truncatedTo(ChronoUnit.MICROS) // Granularity of the datetime in Postgresql is only microseconds, not nanoseconds
      val consumption = Consumption(10.4, future.minusMinutes(30), future)
      val insertedId: UUID = db.storeConsumption(Electricity, List(consumption)).head
      try {
        db.listConsumption().head must beEqualTo(consumption)
      } finally {
        db.deleteConsumption(insertedId)
      }
    }
    "retrieve data between two datetimes" in {
      val future = LocalDateTime.now().plusYears(100).truncatedTo(ChronoUnit.MICROS) // Granularity of the datetime in Postgresql is only microseconds, not nanoseconds
      val consumption1 = Consumption(5, future.minusDays(3), future.minusDays(3).plusMinutes(30))
      val consumption2 = Consumption(6, future.minusDays(2), future.minusDays(2).plusMinutes(30))
      val consumption3 = Consumption(7, future.minusDays(1), future.minusDays(1).plusMinutes(30))
      val insertedIds = db.storeConsumption(Electricity, List(consumption1, consumption2, consumption3))
      try {
        db.listConsumption(Some(future.minusDays(3)), Some(future)) must containAllOf(Seq(consumption1, consumption2, consumption3))
        db.listConsumption(Some(future.minusDays(2)), Some(future)) must containAllOf(Seq(consumption2, consumption3))
        db.listConsumption(Some(future.minusDays(1)), Some(future)) must containAllOf(Seq(consumption3))
        db.listConsumption(Some(future.minusDays(0)), Some(future)) must haveLength(0)

        db.listConsumption(Some(future.minusDays(3)), Some(future.minusDays(1))) must containAllOf(Seq(consumption1, consumption2))
        db.listConsumption(Some(future.minusDays(3)), Some(future.minusDays(2))) must containAllOf(Seq(consumption1))

        db.listConsumption(Some(future.minusDays(3)), None) must containAllOf(Seq(consumption1, consumption2, consumption3))
        db.listConsumption(Some(future.minusDays(2)), None) must containAllOf(Seq(consumption2, consumption3))

        db.listConsumption(None, None).take(10) must beEqualTo(db.listConsumption().take(10))
      } finally {
        insertedIds.foreach(db.deleteConsumption)
      }
    }
    "return the latest interval date" in {
      val future = LocalDateTime.now().plusYears(100).truncatedTo(ChronoUnit.MICROS) // Granularity of the datetime in Postgresql is only microseconds, not nanoseconds
      val consumption = Consumption(10.4, future.minusMinutes(30), future)
      val insertedId: UUID = db.storeConsumption(Electricity, List(consumption)).head
      try {
        db.findMostRecentIntervalEnd(Electricity) must beSome(future.atZone(ZoneId.of("UTC")))
      } finally {
        db.deleteConsumption(insertedId)
      }
    }
  }

}
