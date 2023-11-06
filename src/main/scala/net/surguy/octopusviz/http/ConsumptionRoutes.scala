package net.surguy.octopusviz.http

import cats.effect.*
import net.surguy.octopusviz.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.Location
import org.http4s.implicits.*

import java.time.LocalDate

object ConsumptionRoutes {

  val consumptionRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "consumption" / startDateText / endDateText =>
      val startDate = LocalDate.parse(startDateText)
      val endDate = LocalDate.parse(endDateText)
      val htmlContent: String = views.html.index(startDate, endDate).toString
      Ok(htmlContent, "Content-Type" -> "text/html")
    case GET -> Root / "consumption" =>
      val endDate = LocalDate.now()
      val startDate = endDate.minusYears(1)
      val htmlContent: String = views.html.index(startDate, endDate).toString
      Ok(htmlContent, "Content-Type" -> "text/html")
    case GET -> Root =>
      TemporaryRedirect(Location(uri"/consumption"))
  }
}
