package net.surguy.weather

import net.surguy.octopusviz.UsesConfig
import org.specs2.mutable.Specification

class IqAirRetrieverTest extends Specification with UsesConfig {

  private val weatherRetriever = IqAirRetriever(latitude, longitude, iqAirApiKey)

  "retrieving weather" should {
    "contain some values" in {
      val maybeWeather = weatherRetriever.fetchWeather()
      maybeWeather must beSome
      val weather = maybeWeather.get
      println(weather)
    }
  }
}
