package net.surguy.octopusviz.retrieve

import io.circe.*
import io.circe.parser.*
import net.surguy.octopusviz.utils.Logging
import sttp.client3.*
import sttp.model.Uri

import java.net.URI
import scala.collection.mutable.ListBuffer

class OctopusRetriever(electricity: MeterId, gas: MeterId, apiKey: String) extends Logging {

  private val backend = HttpClientSyncBackend()
  private val authRequest = basicRequest.auth.basic(apiKey, "")

  def fetchAllElectricityData(): Option[List[Consumption]] = {
    val initialElectricityData: Option[ResultsSummary] = fetchEnergyData("electricity", electricity, Some(1000)).flatMap(parseResults)
    initialElectricityData.map(fetchAllEnergyData)
  }

  def fetchAllGasData(): Option[List[Consumption]] = {
    val initialGasData: Option[ResultsSummary] = fetchEnergyData("gas", gas, Some(1000)).flatMap(parseResults)
    initialGasData.map(fetchAllEnergyData)
  }

  private def fetchAllEnergyData(initialData: ResultsSummary): List[Consumption] = {
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

  private[retrieve] def fetchEnergyData(energyType: String, meterId: MeterId, pageSize: Option[Int] = None): Option[String] = {
    fetchText(uri"https://api.octopus.energy/v1/$energyType-meter-points/${meterId.meterPointNumber}/meters/${meterId.serialNo}/consumption/?page_size=$pageSize")
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
