package net.surguy.octopusviz

import net.surguy.octopusviz.retrieve.OctopusRetriever
import net.surguy.octopusviz.storage.DatabaseAccess

class EnergyUsageStorer(retriever: OctopusRetriever, dbAccess: DatabaseAccess) {

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

}
