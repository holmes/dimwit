package com.thejholmes.dimwit

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class LightLevels(val now: LocalDateTime, val lowLevel: Int, val highLevel: Int)

/**
 * A TimeFrame is a description of a light level. These are stacked together to
 * represent how we want to light up a room at a certain time of day.
 */
data class TimeFrame(val endTimeValue: String, val lowLevel: Int, val highLevel: Int) {
    internal fun contains(twilight: Twilight, now: LocalTime): Boolean {
        return twilight.parse(endTimeValue).isAfter(now)
    }
}

data class ConvertedTimeFrame(val endTime: LocalTime, val lowLevel: Int, val highLevel: Int)

/**
 * A LightZone contains a list of {@link TimeFrame}s. Given a time, the LightZone can calculate
 * what the level should be.
 *
 * A LightZone can also have dependent zones, those that should be adjusted when this zone is adjusted.
 * In practice: if I turn on the family room lights between midnight -> sunrise then I also want to
 * turn on the kitchen lights, because that's probably where I'm going.
 */
data class LightZone(
        val deviceId: String,
        val timeFrames: List<TimeFrame>,
        val twilight: Observable<Twilight>,
        val now: Observable<LocalTime>
) {
    val lightLevels: Observable<LightLevels> = Observable
            .combineLatest(now, twilight,
                    BiFunction<LocalTime, Twilight, LightLevels> { now, twilight -> calculateLevels(twilight, now) })

    internal fun calculateLevels(twilight: Twilight, now: LocalTime): LightLevels {
        // Always use lowLevel in the morning.
        if (isInFirstFrame(twilight, now)) {
            val first = timeFrames.first()
            return LightLevels(LocalDateTime.of(twilight.date, now), first.lowLevel, first.highLevel)
        }

        val previousFrame = previousFrame(twilight, now)
        val currentFrame = currentFrame(twilight, now)

        val startValue = previousFrame.lowLevel
        val endValue = currentFrame(twilight, now).lowLevel

        val startTime = previousFrame.endTime
        val endTime = currentFrame.endTime

        val frameLength = Duration.between(startTime, endTime)
        val inset = Duration.between(startTime, now)
        val ratio: Double = (inset.toMinutes().toDouble() / frameLength.toMinutes().toDouble())
        val adjustedRatio = (Math.abs(startValue - endValue) * ratio).toInt()

        var calculatedLevel: Int
        if (startValue > endValue) {
            calculatedLevel = Math.abs(adjustedRatio - startValue)
            calculatedLevel = Math.max(calculatedLevel, endValue)
        } else {
            calculatedLevel = adjustedRatio + startValue
            calculatedLevel = Math.max(calculatedLevel, startValue)
        }

        return LightLevels(LocalDateTime.of(twilight.date, now), calculatedLevel, currentFrame.highLevel)
    }

    private fun isInFirstFrame(twilight: Twilight, now: LocalTime): Boolean {
        return timeFrames[0].contains(twilight, now)
    }

    private fun previousFrame(twilight: Twilight, now: LocalTime): ConvertedTimeFrame {
        return with(timeFrames.last { !it.contains(twilight, now) }) {
            ConvertedTimeFrame(twilight.parse(endTimeValue), lowLevel, highLevel)
        }
    }

    private fun currentFrame(twilight: Twilight, now: LocalTime): ConvertedTimeFrame {
        return with(timeFrames.first { it.contains(twilight, now) }) {
            ConvertedTimeFrame(twilight.parse(endTimeValue), lowLevel, highLevel)
        }
    }
}
