package net.surguy.octopusviz.http

import net.surguy.octopusviz.EnergyType
import sttp.tapir.*

import java.time.{LocalDate, LocalDateTime}

object TapirCodecs:

  given Codec[String, LocalDate, CodecFormat.TextPlain] =
    Codec.string.mapDecode { s =>
      scala.util.Try(LocalDate.parse(s)) match
        case scala.util.Success(d) => DecodeResult.Value(d)
        case scala.util.Failure(e) => DecodeResult.Error(s, e)
    }(_.toString)

  given Codec[String, LocalDateTime, CodecFormat.TextPlain] =
    Codec.string.mapDecode { s =>
      scala.util.Try(LocalDateTime.parse(s)) match
        case scala.util.Success(d) => DecodeResult.Value(d)
        case scala.util.Failure(e) => DecodeResult.Error(s, e)
    }(_.toString)

  given Codec[String, EnergyType, CodecFormat.TextPlain] =
    Codec.string.mapDecode { s =>
      EnergyType.lookup(s) match
        case Some(et) => DecodeResult.Value(et)
        case None     => DecodeResult.Error(s, new IllegalArgumentException(
          s"Unknown energy type '$s'. Allowed: ${EnergyType.all.map(_.name).mkString(", ")}"))
    }(_.name)

  given Schema[LocalDate]     = Schema(SchemaType.SString()).format("date")
  given Schema[LocalDateTime] = Schema(SchemaType.SString()).format("date-time")
  given Schema[EnergyType]    = Schema.string.validate(
    Validator.enumeration(EnergyType.all.toList, (et: EnergyType) => Some(et.name)))
