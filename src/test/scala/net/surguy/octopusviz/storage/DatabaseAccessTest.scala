package net.surguy.octopusviz.storage

import com.zaxxer.hikari.HikariDataSource
import net.surguy.octopusviz.retrieve.Consumption
import net.surguy.octopusviz.utils.Logging
import net.surguy.octopusviz.{Electricity, UsesConfig}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeSpec
import org.specs2.specification.core.Fragments
import play.api.Configuration
import play.api.db.DBApi
import play.api.db.evolutions.OfflineEvolutions
import play.api.inject.guice.GuiceApplicationBuilder

import java.io.File
import java.time.{LocalDateTime, ZoneId}
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class DatabaseAccessTest(implicit ee: ExecutionEnv) extends Specification with BeforeSpec with Logging with UsesConfig {

  sequential
  import scala.language.implicitConversions

  private val dataSource = HikariDataSource(dbConfig)
  private val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  private val db = new DatabaseAccess(dataSource, ec)

  override def beforeSpec: Fragments = {
    val s = step( { Await.result(ApplyEvolutions.from(Configuration(config)), 5.seconds) must not(throwAn[Exception]) } )
    Fragments(s)
  }

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

object ApplyEvolutions extends App with Logging {
  def from(config: play.api.Configuration) = {
    val app = new GuiceApplicationBuilder().configure(config).build()
    val dbApi = app.injector.instanceOf[DBApi]
    dbApi.databases().foreach { db =>
      log.info("Applying evolutions")
      OfflineEvolutions.applyScript(new File("."), this.getClass.getClassLoader, dbApi, db.name)
    }
    app.stop()
  }
}