package net.surguy.octopusviz.http

import cats.effect.*
import cats.syntax.all.*
import net.surguy.octopusviz.*
import org.http4s.dsl.io.*
import org.http4s.*

import java.time.LocalDate

object JsonDataRoutes {

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
    case req@GET -> Root / "data" / "consumption" :? StartDateQueryParamMatcher(maybeStartDate) :? EndDateQueryParamMatcher(maybeEndDate) :? EnergyTypeQueryParamMatcher(validatedEnergyType) =>
      validatedEnergyType.fold(
        parseFailures => BadRequest(parseFailures.map(_.sanitized).mkString_("; ")),
        energyType =>
          val consumptionQuery = ConsumptionQuery(maybeStartDate, maybeEndDate, energyType)
          val json: String = ConsumptionResponse(consumptionQuery).asJson.deepDropNullValues.noSpaces
          val x = json
          Ok(json)
      )
  }

  case class ConsumptionResponse(query: ConsumptionQuery)

  case class ConsumptionQuery(startDate: Option[LocalDate], endDate: Option[LocalDate], energyType: EnergyType)

}
