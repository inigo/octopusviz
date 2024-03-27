package net.surguy.octopusviz.http

import cats.effect.*
import cats.syntax.all.*
import net.surguy.octopusviz.*
import net.surguy.octopusviz.retrieve.{Consumption, Telemetry}
import net.surguy.octopusviz.storage.DatabaseAccess
import org.http4s.*
import org.http4s.dsl.io.*

import java.time.temporal.ChronoUnit
import java.time.{Duration, LocalDate, LocalDateTime, LocalTime}
import scala.collection.MapView

class JsonDataRoutes(dbAccess: DatabaseAccess) {

  import io.circe.*
  import io.circe.generic.auto.*
  import io.circe.syntax.*

  implicit val encodeEnergyType: Encoder[EnergyType] = Encoder.instance(energyType => Json.fromString(energyType.name))

  implicit val localDateQueryParamDecoder: QueryParamDecoder[LocalDate] = QueryParamDecoder[String].map(LocalDate.parse)
  implicit val localDateTimeQueryParamDecoder: QueryParamDecoder[LocalDateTime] = QueryParamDecoder[String].map(LocalDateTime.parse)
  implicit val energyTypeQueryParamDecoder: QueryParamDecoder[EnergyType] = QueryParamDecoder[String].emap { s =>
    val failureMessage = "Failed to parse energy type - allowed values are: " + EnergyType.all.mkString(", ")
    EnergyType.lookup(s).toRight(ParseFailure(failureMessage, failureMessage))
  }

  private object StartDateQueryParamMatcher extends OptionalQueryParamDecoderMatcher[LocalDate]("startDate")
  private object EndDateQueryParamMatcher extends OptionalQueryParamDecoderMatcher[LocalDate]("endDate")

  private object StartDateTimeQueryParamMatcher extends OptionalQueryParamDecoderMatcher[LocalDateTime]("startDate")
  private object EndDateTimeQueryParamMatcher extends OptionalQueryParamDecoderMatcher[LocalDateTime]("endDate")

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
    case _ @ GET -> Root / "data" / "telemetry" :? StartDateTimeQueryParamMatcher(maybeStartDate) :? EndDateTimeQueryParamMatcher(maybeEndDate) =>
        val values: Seq[Telemetry] = dbAccess.listTelemetry(maybeStartDate, maybeEndDate)
        val consumptionQuery = TelemetryQuery(maybeStartDate, maybeEndDate)
        val json: String = TelemetryResponse(consumptionQuery, values).asJson.deepDropNullValues.noSpaces
        Ok(json)
    case _ @ GET -> Root / "data" / "telemetry" / "historicalmean" :? StartDateTimeQueryParamMatcher(maybeStartDate) :? EndDateTimeQueryParamMatcher(maybeEndDate) =>
        val startDate = maybeStartDate.getOrElse(throw new IllegalArgumentException("No start date provided"))  
        val endDate = maybeStartDate.getOrElse(throw new IllegalArgumentException("No start date provided"))  
        val values: Seq[Telemetry] = previousWeekMean(startDate, endDate)
        val consumptionQuery = TelemetryQuery(maybeStartDate, maybeEndDate)
        val json: String = TelemetryResponse(consumptionQuery, values).asJson.deepDropNullValues.noSpaces
        Ok(json)
  }

  def previousWeekMean(startDate: LocalDateTime, endDate: LocalDateTime): List[Telemetry] = {
    val extendedPeriod = Duration.ofDays(28)
    val historicalStartDate = startDate.minus(extendedPeriod)
    val values = dbAccess.listTelemetry(Some(historicalStartDate), Some(startDate))
    // Often values are missing, so cannot assume that we can group just by counting. Grouping by more minutes increases the chance of good data
    val groupByTime: Map[LocalTime, Seq[Telemetry]] = values.groupBy(f => f.readAt.toLocalTime.truncateToNearestXMinutes(5))
    val averagedByTime: MapView[LocalTime, Option[Double]] = groupByTime.view.mapValues {
      case telemetrySeq if telemetrySeq.size > 10 => Some(telemetrySeq.map(_.demand).sum / telemetrySeq.size.toDouble)
      case _ => None
    }
    val initialDate = startDate.toLocalDate
    averagedByTime.flatMap { 
      case (time, Some(demandAverage)) => Some(Telemetry(LocalDateTime.of(initialDate, time), 0, demandAverage))
      case _ => None
    }.toList.sortBy(_.readAt)
  }

  case class ConsumptionResponse(query: ConsumptionQuery, values: Seq[Consumption])
  case class ConsumptionQuery(startDate: Option[LocalDate], endDate: Option[LocalDate], energyType: EnergyType)

  case class TelemetryResponse(query: TelemetryQuery, values: Seq[Telemetry])
  case class TelemetryQuery(startDate: Option[LocalDateTime], endDate: Option[LocalDateTime])

}

implicit class LocalTimeExtensions(val time: LocalTime) extends AnyVal {
  def truncateToNearestXMinutes(x: Int): LocalTime = {
    val minute = time.getMinute
    val nearestX = minute - (minute % x)
    time.truncatedTo(ChronoUnit.HOURS).plusMinutes(nearestX)
  }
}