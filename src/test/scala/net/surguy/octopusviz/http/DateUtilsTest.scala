package net.surguy.octopusviz.http

import org.specs2.mutable.Specification

import java.time.LocalDate

class DateUtilsTest extends Specification {

  import DateUtils.*

  "formatting a date" should {
    "give the expected output" in {
      formatDate(LocalDate.of(2023, 11, 30)) must beEqualTo("30th Nov 2023")
      formatDate(LocalDate.of(2023, 1, 2)) must beEqualTo("2nd Jan 2023")
      formatDate(LocalDate.of(2021, 2, 1)) must beEqualTo("1st Feb 2021")
    }
  }

  "working out the ordinal suffix" should {
    "format low numbers correctly" in {
      ordinalSuffix(0) must beEqualTo("th")
      ordinalSuffix(1) must beEqualTo("st")
      ordinalSuffix(2) must beEqualTo("nd")
      ordinalSuffix(3) must beEqualTo("rd")
      ordinalSuffix(4) must beEqualTo("th")
    }
    "format teens correctly" in {
      ordinalSuffix(10) must beEqualTo("th")
      ordinalSuffix(11) must beEqualTo("th")
      ordinalSuffix(12) must beEqualTo("th")
      ordinalSuffix(13) must beEqualTo("th")
      ordinalSuffix(14) must beEqualTo("th")
    }
    "format fifties correctly" in {
      ordinalSuffix(50) must beEqualTo("th")
      ordinalSuffix(51) must beEqualTo("st")
      ordinalSuffix(52) must beEqualTo("nd")
      ordinalSuffix(53) must beEqualTo("rd")
      ordinalSuffix(54) must beEqualTo("th")
    }
    "format numbers over one hundred correctly" in { ordinalSuffix(101) must beEqualTo("st") }
    "format numbers over one thousand correctly" in { ordinalSuffix(1101) must beEqualTo("st") }
  }

}
