package com.thejholmes.dimwit

import com.thejholmes.dimwit.DimCalculator.ToggleLightResult.ToggleLightValue
import java.time.LocalTime

/**
 * Represents a room with various light levels defined by TimeFrame(s). TimeFrame(s) are defined by
 * their endTime(s) and are stored consecutively in the List.
 */
class DimCalculator(private val lightZones: LightZones, private val now: ()->LocalTime) {
  /**
   * Toggling on a light can result in other lights being turned on at the same time. One example is between
   * midnight and twilight: turning on the family room light should turn on the kitchen as well.
   */
  data class ToggleLightResult(val results: List<ToggleLightValue>) {
    data class ToggleLightValue(val deviceId: String, val value: Int)
  }

  data class AutoDimResult(val dimLevel: Int, val needsReschedule: Boolean) {
    companion object Factory {
      val NO_CHANGE: AutoDimResult = AutoDimResult(-1, false)
    }
  }

  /**
   * Dim (or brighten) the lights if the calculated level is near the currentValue. We check that it's near
   * because a person in the room might have turned the lights up intentionally. We don't want to turn them
   * down if that's the case.
   */
  fun autoDim(zone: LightZone, currentValue: Int): AutoDimResult {
    if (currentValue == 0 || zone.isInFirstFrame(now)) {
      return AutoDimResult.NO_CHANGE
    }

    val calculatedLevel = zone.calculateLightLevel(now)
    val delta = Math.abs(calculatedLevel - currentValue)
    val allowed = delta <= 2

    return if (allowed) AutoDimResult(calculatedLevel, delta > 1) else AutoDimResult.NO_CHANGE
  }

  /**
   * If the lights are off, set them to the low value.
   *
   * If they're already on, brighten or dim them depending on how bright they currently are.
   * Pick the level opposite of what the current value is closest to.
   */
  fun toggleLights(zone: LightZone, currentValue: Int): ToggleLightResult {
    val calculatedLevel = zone.calculateLightLevel(now)

    val lightLevel = when {
      shouldUseCalculatedLevel(zone, currentValue, calculatedLevel) -> calculatedLevel
      else -> zone.currentFrame(now).highLevel
    }

    val results = listOf(ToggleLightValue(zone.deviceId, lightLevel))
    val childResults: List<ToggleLightValue> =
            if (currentValue == 0) {
              zone.subZones
                      .filter { it.contains(now) }
                      .mapNotNull { lightZones.zone(it.deviceId) }
                      .map { toggleLights(it, 0) }
                      .flatMap { it.results }
            } else {
              emptyList()
            }

    return ToggleLightResult(results.plus(childResults))
  }

  private fun shouldUseCalculatedLevel(zone: LightZone, currentValue: Int, calculatedLevel: Int): Boolean {
    val currentFrame = zone.currentFrame(now)
    val currentHigh = currentFrame.highLevel

    return currentValue < calculatedLevel || currentValue >= currentHigh
  }
}
