package net.surguy.octopusviz.http

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    QuickstartServer.run[IO]()
  }

}
