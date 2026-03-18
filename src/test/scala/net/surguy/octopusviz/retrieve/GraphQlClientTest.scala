package net.surguy.octopusviz.retrieve

import net.surguy.octopusviz.UsesConfig
import org.specs2.mutable.Specification

import java.time.LocalDateTime


class GraphQlClientTest extends Specification with UsesConfig {

  val client = new GraphQlClient(apiKey)
  import io.circe.parser.*

  "parsing JSON responses" should {
    "extract the meter device IDs" in {
      val jsonString: String =
        """
      {
        "data": {
          "account": {
            "electricityAgreements": [
              {
                "meterPoint": {
                  "meters": [
                    {
                      "smartImportElectricityMeter": {
                        "deviceId": "11-22-33-FF-AA-BB-CC-DD"
                      },
                      "smartExportElectricityMeter": null
                    }
                  ]
                }
              }
            ],
            "gasAgreements": [
              {
                "meterPoint": {
                  "meters": [
                    {
                      "smartGasMeter": {
                        "deviceId": "D1-E1-F1-00-11-22-33-44"
                      }
                    }
                  ]
                }
              }
            ]
          }
        }
      }
      """
      client.parseDeviceIds(parse(jsonString).toOption.get) must beEqualTo(MeterIds(Some("11-22-33-FF-AA-BB-CC-DD"), Some("D1-E1-F1-00-11-22-33-44")))
    }
    "extract the telemetry" in {
      val jsonString =
        """
          |{
          |  "data": {
          |    "smartMeterTelemetry": [
          |      {
          |        "readAt": "2024-03-20T17:00:00+00:00",
          |        "consumptionDelta": "0E+1",
          |        "demand": "624.0"
          |      },
          |      {
          |        "readAt": "2024-03-20T17:00:10+00:00",
          |        "consumptionDelta": "0E+1",
          |        "demand": "630.0"
          |      },
          |      {
          |        "readAt": "2024-03-20T17:00:20+00:00",
          |        "consumptionDelta": "0E+1",
          |        "demand": "519.0"
          |      },
          |      {
          |        "readAt": "2024-03-20T17:00:30+00:00",
          |        "consumptionDelta": "0E+1",
          |        "demand": "532.0"
          |      },
          |      {
          |        "readAt": "2024-03-20T17:00:50+00:00",
          |        "consumptionDelta": "0E+1",
          |        "demand": "525.0"
          |      }
          |    ]
          |  }
          |}
          |""".stripMargin
      val telemetry = client.parseTelemetry(parse(jsonString).toOption.get)
      telemetry must haveLength(5)
      telemetry.head must beEqualTo(TelemetryRaw("2024-03-20T17:00:00+00:00", "0E+1", "624.0"))
    }
  }

  "calling the service" should {
    "return real meter IDs" in {
      val meterIds = client.getMeterIds(accountNumber)
      meterIds.electricityDeviceId must beSome[String]
      println(meterIds)
    }
    "return real values" in {
      val meterIds = client.getMeterIds(accountNumber)
      val deviceId = meterIds.electricityDeviceId.get
      val now = LocalDateTime.now().withSecond(0).withNano(0)
      val result = client.getElectricityConsumption(deviceId, now.minusMinutes(5), now.minusMinutes(4))
      println(result)
      result must haveLength(1) // This depends on the granularity of the query - 10s would be 6, 1 minute would be 1
    }
  }

}
