package com.thejholmes.dimwit

import java.time.Duration
import java.time.LocalTime

/**
 * A TimeFrame is a description of a light level. These are stacked together to
 * represent how we want to light up a room at a certain time of day.
 */
data class TimeFrame(val endTime: () -> LocalTime, val lowLevel: Int, val highLevel: Int) {
  fun contains(currentTime: LocalTime): Boolean = endTime().isAfter(currentTime)

  companion object Factory {
    fun staticTimeFrame(time: LocalTime, lowLevel: Int, highLevel: Int): TimeFrame
            = TimeFrame({ time }, lowLevel, highLevel)
  }
}

/**
 * A LightZone contains a list of {@link TimeFrame}s. Given a time, the LightZone can calculate
 * what the level should be.
 *
 * A LightZone can also have dependent zones, those that should be adjusted when this zone is adjusted.
 * In practice: if I turn on the family room lights between midnight -> sunrise then I also want to
 * turn on the kitchen lights, because that's probably where I'm going.
 */
data class  LightZone(val deviceId: String, val timeFrames: List<TimeFrame>, val subZones: List<DependentZone>) {
  data class DependentZone(val deviceId: String, private val timeFrames: List<DependentTimeFrame>) {
    data class DependentTimeFrame(val startTime: () -> LocalTime, val endTime: () -> LocalTime)
    fun contains(now: ()->LocalTime): Boolean =
            timeFrames.any { it.startTime() < now() && it.endTime() > now() }
  }

  fun highLevel(now: () -> LocalTime): Int {
    return currentFrame(now).highLevel
  }

  fun calculateLightLevel(now: () -> LocalTime): Int {
    // Always use lowLevel in the morning.
    if (isInFirstFrame(now)) {
      return timeFrames.first().lowLevel
    }

    val startValue = previousFrame(now).lowLevel
    val endValue = currentFrame(now).lowLevel

    val startTime = previousFrame(now).endTime()
    val endTime = currentFrame(now).endTime()
    val nowTime = now()

    val frameLength = Duration.between(startTime, endTime)
    val inset = Duration.between(startTime, nowTime)
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

    return calculatedLevel
  }

  fun isInFirstFrame(now: ()->LocalTime): Boolean = timeFrames[0].contains(now())

  fun previousFrame(now: ()->LocalTime): TimeFrame = timeFrames.last { !it.contains(now()) }

  fun currentFrame(now: ()->LocalTime): TimeFrame = timeFrames.first { it.contains(now()) }
}

/** Simple store of all the {@link LightZone}s. Query them with {@link #zone}. */
class LightZones {
  private val zones = HashMap<String, LightZone>()

  fun zone(deviceId: String): LightZone? = zones[deviceId]
}
