package net.surguy.octopusviz.http

import cats.effect.*
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import com.comcast.ip4s.*
import net.surguy.octopusviz.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.staticcontent.webjarServiceBuilder

object QuickstartServer {

  import ConsumptionRoutes.*
  import JsonDataRoutes.*

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
    val httpApp = Router("/" -> (consumptionRoutes <+> jsonDataRoutes <+> webjarRoutes <+> fileRoutes) ).orNotFound
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