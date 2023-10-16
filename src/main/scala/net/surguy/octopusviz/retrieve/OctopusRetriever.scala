package net.surguy.octopusviz.retrieve

import io.circe.*
import io.circe.parser.*
import net.surguy.octopusviz.utils.Logging
import sttp.client3.*
import sttp.model.Uri

import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.collection.mutable.ListBuffer

class OctopusRetriever(electricity: MeterId, gas: MeterId, apiKey: String) extends Logging {

  private val backend = HttpClientSyncBackend()
  private val authRequest = basicRequest.auth.basic(apiKey, "")

  private val DEFAULT_PAGE_SIZE: Some[Int] = Some(1000)

  def fetchElectricityFrom(startDate: ZonedDateTime): Option[List[Consumption]] = {
    fetchInitialEnergyData("electricity", electricity, DEFAULT_PAGE_SIZE, Some(startDate)).flatMap(parseResults).map(fetchRemainingEnergyData)
  }

  def fetchGasFrom(startDate: ZonedDateTime): Option[List[Consumption]] = {
    fetchInitialEnergyData("gas", gas, DEFAULT_PAGE_SIZE, Some(startDate)).flatMap(parseResults).map(fetchRemainingEnergyData)
  }

  def fetchAllElectricityData(): Option[List[Consumption]] = {
    fetchInitialEnergyData("electricity", electricity, DEFAULT_PAGE_SIZE, None).flatMap(parseResults).map(fetchRemainingEnergyData)
  }

  def fetchAllGasData(): Option[List[Consumption]] = {
    fetchInitialEnergyData("gas", gas, DEFAULT_PAGE_SIZE, None).flatMap(parseResults).map(fetchRemainingEnergyData)
  }

  private def fetchRemainingEnergyData(initialData: ResultsSummary): List[Consumption] = {
    var energyData: Option[ResultsSummary] = Some(initialData)
    val allConsumption = new ListBuffer[Consumption]()
    // do-while loops have been dropped in Scala 3; instead, this loops while the final line in the block resolves to true
    while {
      allConsumption ++= energyData.map(_.results).getOrElse(Nil)
      energyData.flatMap(_.next) match {
        case Some(nextUrl) =>
          log.info(s"Following link $nextUrl to next set of energy data")
          energyData = fetchMoreEnergyData(nextUrl).flatMap(parseResults)
        case None => energyData = None
      }
      energyData.isDefined
    } do ()

    allConsumption.toList
  }

  private[retrieve] def fetchInitialEnergyData(energyType: String, meterId: MeterId, pageSize: Option[Int] = None, periodFrom: Option[ZonedDateTime] = None): Option[String] = {
    val formattedPeriodFrom = periodFrom.map( p => p.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")))
    fetchText(uri"https://api.octopus.energy/v1/$energyType-meter-points/${meterId.meterPointNumber}/meters/${meterId.serialNo}/consumption/?page_size=$pageSize&period_from=$formattedPeriodFrom")
  }
  private[retrieve] def fetchMoreEnergyData(nextUrl: String): Option[String] = fetchText(Uri(URI.create(nextUrl)))

  private[retrieve] def fetchText(uri: Uri) = {
    val request = authRequest.get(uri)
    val response = backend.send(request)
    response.body match {
      case Right(success) =>
        log.debug("Retrieved:\n" + success)
        Some(success)
      case Left(value) =>
        log.warn(s"Failed to retrieve electricity data with: $value")
        None
    }
  }

  private[retrieve] def parseResults(results: String): Option[ResultsSummary] = {
    log.debug("Parsing: " + results)
    decode[ResultsSummary](results) match {
      case Right(success) => Some(success)
      case Left(failure) =>
        log.warn("Failed to parse JSON with: " + failure)
        None
    }
  }

}
