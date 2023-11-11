package net.surguy.octopusviz.http

import cats.effect.*
import cats.syntax.all.*
import net.surguy.octopusviz.*
import net.surguy.octopusviz.retrieve.Consumption
import net.surguy.octopusviz.storage.DatabaseAccess
import org.http4s.dsl.io.*
import org.http4s.*

import java.time.LocalDate

class JsonDataRoutes(dbAccess: DatabaseAccess) {

  import io.circe.*
  import io.circe.generic.auto.*
  import io.circe.syntax.*

  implicit val encodeEnergyType: Encoder[EnergyType] = Encoder.instance(energyType => Json.fromString(energyType.name))

  implicit val localDateQueryParamDecoder: QueryParamDecoder[LocalDate] = QueryParamDecoder[String].map(LocalDate.parse)
  implicit val energyTypeQueryParamDecoder: QueryParamDecoder[EnergyType] = QueryParamDecoder[String].emap { s =>
    val failureMessage = "Failed to parse energy type - allowed values are: " + EnergyType.all.mkString(", ")
    EnergyType.lookup(s).toRight(ParseFailure(failureMessage, failureMessage))
  }

  private object StartDateQueryParamMatcher extends OptionalQueryParamDecoderMatcher[LocalDate]("startDate")

  private object EndDateQueryParamMatcher extends OptionalQueryParamDecoderMatcher[LocalDate]("endDate")

  private object EnergyTypeQueryParamMatcher extends ValidatingQueryParamDecoderMatcher[EnergyType]("energyType")

  val jsonDataRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case _ @ GET -> Root / "data" / "consumption" :? StartDateQueryParamMatcher(maybeStartDate) :? EndDateQueryParamMatcher(maybeEndDate) :? EnergyTypeQueryParamMatcher(validatedEnergyType) =>
      validatedEnergyType.fold(
        parseFailures => BadRequest(parseFailures.map(_.sanitized).mkString_("; ")),
        energyType =>
          val values: Seq[Consumption] = dbAccess.listConsumption(maybeStartDate.map(_.atStartOfDay), maybeEndDate.map(_.plusDays(1).atStartOfDay))
          val consumptionQuery = ConsumptionQuery(maybeStartDate, maybeEndDate, energyType)
          val json: String = ConsumptionResponse(consumptionQuery, values).asJson.deepDropNullValues.noSpaces
          Ok(json)
      )
  }

  case class ConsumptionResponse(query: ConsumptionQuery, values: Seq[Consumption])
  case class ConsumptionQuery(startDate: Option[LocalDate], endDate: Option[LocalDate], energyType: EnergyType)

}
