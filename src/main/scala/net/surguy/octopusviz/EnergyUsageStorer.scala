package net.surguy.octopusviz

import net.surguy.octopusviz.retrieve.{GraphQlClient, OctopusRetriever, Telemetry}
import net.surguy.octopusviz.storage.DatabaseAccess
import net.surguy.octopusviz.utils.Logging

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime, LocalTime}
import scala.collection.mutable.ListBuffer

class EnergyUsageStorer(retriever: OctopusRetriever, graphQl: GraphQlClient, dbAccess: DatabaseAccess) extends Logging {

  import net.surguy.octopusviz.EnergyUsageStorer.*

  def retrieveAndStore(): Unit = {
    val dateOfLatestElectricity = dbAccess.findMostRecentIntervalEnd(Electricity)
    val electricity = dateOfLatestElectricity match {
      case Some(date) => retriever.fetchElectricityFrom(date)
      case None => retriever.fetchAllElectricityData()
    }
    electricity.map { data =>
      dbAccess.storeConsumption(Electricity, data)
    }

    val dateOfLatestGas = dbAccess.findMostRecentIntervalEnd(Gas)
    val gas = dateOfLatestGas match {
      case Some(date) => retriever.fetchGasFrom(date)
      case None => retriever.fetchAllGasData()
    }
    gas.map { data =>
      dbAccess.storeConsumption(Gas, data)
    }
  }

  def retrieveTelemetry(accountNumber: String): Seq[Telemetry] = {
    val dateOfLatestTelemetry = dbAccess.findMostRecentReadAt().map(_.toLocalDateTime)
    val now = LocalDateTime.now()
    val today: LocalDateTime = now.truncatedTo(ChronoUnit.DAYS)
    val startTime = dateOfLatestTelemetry match {
      case None => today
      case Some(date) if date.isBefore(today) => today
      case Some(date) => date
    }
    log.info(s"Retrieving from $startTime to $now")
    val deviceId = graphQl.getMeterIds(accountNumber).electricityDeviceId.get
    val telemetry = graphQl.getElectricityConsumption(deviceId, startTime, now)
    dbAccess.storeTelemetry(telemetry)
    telemetry
  }

  def retrieveTelemetryBetweenDays(accountNumber: String, startDay: LocalDate, endDay: LocalDate): Seq[Telemetry] = {
    daysBetweenInclusive(startDay, endDay).flatMap(d => retrieveTelemetryForDay(accountNumber, d))
  }
  
  def retrieveTelemetryForDay(accountNumber: String, day: LocalDate): Seq[Telemetry] = {
    val start = LocalDateTime.of(day, LocalTime.MIDNIGHT)
    val end = start.plusDays(1).minusMinutes(1)
    log.info(s"Retrieving from $start to $end")
    val deviceId = graphQl.getMeterIds(accountNumber).electricityDeviceId.get
    val telemetry = graphQl.getElectricityConsumption(deviceId, start, end)
    dbAccess.storeTelemetry(telemetry)
    telemetry
  }

}

object EnergyUsageStorer {
  def daysBetweenInclusive(startDay: LocalDate, endDay: LocalDate): List[LocalDate] = {
    if (endDay.isBefore(startDay)) throw new IllegalArgumentException(s"End day $endDay is before start day $startDay")
    val days = new ListBuffer[LocalDate]
    var currentDay = startDay
    while (!currentDay.isAfter(endDay)) {
      days += currentDay
      currentDay = currentDay.plusDays(1)
    }
    days.toList
  }
}