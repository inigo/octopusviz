package net.surguy.octopusviz.http

import cats.effect.*
import io.circe.generic.auto.*
import net.surguy.octopusviz.*
import net.surguy.octopusviz.storage.DatabaseAccess
import org.http4s.HttpRoutes
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.Http4sServerInterpreter

import java.time.LocalDate

class IngestionDataRoutes(dbAccess: DatabaseAccess, energyUsageStorer: EnergyUsageStorer, accountNumber: String):
  import TapirCodecs.given

  case class IngestionResponse(count: Int)

  private val ingestAll = endpoint.post
    .in("ingest")
    .out(jsonBody[IngestionResponse])
    .description("Ingest all available telemetry data")

  private val ingestDay = endpoint.post
    .in("ingest" / path[LocalDate]("date"))
    .out(jsonBody[IngestionResponse])
    .description("Ingest telemetry for a specific date")

  private val ingestRange = endpoint.post
    .in("ingest" / path[LocalDate]("startDate") / path[LocalDate]("endDate"))
    .out(jsonBody[IngestionResponse])
    .description("Ingest telemetry for a date range")

  val allEndpoints: List[AnyEndpoint] = List(ingestAll, ingestDay, ingestRange)

  val ingestionDataRoutes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(List(
    ingestAll.serverLogicSuccess[IO] { _ =>
      val ingestedData = energyUsageStorer.retrieveTelemetry(accountNumber)
      IO.pure(IngestionResponse(ingestedData.length))
    },
    ingestDay.serverLogicSuccess[IO] { date =>
      val ingestedData = energyUsageStorer.retrieveTelemetryForDay(accountNumber, date)
      IO.pure(IngestionResponse(ingestedData.length))
    },
    ingestRange.serverLogicSuccess[IO] { (startDate, endDate) =>
      val ingestedData = energyUsageStorer.retrieveTelemetryBetweenDays(accountNumber, startDate, endDate)
      IO.pure(IngestionResponse(ingestedData.length))
    }
  ))
