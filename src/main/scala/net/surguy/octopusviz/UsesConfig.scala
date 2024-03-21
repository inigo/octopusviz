package net.surguy.octopusviz

import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.HikariConfig
import net.surguy.octopusviz.retrieve.MeterId

trait UsesConfig {

  protected val config: Config = ConfigFactory.load("application.conf")

  protected val electricityId: MeterId = MeterId(config.getString("octopus.electricity.meterPointNumber"), config.getString("octopus.electricity.serialNo"))
  protected val gasId: MeterId = MeterId(config.getString("octopus.gas.meterPointNumber"), config.getString("octopus.gas.serialNo"))
  protected val apiKey: String = config.getString("octopus.apiKey")
  protected val accountNumber: String = config.getString("octopus.accountNumber")
  
  protected val iqAirApiKey: String = config.getString("iqAir.apiKey")
  protected val latitude: Double = config.getDouble("position.latitude")
  protected val longitude: Double = config.getDouble("position.longitude")

  protected val dbConfig: HikariConfig = {
    val hikari = HikariConfig()
    val c = config.getConfig("db.default")
    hikari.setDriverClassName(c.getString("driver"))
    hikari.setJdbcUrl(c.getString("url"))
    hikari.setUsername(c.getString("username"))
    hikari.setPassword(c.getString("password"))
    hikari
  }

}
