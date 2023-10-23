package net.surguy.octopusviz.http

import cats.data.NonEmptyList
import cats.effect.*
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import com.comcast.ip4s.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.Logger
import cats.effect.Sync
import cats.implicits.*
import org.http4s.CacheDirective.`no-cache`
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Location, `Cache-Control`}
import cats.implicits.*
import org.http4s.implicits.*
import org.http4s.server.staticcontent.WebjarServiceBuilder.WebjarAsset
import org.http4s.server.staticcontent.{FileService, fileService, webjarServiceBuilder}

object QuickstartServer {



  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  private val helloWorldService: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name =>
      val htmlContent: String = views.html.index(s"Goood day $name!").toString
      Ok(htmlContent, "Content-Type" -> "text/html")
    case GET -> Root / "hello"  =>
      val htmlContent: String = views.html.index(s"Hello again someone!").toString
      Ok(htmlContent, "Content-Type" -> "text/html")
    case GET -> Root =>
      TemporaryRedirect(Location(uri"/hello"))
  }

  private val otherService: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "other" =>
      Ok("Other")
  }

  private val webjarService: HttpRoutes[IO] = {
      webjarServiceBuilder[IO].toRoutes
  }

  private val fileTypes = List(".js", ".css", ".map", ".html", ".webm", ".webp", ".json")
  private val fileRoutes = HttpRoutes.of[IO] {
    case request @ GET -> Root / "assets" / path if fileTypes.exists(path.endsWith) =>
      StaticFile.fromResource("/assets/" + path, Some(request)).getOrElseF(NotFound())
  }

  def run[F[_] : Async](): IO[ExitCode] = {
    val httpApp = Router("/" -> (helloWorldService <+> otherService <+> webjarService <+> fileRoutes) ).orNotFound
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