package net.surguy.octopusviz.http

import cats.effect.IO
import org.http4s.{HttpRoutes, Method, Request, Response, Uri}
import org.typelevel.ci.CIString

trait Http4sTestHelpers {
  import cats.effect.unsafe.implicits.global

  extension (httpRoutes: HttpRoutes[IO]) {
    def testGet(uri: String): Response[IO] = { testGet(Uri.unsafeFromString(uri)) }
    def testGet(uri: Uri): Response[IO] = { runRequest(Request[IO](Method.GET, uri)) }

    private def runRequest(request: Request[IO]) = { httpRoutes.orNotFound.run(request).unsafeRunSync() }
  }

  extension (response: Response[IO]) {
    def unsafeBodyText(): String = {
      response.bodyText.compile.toList.map(_.mkString).unsafeRunSync()
    }

    def headerValue(key: String): Option[String] = {
      response.headers.get(CIString(key)).map(_.head).map(_.value)
    }
  }

}
