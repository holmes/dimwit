package com.thejholmes.dimwit

import java.time.LocalDate
import java.time.LocalTime

data class TwilightResult(val date: LocalDate, val twilightBegin: LocalTime, val sunrise: LocalTime,
        val solarNoon: LocalTime, val sunset: LocalTime, val twilightEnd: LocalTime)

/** Finds sunrise/sunset and solar noon for use in [TimeFrame]s. */
class Twilight(private val twilightProvider: () -> TwilightResult) {
  fun twilightBegin(offset: Int = 0): () -> LocalTime
          = { twilightProvider().twilightBegin.plusMinutes(offset) }

  fun sunrise(offset: Int = 0): () -> LocalTime
          = { twilightProvider().sunrise.plusMinutes(offset) }

  fun solarNoon(offset: Int = 0): () -> LocalTime =
          { twilightProvider().solarNoon.plusMinutes(offset) }

  fun sunset(offset: Int = 0): () -> LocalTime
          = { twilightProvider().sunset.plusMinutes(offset) }

  fun twilightEnd(offset: Int = 0): () -> LocalTime =
          { twilightProvider().twilightEnd.plusMinutes(offset) }
}

private fun LocalTime.plusMinutes(offset: Int): LocalTime {
  return this.plusMinutes(offset.toLong())
}

