package net.surguy.octopusviz.retrieve

import net.surguy.octopusviz.UsesConfig
import org.specs2.mutable.Specification

class OctopusRetrieverIntegrationTest extends Specification with UsesConfig {

  private val retriever = new OctopusRetriever(electricityId, gasId, apiKey)
  
  "Retrieving energy data" should {
    "return some text" in {
      retriever.fetchEnergyData("electricity", electricityId) must beSome
    }
    "parse the returned JSON" in {
      retriever.fetchEnergyData("electricity", electricityId).map(retriever.parseResults) must beSome
    }
// Hits the Octopus API repeatedly, and is slow, so commented out to be polite
//    "retrieve all energy data, following the 'next' links" in {
//      val allResults = retriever.fetchAllElectricityData()
//      println(allResults.map(_.minBy(_.intervalStart)))
//      println(allResults.map(_.maxBy(_.intervalStart)))
//      allResults must beSome(haveLength(greaterThan(3000)))
//    }
  }

  "Parsing Octopus JSON data" should {
    val json =
      """
        |{"count":12345,
        |"next":"https://api.octopus.energy/v1/electricity-meter-points/123456/meters/01P0123456/consumption/?page=2",
        |"previous":null,
        |"results":[
        |   {"consumption":0.023,"interval_start":"2023-10-01T00:30:00+01:00","interval_end":"2023-10-01T01:00:00+01:00"},
        |   {"consumption":0.043,"interval_start":"2023-10-01T00:00:00+01:00","interval_end":"2023-10-01T00:30:00+01:00"}
        |]}""".stripMargin

    "parse sample JSON" in {
      retriever.parseResults(json) must beSome
    }
  }

}
