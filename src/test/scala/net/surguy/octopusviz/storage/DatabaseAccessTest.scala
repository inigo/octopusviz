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
      val countBefore = db.listConsumption().length
      val future = LocalDateTime.now().plusYears(100).truncatedTo(ChronoUnit.MICROS) // Granularity of the datetime in Postgresql is only microseconds, not nanoseconds
      val consumption = Consumption(10.4, future.minusMinutes(30), future)
      val insertedId: UUID = db.storeConsumption(Electricity, List(consumption)).head
      try {
        val countAfter = db.listConsumption().length
        (countAfter - 1) must beEqualTo(countBefore)
        db.listConsumption().head must beEqualTo(consumption)
      } finally {
        db.deleteConsumption(insertedId)
        db.listConsumption().length must beEqualTo(countBefore)
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
