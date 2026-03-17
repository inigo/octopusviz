package net.surguy.octopusviz.retrieve

import io.circe.Json
import io.circe.generic.auto.*
import io.circe.parser.*
import net.surguy.octopusviz.utils.Logging
import sangria.ast.Document
import sangria.parser.QueryParser
import sangria.renderer.QueryRenderer
import sttp.client3.*
import sttp.model.MediaType

import java.io.IOException
import java.time.{Instant, LocalDateTime, ZoneOffset, ZonedDateTime}

class GraphQlClient(apiKey: String) extends Logging {

  private val baseUrl = "https://api.octopus.energy/v1/graphql/"
  private val backend = HttpClientSyncBackend()
  private var accessToken: Option[String] = None
  private var tokenExpiration: Option[Instant] = None

  private val obtainAccessTokenQuery: Document =
    QueryParser.parse(s"""
      mutation {
        obtainKrakenToken(input: { APIKey: "$apiKey" }) {
          token
        }
      }
    """).get

  private def refreshAccessToken(): Unit = {
    val response = sendRequest(obtainAccessTokenQuery, omitAccessToken = true)
    val json = parse(response).getOrElse(Json.Null)
    val accessToken = json.hcursor.downField("data").downField("obtainKrakenToken").downField("token").as[String].getOrElse("")
    val tokenExpiration = Instant.now().plusSeconds(3600) // Token is valid for 1 hour
    this.accessToken = Some(accessToken)
    this.tokenExpiration = Some(tokenExpiration)
  }

  private def sendRequest(query: Document, omitAccessToken: Boolean = false): String = {
    if (!omitAccessToken && (accessToken.isEmpty || tokenExpiration.exists(_.isBefore(Instant.now())))) {
      refreshAccessToken()
    }

    val request = if (omitAccessToken) basicRequest else basicRequest.header("Authorization", s"${accessToken.get}")
    val queryBody = QueryRenderer.renderPretty(query)

    val response: Identity[Response[Either[String, String]]] = request
      .post(uri"$baseUrl")
      .contentType(MediaType.ApplicationJson)
      .body(Json.obj("query" -> Json.fromString(queryBody)).toString)
      .send(backend)

    response.body match {
      case Right(body) => body
      case Left(error) => throw new IOException(s"Request failed: $error")
    }
  }

  def getMeterIds(accountNumber: String): MeterIds = {
    val deviceIdsQuery =
      QueryParser.parse(s"""
      query {
        account(accountNumber: "${accountNumber}") {
          electricityAgreements(active: true) {
            meterPoint {
              meters(includeInactive: false) {
                smartImportElectricityMeter {
                  deviceId
                }
                smartExportElectricityMeter {
                  deviceId
                }
              }
            }
          }
          gasAgreements(active: true) {
            meterPoint {
              meters(includeInactive: false) {
                smartGasMeter {
                  deviceId
                }
              }
            }
          }
        }
      }
    """).get

    val response = sendRequest(deviceIdsQuery)
    val json: Json = parse(response).getOrElse(Json.Null)
    parseDeviceIds(json)
  }

  private[retrieve] def parseDeviceIds(deviceIdResponse: Json): MeterIds = {
    val account = deviceIdResponse.hcursor.downField("data").downField("account")
    val electricityId = account.downField("electricityAgreements").downArray.downField("meterPoint").downField("meters").downArray.downField("smartImportElectricityMeter").downField("deviceId").as[String].toOption
    val gasId = account.downField("gasAgreements").downArray.downField("meterPoint").downField("meters").downArray.downField("smartGasMeter").downField("deviceId").as[String].toOption

    MeterIds(electricityId, gasId)
  }


  def getElectricityConsumption(deviceId: String, startTime: LocalDateTime, endTime: LocalDateTime): List[Telemetry] = {
    val start = startTime.atZone(ZoneOffset.UTC).toInstant.toString
    val end = endTime.atZone(ZoneOffset.UTC).toInstant.toString

    val query =
      QueryParser.parse(s"""
      query {
        smartMeterTelemetry(
          deviceId: "${deviceId}",
          grouping: ONE_MINUTE,
          start: "${start}",
          end: "${end}"
        ) {
          readAt
          consumptionDelta
          demand
        }
      }
    """).get

    val response = sendRequest(query)
    log.debug(s"Received response: $response")
    val json = parse(response).getOrElse(Json.Null)
    parseTelemetry(json).map(_.toTelemetry)
  }

  private[retrieve] def parseTelemetry(telemetryResponse: Json) = {
    telemetryResponse.hcursor.downField("data").downField("smartMeterTelemetry").as[List[TelemetryRaw]] match {
      case Right(t) => t
      case Left(error) => throw new IOException("Failed to parse with "+error)
    }
  }
}

case class TelemetryRaw(readAt: String, consumptionDelta: String, demand: String) {
  def toTelemetry: Telemetry = Telemetry(ZonedDateTime.parse(readAt).toLocalDateTime, BigDecimal(consumptionDelta).doubleValue, demand.toDouble)
}
case class Telemetry(readAt: LocalDateTime, consumptionDelta: Double, demand: Double)

case class MeterIds(electricityDeviceId: Option[String], gasDeviceId: Option[String])
