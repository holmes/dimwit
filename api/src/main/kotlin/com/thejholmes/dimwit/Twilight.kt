package com.thejholmes.dimwit

import java.time.LocalDate
import java.time.LocalTime


/** Finds sunrise/sunset and solar noon for use in [TimeFrame]s. */
class Twilight(val twilightProvider: () -> TwilightResult) {
  fun twilightBegin(offset: Int = 0): () -> LocalTime {
    return { addOffset(offset, twilightProvider().twilightBegin) }
  }

  fun sunrise(offset: Int = 0): () -> LocalTime {
    return { addOffset(offset, twilightProvider().sunrise) }
  }

  fun sunset(offset: Int = 0): () -> LocalTime {
    return { addOffset(offset, twilightProvider().sunset) }
  }

  fun solarNoon(offset: Int = 0): () -> LocalTime {
    return { addOffset(offset, twilightProvider().solarNoon) }
  }

  fun twilightEnd(offset: Int = 0): () -> LocalTime {
    return { addOffset(offset, twilightProvider().twilightEnd) }
  }

  private fun addOffset(offset: Int, baseDate: LocalTime): LocalTime {
    return baseDate.plusMinutes(offset.toLong())
  }
}

data class TwilightResult(val date: LocalDate, val twilightBegin: LocalTime, val sunrise: LocalTime, val solarNoon: LocalTime, val sunset: LocalTime, val twilightEnd: LocalTime)

