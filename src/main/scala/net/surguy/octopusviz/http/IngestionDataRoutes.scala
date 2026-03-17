package net.surguy.octopusviz.http

import cats.effect.*
import net.surguy.octopusviz.*
import net.surguy.octopusviz.storage.DatabaseAccess
import org.http4s.*
import org.http4s.dsl.io.*

import java.time.LocalDate

class IngestionDataRoutes(dbAccess: DatabaseAccess, energyUsageStorer: EnergyUsageStorer, accountNumber: String) {

  import io.circe.*
  import io.circe.generic.auto.*
  import io.circe.syntax.*

  val ingestionDataRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case POST -> Root / "ingest" =>
      val ingestedData = energyUsageStorer.retrieveTelemetry(accountNumber)
      val json: String = IngestionResponse(ingestedData.length).asJson.deepDropNullValues.noSpaces
      Ok(json)
    case POST -> Root / "ingest" / dateText =>
      val ingestionDate = LocalDate.parse(dateText)
      val ingestedData = energyUsageStorer.retrieveTelemetryForDay(accountNumber, ingestionDate)
      val json: String = IngestionResponse(ingestedData.length).asJson.deepDropNullValues.noSpaces
      Ok(json)
    case POST -> Root / "ingest" / startDateText / endDateText =>
      val startDay = LocalDate.parse(startDateText)
      val endDay = LocalDate.parse(endDateText)
      val ingestedData = energyUsageStorer.retrieveTelemetryBetweenDays(accountNumber, startDay, endDay)
      val json: String = IngestionResponse(ingestedData.length).asJson.deepDropNullValues.noSpaces
      Ok(json)
  }

  case class IngestionResponse(count: Int)

}

