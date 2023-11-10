package net.surguy.octopusviz.http

import org.http4s.*
import org.http4s.Status.*
import org.specs2.mutable.Specification

class JsonDataRoutesTest extends Specification with Http4sTestHelpers {

  import JsonDataRoutes.*
  import org.http4s.implicits.uri

  import scala.language.implicitConversions

  "validating consumption query parameters" should {
    "accept a valid energy type" in {
      jsonDataRoutes.testGet(uri"/data/consumption?energyType=electricity").status must beEqualTo(Ok)
    }
    "reject an invalid energy type as a bad request" in {
      jsonDataRoutes.testGet(uri"/data/consumption?energyType=cheese").status must beEqualTo(BadRequest)
    }
    "reject a missing energy type as a not found" in {
      jsonDataRoutes.testGet(uri"/data/consumption").status must beEqualTo(NotFound)
    }
    "accept a valid start date" in {
      jsonDataRoutes.testGet(uri"/data/consumption?energyType=electricity&startDate=2023-02-01").status must beEqualTo(Ok)
    }
    "reject an invalid start date" in {
      jsonDataRoutes.testGet(uri"/data/consumption?energyType=electricity&startDate=cheese").status must beEqualTo(BadRequest)
    }.pendingUntilFixed("Currently throws an error")
  }

  "retrieving consumption data" should {
    "return JSON containing the energy type" in {
      jsonDataRoutes.testGet(uri"/data/consumption?energyType=electricity&startDate=2023-02-01").unsafeBodyText() must contain(""""energyType":"electricity"""")
    }
    "return JSON containing the start date" in {
      jsonDataRoutes.testGet(uri"/data/consumption?energyType=electricity&startDate=2023-02-01").unsafeBodyText() must contain(""""startDate":"2023-02-01"""")
    }
    "return JSON omitting the end date when none was supplied" in {
      jsonDataRoutes.testGet(uri"/data/consumption?energyType=electricity&startDate=2023-02-01").unsafeBodyText() must not(contain(""""endDate""""))
    }
  }


}

    "return data in the correct format for the Plotly graph" in {
      val response = jsonDataRoutes.testGet(uri"/data/consumption?energyType=electricity&startDate=2023-02-01").unsafeBodyText()
      val jsonData = parse(response).getOrElse(Json.Null)
      jsonData.asArray must beSome
      jsonData.asArray.get.forall(item => item.asObject.get.keys.toSet == Set("x", "y")) must beTrue
    }
