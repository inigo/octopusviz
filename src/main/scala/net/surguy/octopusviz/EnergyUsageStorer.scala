package net.surguy.octopusviz

import net.surguy.octopusviz.retrieve.OctopusRetriever
import net.surguy.octopusviz.storage.DatabaseAccess

class EnergyUsageStorer(retriever: OctopusRetriever, dbAccess: DatabaseAccess) {

  def retrieveAndStore(): Unit = {
    retriever.fetchAllElectricityData().map { data =>
      dbAccess.storeConsumption(Electricity, data)
    }
    retriever.fetchAllGasData().map { data =>
      dbAccess.storeConsumption(Gas, data)
    }
  }

}
