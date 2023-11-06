package net.surguy.octopusviz.http

import org.http4s.*
import org.http4s.Status.*
import org.specs2.mutable.Specification

class ConsumptionRoutesTest extends Specification with Http4sTestHelpers {

  import ConsumptionRoutes.*

  import scala.language.implicitConversions

  "calling the consumption service" should {
    "display the expected text" in {
      val response = consumptionRoutes.testGet("/consumption")
      response.status must beEqualTo(Ok)
      val bodyText = response.unsafeBodyText()
      bodyText must contain("Energy consumption")
    }
    "redirect from home to display consumption" in {
      val response = consumptionRoutes.testGet("/")
      response.status must beEqualTo(TemporaryRedirect)
      response.headerValue("Location") must beSome("/consumption")
    }
  }

}
