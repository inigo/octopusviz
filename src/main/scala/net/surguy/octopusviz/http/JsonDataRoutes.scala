package net.surguy.octopusviz.http

import cats.effect.*
import io.circe.Encoder
import io.circe.generic.auto.*
import net.surguy.octopusviz.*
import net.surguy.octopusviz.retrieve.{Consumption, Telemetry}
import net.surguy.octopusviz.storage.DatabaseAccess
import org.http4s.HttpRoutes
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.Http4sServerInterpreter

import java.time.temporal.ChronoUnit
import java.time.{Duration, LocalDate, LocalDateTime, LocalTime}
import scala.collection.MapView

class JsonDataRoutes(dbAccess: DatabaseAccess):
  import TapirCodecs.given

  implicit val encodeEnergyType: Encoder[EnergyType] =
    Encoder.instance(et => io.circe.Json.fromString(et.name))

  case class ConsumptionQuery(startDate: Option[LocalDate], endDate: Option[LocalDate], energyType: EnergyType)
  case class ConsumptionResponse(query: ConsumptionQuery, values: Seq[Consumption])

  case class TelemetryQuery(startDate: Option[LocalDateTime], endDate: Option[LocalDateTime])
  case class TelemetryResponse(query: TelemetryQuery, values: Seq[Telemetry])

  private val consumptionEndpoint = endpoint.get
    .in("data" / "consumption")
    .in(query[Option[LocalDate]]("startDate"))
    .in(query[Option[LocalDate]]("endDate"))
    .in(query[EnergyType]("energyType"))
    .out(jsonBody[ConsumptionResponse])
    .description("Consumption data as JSON")

  private val telemetryEndpoint = endpoint.get
    .in("data" / "telemetry")
    .in(query[Option[LocalDateTime]]("startDate"))
    .in(query[Option[LocalDateTime]]("endDate"))
    .out(jsonBody[TelemetryResponse])
    .description("Telemetry data as JSON")

  private val historicalMeanEndpoint = endpoint.get
    .in("data" / "telemetry" / "historicalmean")
    .in(query[LocalDateTime]("startDate"))
    .in(query[LocalDateTime]("endDate"))
    .out(jsonBody[TelemetryResponse])
    .description("Historical mean telemetry: 4-week average for the same time window")

  val allEndpoints: List[AnyEndpoint] =
    List(consumptionEndpoint, telemetryEndpoint, historicalMeanEndpoint)

  val jsonDataRoutes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(List(
    consumptionEndpoint.serverLogicSuccess[IO] { (maybeStartDate, maybeEndDate, energyType) =>
      val values = dbAccess.listConsumption(maybeStartDate.map(_.atStartOfDay), maybeEndDate.map(_.plusDays(1).atStartOfDay))
      IO.pure(ConsumptionResponse(ConsumptionQuery(maybeStartDate, maybeEndDate, energyType), values))
    },
    telemetryEndpoint.serverLogicSuccess[IO] { (maybeStartDate, maybeEndDate) =>
      val values = dbAccess.listTelemetry(maybeStartDate, maybeEndDate)
      IO.pure(TelemetryResponse(TelemetryQuery(maybeStartDate, maybeEndDate), values))
    },
    historicalMeanEndpoint.serverLogicSuccess[IO] { (startDate, endDate) =>
      val values = previousWeekMean(startDate, endDate)
      IO.pure(TelemetryResponse(TelemetryQuery(Some(startDate), Some(endDate)), values))
    }
  ))

  def previousWeekMean(startDate: LocalDateTime, endDate: LocalDateTime): List[Telemetry] =
    val extendedPeriod = Duration.ofDays(28)
    val historicalStartDate = startDate.minus(extendedPeriod)
    val values = dbAccess.listTelemetry(Some(historicalStartDate), Some(startDate))
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

implicit class LocalTimeExtensions(val time: LocalTime) extends AnyVal:
  def truncateToNearestXMinutes(x: Int): LocalTime =
    val minute = time.getMinute
    val nearestX = minute - (minute % x)
    time.truncatedTo(ChronoUnit.HOURS).plusMinutes(nearestX)
