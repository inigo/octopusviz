package net.surguy.octopusviz.utils

import org.slf4j.{Logger, LoggerFactory}

trait Logging {
  protected lazy val log: Logger = LoggerFactory.getLogger(this.getClass)
}
