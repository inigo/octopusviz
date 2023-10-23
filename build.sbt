val scala3Version = "3.3.1"
val http4sVersion = "0.23.23"

lazy val compilerOptions = Seq("-Xfatal-warnings", "-unchecked", "-deprecation", "-explain", "-feature")
lazy val commonSettings = Seq(scalacOptions ++= compilerOptions)

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = project
  .in(file("."))
  .enablePlugins(SbtTwirl)
  .settings(
    name := "octopus-energyviz",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    mainClass := Some("net.surguy.octopusviz.http.Main"),
    commonSettings,

    libraryDependencies ++= Seq(
      // Test framework
      "org.specs2" %% "specs2-core" % "5.3.2" % Test
      , "org.specs2" %% "specs2-cats" % "4.20.2" % Test
      // Logging
      , "ch.qos.logback" % "logback-classic" % "1.4.11"
      // Config
      , "com.typesafe" % "config" % "1.4.2"
      // JSON parsing
      , "io.circe" %% "circe-core" % "0.14.6"
      , "io.circe" %% "circe-parser" % "0.14.6"
      , "io.circe" %% "circe-generic" % "0.14.6"
      // Database access
      , "org.tpolecat" %% "doobie-core" % "1.0.0-RC4"
      , "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC4"
      , "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC4"
      , "org.postgresql" % "postgresql" % "42.5.4"
      // Database migrations (using Play instead of Flyway since Flyway doesn't do downs)
      , "com.typesafe.play" %% "play-jdbc-evolutions" % "2.9.0-RC3"
      , "com.typesafe.play" %% "play-jdbc" % "2.9.0-RC3"
      , "com.typesafe.play" %% "play-guice" % "2.9.0-RC3"
      // In-memory test database
      , "com.opentable.components" % "otj-pg-embedded" % "1.0.1" % Test
      // HTTP client
      , "com.softwaremill.sttp.client3" %% "core" % "3.9.0"
      // HTTP server
      , "org.http4s" %% "http4s-ember-client" % http4sVersion
      , "org.http4s" %% "http4s-ember-server" % http4sVersion
      , "org.http4s" %% "http4s-dsl" % http4sVersion
      , "org.http4s" %% "http4s-circe" % http4sVersion
      , "com.typesafe.play" %% "twirl-api" % "1.6.1"
//      , "org.playframework.twirl" %% "twirl-api" % "2.0.0-M2"
      // Webjars
      , "org.webjars" % "bootstrap" % "5.3.2"
    )
)
