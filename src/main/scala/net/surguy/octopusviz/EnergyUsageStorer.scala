package net.surguy.octopusviz

import net.surguy.octopusviz.retrieve.{GraphQlClient, OctopusRetriever}
import net.surguy.octopusviz.storage.DatabaseAccess

import java.time.LocalDateTime
import java.time.temporal.{ChronoUnit, TemporalUnit}

class EnergyUsageStorer(retriever: OctopusRetriever, graphQl: GraphQlClient, dbAccess: DatabaseAccess) {

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

  def retrieveTelemetry(accountNumber: String): Unit = {
    val dateOfLatestTelemetry = dbAccess.findMostRecentReadAt().map(_.toLocalDateTime)
    val now = LocalDateTime.now()
    val today: LocalDateTime = now.truncatedTo(ChronoUnit.DAYS)
    val startTime = dateOfLatestTelemetry match {
      case None => today
      case Some(date) if date.isBefore(today) => today
      case Some(date) => date
    }
    val deviceId = graphQl.getMeterIds(accountNumber).electricityDeviceId.get
    val telemetry = graphQl.getElectricityConsumption(deviceId, startTime, now)
    dbAccess.storeTelemetry(telemetry)
  }

}
