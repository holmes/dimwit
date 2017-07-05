package com.thejholmes.dimwit

import java.time.Duration
import java.time.LocalTime

/**
 * Represents a room with various light levels defined by TimeFrame(s). TimeFrame(s) are defined by
 * their endTime(s) and are stored consecutively in the List.
 */
class DimCalculator(val now: () -> LocalTime) {
  data class ToggleLightResult(val value: Int)

  fun toggleLights(zone: LightZone, currentValue: Int): ToggleLightResult {
    val calculatedLevel = calculateLightLevel(zone)

    val lightLevel = when {
      shouldUseCalculatedLevel(zone, currentValue, calculatedLevel) -> calculatedLevel
      else -> zone.currentFrame(now).highLevel
    }

    return ToggleLightResult(lightLevel)
  }

  private fun calculateLightLevel(zone: LightZone): Int {
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

  internal fun shouldUseCalculatedLevel(zone: LightZone, currentValue: Int, calculatedLevel: Int): Boolean {
    val currentFrame = zone.currentFrame(now)
    val currentHigh = currentFrame.highLevel

    return currentValue < calculatedLevel || currentValue >= currentHigh
  }
}
