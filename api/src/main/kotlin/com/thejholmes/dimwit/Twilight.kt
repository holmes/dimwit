package com.thejholmes.dimwit

import java.time.LocalDate
import java.time.LocalTime

/** Finds sunrise/sunset and solar noon including offsets based on the provider given at construction. */
data class Twilight(val date: LocalDate, val twilightBegin: LocalTime, val sunrise: LocalTime,
        val solarNoon: LocalTime, val sunset: LocalTime, val twilightEnd: LocalTime) {
    fun twilightBegin(offset: Int = 0): LocalTime {
        return twilightBegin.plusMinutes(offset)
    }

    fun sunrise(offset: Int = 0): LocalTime {
        return sunrise.plusMinutes(offset)
    }

    fun solarNoon(offset: Int = 0): LocalTime {
        return solarNoon.plusMinutes(offset)
    }

    fun sunset(offset: Int = 0): LocalTime {
        return sunset.plusMinutes(offset)
    }

    fun twilightEnd(offset: Int = 0): LocalTime {
        return twilightEnd.plusMinutes(offset)
    }

    companion object {
        val DEFAULT: Twilight = Twilight(
                LocalDate.now(),
                LocalTime.of(6, 30),
                LocalTime.of(7, 0),
                LocalTime.of(12, 30),
                LocalTime.of(18, 30),
                LocalTime.of(19, 0)
        )
    }
}

private fun LocalTime.plusMinutes(offset: Int): LocalTime {
    return this.plusMinutes(offset.toLong())
}
