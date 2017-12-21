package com.thejholmes.dimwit

import com.google.gson.Gson
import io.reactivex.Observable
import java.time.DateTimeException
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.util.Arrays
import kotlin.math.absoluteValue

/**
 * Used to parse out LightZone(s) from a configuration file. These must be processed when loaded,
 * but the times are not evaluated until DimCalculator uses them.
 *
 * The idea is that a configuration file is loaded once, but the times must be calculated multiple
 * times on different days, and thus will have different times.
 */
class LightZoneParser(
        private val gson: Gson,
        private val twilight: Observable<Twilight>,
        private val now: Observable<LocalTime>
) {
    fun parse(input: String): LightZone {
        val parsedZone = gson.fromJson(input, ParsedLightZone::class.java)
        return parsedZone.lightZone(twilight, now)
    }
}

data class ParsedLightZone(val deviceId: String, val timeFrames: Array<ParsedTimeFrame>) {
    data class ParsedTimeFrame(val endTime: String, val lowLevel: Int, val highLevel: Int) {
        val timeFrame: TimeFrame
            get() {
                // Check rules now - otherwise they'll be runtime errors down the road.
                validate()
                return TimeFrame(endTime, lowLevel, highLevel)
            }

        private fun validate() {
            val (base, offset) = endTime.split(":")
            val minuteOffset = offset.toIntOrNull() ?: 0

            val legalValues = arrayOf("twilightBegin", "sunrise", "solarNoon", "sunset", "twilightEnd")
            when {
                endTime.first().isDigit() -> try {
                    // Parse time now.
                    LocalTime.of(base.toInt(), minuteOffset)
                } catch (e: DateTimeException) {
                    throw IllegalArgumentException("Unable to convert $endTime to hh:mm. ${e.message}", e)
                }
                legalValues.contains(base).not() -> {
                    throw IllegalArgumentException(
                            "Unknown base: $base. Must be in one of: ${legalValues.joinToString(", ")}")
                }
            }
        }
    }

    fun lightZone(twilight: Observable<Twilight>, now: Observable<LocalTime>): LightZone {
        return LightZone(deviceId, timeFrames.map { it.timeFrame }, twilight, now)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParsedLightZone

        if (deviceId != other.deviceId) return false
        if (!Arrays.equals(timeFrames, other.timeFrames)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deviceId.hashCode()
        result = 31 * result + Arrays.hashCode(timeFrames)
        return result
    }
}

internal fun Twilight.parse(input: String): LocalTime {
    val (base, offset) = input.split(":")
    val minuteOffset = offset.toIntOrNull() ?: 0

    return when {
        base.equals("twilightBegin", true) -> twilightBegin(minuteOffset)
        base.equals("sunrise", true) -> sunrise(minuteOffset)
        base.equals("solarNoon", true) -> solarNoon(minuteOffset)
        base.equals("sunset", true) -> sunset(minuteOffset)
        base.equals("twilightEnd", true) -> twilightEnd(minuteOffset)
        else -> { LocalTime.of(base.toInt(), minuteOffset) }
    }
}
