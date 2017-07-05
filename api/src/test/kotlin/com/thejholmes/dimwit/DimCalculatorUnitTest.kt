package com.thejholmes.dimwit

import com.google.common.truth.Truth.assertThat
import com.thejholmes.dimwit.TimeFrame.Factory.StaticTimeFrame
import org.junit.Before
import org.junit.Test
import java.time.LocalTime
import java.time.LocalTime.MAX
import java.time.LocalTime.MIDNIGHT

class DimCalculatorUnitTest {
  lateinit var dimCalculator: DimCalculator
  lateinit var zone: LightZone
  lateinit var now: LocalTime

  @Before fun setUp() {
    val timeFrames = listOf(
        StaticTimeFrame(MIDNIGHT.plusHours(6), 10, 35),
        StaticTimeFrame(MIDNIGHT.plusHours(8), 30, 60),
        StaticTimeFrame(MIDNIGHT.plusHours(13), 60, 85),
        StaticTimeFrame(MIDNIGHT.plusHours(20), 30, 60),
        StaticTimeFrame(MAX, 1, 35)
    )

    zone = LightZone(timeFrames)
    dimCalculator = DimCalculator({ now })
  }

  @Test fun testLowLevelInFirstFrame() {
    val currentFrame = zone.timeFrames[0]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.calculateLightLevels(zone).lowLevel
    assertThat(result).isEqualTo(10)
  }

  @Test fun testLowLevelInMiddleFrame() {
    val currentFrame = zone.timeFrames[2]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.calculateLightLevels(zone).lowLevel
    assertThat(result).isEqualTo(50)
  }

  @Test fun testLowLevelInEveningrame() {
    val currentFrame = zone.timeFrames[3]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.calculateLightLevels(zone).lowLevel
    assertThat(result).isEqualTo(38)
  }

  @Test fun testKitchenLightThatWontDim() {
    val currentFrame = zone.timeFrames[3]

    now = currentFrame.endTime().minusMinutes(30)
    val result = dimCalculator.calculateLightLevels(zone).lowLevel
    assertThat(result).isEqualTo(33)
  }

  @Test fun testPreviousFrame() {
    val previousFrame = zone.timeFrames[1]
    val testingFrame = zone.timeFrames[2]

    now = testingFrame.endTime().minusHours(1)
    assertThat(zone.previousFrame({ now })).isSameAs(previousFrame)
  }

  @Test fun testCurrentFrame() {
    val testingFrame = zone.timeFrames[2]
    now = testingFrame.endTime().minusHours(1)
    assertThat(zone.currentFrame({ now })).isSameAs(testingFrame)
  }
}
