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

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object QuickstartServer extends UsesConfig {

  import ConsumptionRoutes.*

  private val dataSource = HikariDataSource(dbConfig)
  private val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  private val db = new DatabaseAccess(dataSource, ec)

  val jsonDataRoutes = new JsonDataRoutes(db)

  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  private val webjarRoutes: HttpRoutes[IO] = {
      webjarServiceBuilder[IO].toRoutes
  }

  private val fileTypes = List(".js", ".css", ".map", ".html", ".webm", ".webp", ".json")
  private val fileRoutes = HttpRoutes.of[IO] {
    case request @ GET -> Root / "assets" / path if fileTypes.exists(path.endsWith) =>
      StaticFile.fromResource("/assets/" + path, Some(request)).getOrElseF(NotFound())
  }

  def run[F[_] : Async](): IO[ExitCode] = {
    val httpApp = Router("/" -> (consumptionRoutes <+> jsonDataRoutes.jsonDataRoutes <+> webjarRoutes <+> fileRoutes) ).orNotFound
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