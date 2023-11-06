package net.surguy.octopusviz.http

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {

  def formatDate(dateTime: LocalDate): String = {
    val dayOfMonth = dateTime.getDayOfMonth
    val formatter = DateTimeFormatter.ofPattern(s"d'${ordinalSuffix(dayOfMonth)}' MMM yyyy")
    formatter.format(dateTime)
  }

  def ordinalSuffix(number: Int): String = {
    val hundredRemainder = number % 100
    hundredRemainder match {
      case 11 | 12 | 13 => "th"
      case _ => number % 10 match {
        case 1 => "st"
        case 2 => "nd"
        case 3 => "rd"
        case _ => "th"
      }
    }
  }

}
