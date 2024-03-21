package net.surguy.weather

import io.circe
import io.circe.{Decoder, HCursor}
import io.circe.derivation.ConfiguredCodec
import io.circe.parser.{decode, parse}
import net.surguy.octopusviz.retrieve.ResultsSummary
import net.surguy.octopusviz.utils.Logging
import sttp.client3.*
import sttp.model.Uri

import java.time.ZonedDateTime

class IqAirRetriever(latitude: Double, longitude: Double, apiKey: String) extends Logging {

  private val backend = HttpClientSyncBackend()
  
  private val endpoint = uri"https://api.airvisual.com/v2/nearest_city?lat=${latitude}&lon=${longitude}&key=${apiKey}"

  def fetchWeather(): Option[Weather] = {
    fetchWeatherJson().flatMap(parseResults)
  }
  
  private[weather] def fetchWeatherJson() = {
    val request = basicRequest.get(endpoint)
    val response = backend.send(request)
    response.body match {
      case Right(success) =>
        log.trace("Retrieved:\n" + success)
        Some(success)
      case Left(value) =>
        log.warn(s"Failed to retrieve weather data with: $value")
        None
    }
  }

  private[weather] def parseResults(resultsJson: String): Option[Weather] = {
    val weatherDecoder: Decoder[Weather] = (c: HCursor) => for {
      ts <- c.downField("ts").as[String]
      timestamp = ZonedDateTime.parse(ts)
      tp <- c.downField("tp").as[Int]
      pr <- c.downField("pr").as[Int]
      hu <- c.downField("hu").as[Int]
      ws <- c.downField("ws").as[Double]
      wd <- c.downField("wd").as[Int]
      ic: String <- c.downField("ic").as[String]
      (weatherCode: WeatherCode, period: Period) = parseWeatherPeriod(ic).getOrElse(throw new IllegalArgumentException(s"Unexpected weather icon code: $ic"))
    } yield Weather(timestamp, tp, pr, hu, ws, wd, weatherCode, period)

    val parsedWeather: Either[circe.Error, Weather] = for {
      json <- parse(resultsJson)
      weather <- json.hcursor.downField("data").downField("current").downField("weather").as[Weather](weatherDecoder)
    } yield weather
    
    parsedWeather.toOption
  }

  private def parseWeatherCode(code: String): Option[WeatherCode] = code match {
    case "01" => Some(Clear)
    case "02" => Some(FewClouds)
    case "03" => Some(ScatteredClouds)
    case "04" => Some(BrokenClouds)
    case "09" => Some(Showers)
    case "10" => Some(Rain)
    case "11" => Some(Thunderstorm)
    case "13" => Some(Snow)
    case "50" => Some(Mist)
    case _ => None
  }

  private def parsePeriod(period: Char): Option[Period] = period match {
    case 'd' => Some(Day)
    case 'n' => Some(Night)
    case _ => None
  }

  def parseWeatherPeriod(input: String): Option[(WeatherCode, Period)] = {
    if (input.length == 3) {
      for {
        weatherCode <- parseWeatherCode(input.substring(0, 2))
        period <- parsePeriod(input.charAt(2))
      } yield (weatherCode, period)
    } else None
  }

}

trait WeatherCode
case object Clear extends WeatherCode
case object FewClouds extends WeatherCode
case object ScatteredClouds extends WeatherCode
case object BrokenClouds extends WeatherCode
case object Showers extends WeatherCode
case object Rain extends WeatherCode
case object Thunderstorm extends WeatherCode
case object Snow extends WeatherCode
case object Mist extends WeatherCode

trait Period
case object Day extends Period
case object Night extends Period

case class Weather(timestamp: ZonedDateTime, // Timestamp
                   tempCelsius: Double, // Temperature in C
                   pressureHpa: Double, // Atmospheric pressure in hPa
                   humidityPercent: Double, // Humidity %
                   windSpeedMs: Double, // Wind speed m/s
                   windDirectionDegrees: Double, // Wind direction in degrees (0-360)
                   // Weather icon code e.g. 01d, where d/n = day/night, numeric code: 01 = clear, 02 = few clouds, 03 scattered clouds, 04 broken clouds, 09 rain showers, 10 rain, 11 thunderstorm, 13 snow, 50 mist
                   weatherCode: WeatherCode,
                   period: Period 
                  )
