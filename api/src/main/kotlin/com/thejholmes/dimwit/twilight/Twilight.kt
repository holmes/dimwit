package com.thejholmes.dimwit.twilight

import java.time.LocalDate
import java.time.LocalTime

///** Finds sunrise/sunset and solar noon for use in [TimeFrame]s. */
//data class Twilight(val date: LocalDate, val twilightBegin: LocalTime, val sunrise: LocalTime,
//        val solarNoon: LocalTime, val sunset: LocalTime, val twilightEnd: LocalTime) {
//
//  fun twilightBegin(offset: Int = 0): () -> LocalTime
//          = { twilightBegin.plusMinutes(offset) }
//
//  fun sunrise(offset: Int = 0): () -> LocalTime
//          = { sunrise.plusMinutes(offset) }
//
//  fun solarNoon(offset: Int = 0): () -> LocalTime =
//          { solarNoon.plusMinutes(offset) }
//
//  fun sunset(offset: Int = 0): () -> LocalTime
//          = { sunset.plusMinutes(offset) }
//
//  fun twilightEnd(offset: Int = 0): () -> LocalTime =
//          { twilightEnd.plusMinutes(offset) }
//}

data class TwilightResult(val date: LocalDate, val twilightBegin: LocalTime, val sunrise: LocalTime,
        val solarNoon: LocalTime, val sunset: LocalTime, val twilightEnd: LocalTime)

/** Finds sunrise/sunset and solar noon for use in [TimeFrame]s. */
class Twilight(val twilightProvider: () -> TwilightResult) {
    fun twilightBegin(offset: Int = 0): () -> LocalTime {
        return { twilightProvider().twilightBegin.plusMinutes(offset) }
    }

    fun sunrise(offset: Int = 0): () -> LocalTime {
        return { twilightProvider().sunrise.plusMinutes(offset) }
    }

    fun solarNoon(offset: Int = 0): () -> LocalTime {
        return { twilightProvider().solarNoon.plusMinutes(offset) }
    }

    fun sunset(offset: Int = 0): () -> LocalTime {
        return { twilightProvider().sunset.plusMinutes(offset) }
    }

    fun twilightEnd(offset: Int = 0): () -> LocalTime {
        return { twilightProvider().twilightEnd.plusMinutes(offset) }
    }
}

private fun LocalTime.plusMinutes(offset: Int): LocalTime {
    return this.plusMinutes(offset.toLong())
}
