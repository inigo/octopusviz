package net.surguy.octopusviz.retrieve

import io.circe.Decoder
import io.circe.derivation.{Configuration, ConfiguredCodec}

import java.time.{LocalDateTime, ZoneOffset}

case class MeterId(meterPointNumber: String, serialNo: String)

// The JSON uses the snake case interval_start and interval_end, we prefer intervalStart and intervalEnd
// The "derives ConfiguredCodec" makes the case classes use this Configuration
given Configuration = Configuration.default.withSnakeCaseMemberNames
// It's harder to get a ZonedDateTime into the Postgres database using Doobie, so convert it to a UTC LocalDateTime instead
given decodeLocalDateTime: Decoder[LocalDateTime] = Decoder.decodeZonedDateTime.map(_.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime)
case class Consumption(consumption: Double, intervalStart: LocalDateTime, intervalEnd: LocalDateTime) derives Decoder, ConfiguredCodec
private[retrieve] case class ResultsSummary(count: Int, next: Option[String], previous: Option[String], results: List[Consumption]) derives Decoder, ConfiguredCodec
