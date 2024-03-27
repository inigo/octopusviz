package net.surguy.octopusviz

import com.typesafe.config.ConfigFactory
import net.surguy.octopusviz.retrieve.{GraphQlClient, MeterId, OctopusRetriever}
import org.specs2.mutable.Specification

import java.time.LocalDate

class EnergyUsageStorerIntegrationTest extends Specification with UsesConfig with UsesDatabase {

  private val config = ConfigFactory.load("application.conf")

  private val electricityId = MeterId(config.getString("octopus.electricity.meterPointNumber"), config.getString("octopus.electricity.serialNo"))
  private val gasId: MeterId = MeterId(config.getString("octopus.gas.meterPointNumber"), config.getString("octopus.gas.serialNo"))
  private val retriever = new OctopusRetriever(electricityId, gasId, config.getString("octopus.apiKey"))
  val graphQlClient = new GraphQlClient(apiKey)

  skipAll

  "retrieving the latest data" should {
    "populate the database" in {
      new EnergyUsageStorer(retriever, graphQlClient, db).retrieveAndStore()
      ok
    }
  }

  "retrieving todays consumption" should {
    "retrieve telemetry" in {
      new EnergyUsageStorer(retriever, graphQlClient, db).retrieveTelemetry(accountNumber)
      ok
    }
    "retrieve telemetry for specified time period" in {
      new EnergyUsageStorer(retriever, graphQlClient, db).retrieveTelemetryBetweenDays(accountNumber, LocalDate.of(2024, 3, 26), LocalDate.of(2024, 3, 26))
      ok
    }
  }

  "retrieve days between two dates" should {
    "return the start date through to the end date" in {
      EnergyUsageStorer.daysBetweenInclusive(LocalDate.of(2020, 1, 20), LocalDate.of(2020, 1, 23)) must
        beEqualTo(Seq(LocalDate.of(2020, 1, 20), LocalDate.of(2020, 1, 21), LocalDate.of(2020, 1, 22), LocalDate.of(2020, 1, 23)))
    }
    "return a single day if start and end are the same" in {
      EnergyUsageStorer.daysBetweenInclusive(LocalDate.of(2020, 1, 20), LocalDate.of(2020, 1, 20)) must beEqualTo(Seq(LocalDate.of(2020, 1, 20)))
    }
    "throw an exception if the end is before the start" in {
      EnergyUsageStorer.daysBetweenInclusive(LocalDate.of(2020, 1, 20), LocalDate.of(2019, 1, 20)) must throwAn[IllegalArgumentException]
    }
  }

}
