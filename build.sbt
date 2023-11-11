val scala3Version = "3.3.1"
val http4sVersion = "0.23.23"
val circeVersion = "0.14.6"
val doobieVersion = "1.0.0-RC4"
val playVersion = "2.9.0"

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
    commonSettings,

    libraryDependencies ++= Seq(
      // Test framework (note that the Specs2 cats library is v4, and doesn't seem compatible with specs v5)
      "org.specs2" %% "specs2-core" % "5.4.0" % Test
      , "org.mockito" % "mockito-core" % "5.7.0" % Test
      // Logging
      , "ch.qos.logback" % "logback-classic" % "1.4.11"
      // Config
      , "com.typesafe" % "config" % "1.4.3"
      // JSON parsing
      , "io.circe" %% "circe-core" % circeVersion
      , "io.circe" %% "circe-parser" % circeVersion
      , "io.circe" %% "circe-generic" % circeVersion
      , "io.circe" %% "circe-literal" % circeVersion
      // Database access
      , "org.tpolecat" %% "doobie-core" % doobieVersion
      , "org.tpolecat" %% "doobie-postgres" % doobieVersion
      , "org.tpolecat" %% "doobie-hikari" % doobieVersion
      , "org.postgresql" % "postgresql" % "42.6.0"
      // Database migrations (using Play instead of Flyway since Flyway doesn't do downs)
      , "com.typesafe.play" %% "play-jdbc-evolutions" % playVersion
      , "com.typesafe.play" %% "play-jdbc" % playVersion
      , "com.typesafe.play" %% "play-guice" % playVersion
      // In-memory test database
      , "com.opentable.components" % "otj-pg-embedded" % "1.0.2" % Test
      // HTTP client
      , "com.softwaremill.sttp.client3" %% "core" % "3.9.1"
      // HTTP server
      , "org.http4s" %% "http4s-ember-client" % http4sVersion
      , "org.http4s" %% "http4s-ember-server" % http4sVersion
      , "org.http4s" %% "http4s-dsl" % http4sVersion
      , "org.http4s" %% "http4s-circe" % http4sVersion
      , "com.typesafe.play" %% "twirl-api" % "1.6.2"
//      , "org.playframework.twirl" %% "twirl-api" % "2.0.0-M2"
      // Webjars
      , "org.webjars" % "bootstrap" % "5.3.2"
      , "org.webjars.npm" % "plotly.js-dist-min" % "2.18.2"
    )
)
