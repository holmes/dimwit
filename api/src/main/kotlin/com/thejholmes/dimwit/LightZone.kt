package com.thejholmes.dimwit

import java.time.LocalTime

/**
 * A TimeFrame is a description of a light level. These are stacked together to
 * represent how we want to light up a room at a certain time of day.
 */
data class TimeFrame(val endTime: () -> LocalTime, val lowLevel: Int, val highLevel: Int) {
  fun contains(currentTime: LocalTime): Boolean {
    return endTime().isAfter(currentTime)
  }

  companion object Factory {
    fun StaticTimeFrame(time: LocalTime, lowLevel: Int, highLevel: Int): TimeFrame {
      return TimeFrame({ time }, lowLevel, highLevel)
    }
  }
}


data class  LightZone(val timeFrames: List<TimeFrame>) {
  fun isInFirstFrame(now: () -> LocalTime): Boolean {
    return timeFrames[0].contains(now())
  }

  fun previousFrame(now: () -> LocalTime): TimeFrame {
    return timeFrames.last { !it.contains(now()) }
  }

  fun currentFrame(now: () -> LocalTime): TimeFrame {
    return timeFrames.first { it.contains(now()) }
  }
}


class LightZones(twilight: Twilight) {
  private val zones = HashMap<Int, LightZone>()

  fun zone(deviceId: Int): LightZone? {
    return zones[deviceId]
  }

  private val MIN: () -> LocalTime
    get() = { LocalTime.MIN }

  private val MAX: () -> LocalTime
    get() = { LocalTime.MAX }
}
