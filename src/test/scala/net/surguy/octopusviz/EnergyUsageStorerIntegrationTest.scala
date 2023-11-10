package net.surguy.octopusviz

import com.typesafe.config.ConfigFactory
import net.surguy.octopusviz.retrieve.{MeterId, OctopusRetriever}
import org.specs2.mutable.Specification

class EnergyUsageStorerIntegrationTest extends Specification with UsesConfig with UsesDatabase {

  private val config = ConfigFactory.load("application.conf")

  private val electricityId = MeterId(config.getString("octopus.electricity.meterPointNumber"), config.getString("octopus.electricity.serialNo"))
  private val gasId: MeterId = MeterId(config.getString("octopus.gas.meterPointNumber"), config.getString("octopus.gas.serialNo"))
  private val retriever = new OctopusRetriever(electricityId, gasId, config.getString("octopus.apiKey"))

  "retrieving the latest data" should {
    "populate the database" in {
      new EnergyUsageStorer(retriever, db).retrieveAndStore()
      ok
    }
  }

}
