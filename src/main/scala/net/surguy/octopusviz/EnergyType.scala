package net.surguy.octopusviz

sealed trait EnergyType(val name: String)
case object Gas extends EnergyType("gas")
case object Electricity extends EnergyType("electricity")

object EnergyType {
  val all: Seq[EnergyType] = List(Gas, Electricity)
  def lookup(name: String): Option[EnergyType] = all.find(_.name==name)
}