package com.thejholmes.dimwit

import java.time.Duration
import java.time.LocalTime

/**
 * Represents a room with various light levels defined by TimeFrame(s). TimeFrame(s) are defined by
 * their endTime(s) and are stored consecutively in the List.
 */
class DimCalculator(val now: () -> LocalTime) {
  data class ToggleLightResult(val lowLevel: Int, val highLevel: Int)

  fun calculateLightLevels(zone: LightZone): ToggleLightResult {
    val calculatedLevel = calculateLowLevel(zone)
    val highLevel = zone.currentFrame(now).highLevel

    return ToggleLightResult(calculatedLevel, highLevel)
  }

  private fun calculateLowLevel(zone: LightZone): Int {
    // Always use lowLevel in the morning.
    if (zone.isInFirstFrame(now)) {
      return zone.timeFrames.first().lowLevel
    }

    val startValue = zone.previousFrame(now).lowLevel
    val endValue = zone.currentFrame(now).lowLevel

    val startTime = zone.previousFrame(now).endTime()
    val endTime = zone.currentFrame(now).endTime()
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
}
