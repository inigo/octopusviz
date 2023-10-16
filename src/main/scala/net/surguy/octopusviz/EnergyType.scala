package net.surguy.octopusviz

trait EnergyType(val name: String)
case object Gas extends EnergyType("gas")
case object Electricity extends EnergyType("electricity")

