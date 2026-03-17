package net.surguy.octopusviz.http

import cats.effect.*
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import com.comcast.ip4s.*
import net.surguy.octopusviz.*
import net.surguy.octopusviz.storage.DatabaseAccess
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.staticcontent.webjarServiceBuilder
import com.zaxxer.hikari.HikariDataSource
import net.surguy.octopusviz.retrieve.{GraphQlClient, OctopusRetriever}
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object QuickstartServer extends UsesConfig {

  import ConsumptionRoutes.*

  private val dataSource = HikariDataSource(dbConfig)
  private val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  private val db = new DatabaseAccess(dataSource, ec)

  private val retriever = new OctopusRetriever(electricityId, gasId, apiKey)
  private val graphQlClient = new GraphQlClient(apiKey)
  private val energyUsageStorer = new EnergyUsageStorer(retriever, graphQlClient, db)

  val jsonDataRoutes = new JsonDataRoutes(db)
  val ingestionDataRoutes = new IngestionDataRoutes(db, energyUsageStorer, accountNumber)

  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  private val webjarRoutes: HttpRoutes[IO] = {
      webjarServiceBuilder[IO].toRoutes
  }

  private val fileTypes = List(".js", ".css", ".map", ".html", ".webm", ".webp", ".json")
  private val fileRoutes = HttpRoutes.of[IO] {
    case request @ GET -> Root / "assets" / path if fileTypes.exists(path.endsWith) =>
      StaticFile.fromResource("/assets/" + path, Some(request)).getOrElseF(NotFound())
  }

  private val swaggerRoutes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(
    SwaggerInterpreter().fromEndpoints[IO](jsonDataRoutes.allEndpoints ++ ingestionDataRoutes.allEndpoints, "OctopusViz", "1.0")
  )

  def run[F[_] : Async](): IO[ExitCode] = {
    val httpApp = Router("/" -> (consumptionRoutes <+> jsonDataRoutes.jsonDataRoutes <+> ingestionDataRoutes.ingestionDataRoutes <+> swaggerRoutes <+> webjarRoutes <+> fileRoutes)).orNotFound
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build
      .useForever
      .as(ExitCode.Success)
  }
}
