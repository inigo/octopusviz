package net.surguy.octopusviz

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariDataSource
import net.surguy.octopusviz.retrieve.{MeterId, OctopusRetriever}
import net.surguy.octopusviz.storage.DatabaseAccess
import org.specs2.mutable.Specification

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class EnergyUsageStorerIntegrationTest extends Specification with UsesConfig {

  private val config = ConfigFactory.load("application.conf")

  private val electricityId = MeterId(config.getString("octopus.electricity.meterPointNumber"), config.getString("octopus.electricity.serialNo"))
  private val gasId: MeterId = MeterId(config.getString("octopus.gas.meterPointNumber"), config.getString("octopus.gas.serialNo"))
  private val retriever = new OctopusRetriever(electricityId, gasId, config.getString("octopus.apiKey"))

  private val dataSource = HikariDataSource(dbConfig)
  private val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  private val db = new DatabaseAccess(dataSource, ec)

  "retrieving the latest data" should {
    "populate the database" in {
      new EnergyUsageStorer(retriever, db).retrieveAndStore()
      ok
    }
  }

}
