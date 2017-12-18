package com.thejholmes.dimwit

import com.google.gson.Gson
import com.thejholmes.dimwit.LightZone.DependentZone
import com.thejholmes.dimwit.LightZone.DependentZone.DependentTimeFrame
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
class LightZoneParser(private val gson: Gson, private val twilight: Twilight) {
    fun parse(input: String): LightZone {
        val parsedZone = gson.fromJson(input, ParsedLightZone::class.java)
        return parsedZone.lightZone(twilight)
    }
}

data class ParsedTimeFrame(val endTime: String, val lowLevel: Int, val highLevel: Int) {
    fun timeFrame(twilight: Twilight): TimeFrame {
        return TimeFrame(twilight.parse(endTime), lowLevel, highLevel)
    }
}

data class ParsedLightZone(val deviceId: String, val timeFrames: Array<ParsedTimeFrame>,
        val subZones: Array<ParsedDependentZone>) {
    fun lightZone(twilight: Twilight): LightZone {
        return LightZone(deviceId, timeFrames.map { it.timeFrame(twilight) },
                subZones.map { it.lightZone(twilight) })
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

data class ParsedDependentTimeFrame(val startTime: String, val endTime: String) {
    fun timeFrame(twilight: Twilight): DependentTimeFrame {
        return DependentTimeFrame(twilight.parse(startTime), twilight.parse(endTime))
    }
}

data class ParsedDependentZone(val deviceId: String,
        val timeFrames: Array<ParsedDependentTimeFrame>) {
    fun lightZone(twilight: Twilight): DependentZone {
        return DependentZone(deviceId, timeFrames.map { it.timeFrame(twilight) })
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParsedDependentZone

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

internal fun Twilight.parse(input: String): () -> LocalTime {
    val (base, offset) = input.split(":")
    val minuteOffset = offset.toIntOrNull() ?: 0

    // Check rules now - otherwise they'll be runtime errors down the road.
    try {
        LocalTime.of(0, minuteOffset.absoluteValue)
    } catch (e: DateTimeException) {
        throw IllegalArgumentException("Illegal offset: $offset. ${e.message}", e)
    }

    val legalValues = arrayOf("twilightBegin", "sunrise", "solarNoon", "sunset", "twilightEnd")
    when {
        input.first().isDigit() -> try {
            // Parse time now.
            val parsedTime = LocalTime.parse(input)
            return { parsedTime }
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Unable to convert $input to hh:mm. ${e.message}", e)
        }
        legalValues.contains(base).not() -> {
            throw IllegalArgumentException(
                    "Unknown base: $base. Must be in one of: ${legalValues.joinToString(", ")}")
        }
    }

    return when {
        base.equals("twilightBegin", true) -> twilightBegin(minuteOffset)
        base.equals("sunrise", true) -> sunrise(minuteOffset)
        base.equals("solarNoon", true) -> solarNoon(minuteOffset)
        base.equals("sunset", true) -> sunset(minuteOffset)
        base.equals("twilightEnd", true) -> twilightEnd(minuteOffset)
        else -> {
            throw IllegalStateException("Unsure how you even got here for input: $input")
        }
    }
}
