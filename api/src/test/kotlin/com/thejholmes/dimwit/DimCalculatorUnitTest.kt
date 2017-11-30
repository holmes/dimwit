package com.thejholmes.dimwit

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import com.thejholmes.dimwit.LightZone.DependentZone
import com.thejholmes.dimwit.LightZone.DependentZone.DependentTimeFrame
import com.thejholmes.dimwit.TimeFrame.Factory.StaticTimeFrame
import org.junit.Before
import org.junit.Test
import java.time.LocalTime
import java.time.LocalTime.MAX
import java.time.LocalTime.MIDNIGHT

class DimCalculatorUnitTest {
  private lateinit var dimCalculator: DimCalculator
  private lateinit var zone: LightZone
  private lateinit var now: LocalTime

  @Before fun setUp() {
    val timeFrames = listOf(
            StaticTimeFrame(MIDNIGHT.plusHours(6), 10, 35),
            StaticTimeFrame(MIDNIGHT.plusHours(8), 30, 60),
            StaticTimeFrame(MIDNIGHT.plusHours(13), 60, 85),
            StaticTimeFrame(MIDNIGHT.plusHours(20), 30, 60),
            StaticTimeFrame(MAX, 1, 35)
    )

    zone = LightZone(11, setOf(
            DependentZone(17, listOf(DependentTimeFrame({ MIDNIGHT }, { MIDNIGHT.plusHours(8) })))
    ), timeFrames)

    val childZone = LightZone(17, emptySet(), timeFrames)

    val lightZones = mock<LightZones> {
      on { zone(com.nhaarman.mockito_kotlin.any()) }.doAnswer { childZone }
    }

    dimCalculator = DimCalculator(lightZones, { now })
  }

  @Test fun testAutoDimLightIsOff() {
    assertThat(dimCalculator.autoDim(zone, 0).dimLevel).isEqualTo(-1)
    assertThat(dimCalculator.autoDim(zone, 0).needsReschedule).isFalse()
  }

  @Test fun testDoNotAutoDimLightInFirstFrame() {
    val firstFrame = zone.timeFrames[0]
    now = firstFrame.endTime().minusHours(1)

    assertThat(dimCalculator.autoDim(zone, 9).dimLevel).isEqualTo(-1)
    assertThat(dimCalculator.autoDim(zone, 9).needsReschedule).isFalse()
  }

  @Test fun testAutoDimLightAtVeryLowLevelDoNothing() {
    val currentFrame = zone.timeFrames[1]
    now = currentFrame.endTime().minusHours(1)

    assertThat(dimCalculator.autoDim(zone, 1).dimLevel).isEqualTo(-1)
    assertThat(dimCalculator.autoDim(zone, 1).needsReschedule).isFalse()
  }

  @Test fun testAutoDimOverHighLevelDoNothing() {
    val currentFrame = zone.timeFrames[2]
    now = currentFrame.endTime().minusHours(1)

    assertThat(dimCalculator.autoDim(zone, 88).dimLevel).isEqualTo(-1)
    assertThat(dimCalculator.autoDim(zone, 88).needsReschedule).isFalse()
  }

  @Test fun testAutoDimLightNearCurrentLevelDims() {
    val testFrame = zone.timeFrames[2]
    now = testFrame.endTime().minusMinutes(10)

    assertThat(dimCalculator.autoDim(zone, 58).dimLevel).isEqualTo(59)
    assertThat(dimCalculator.autoDim(zone, 58).needsReschedule).isFalse()

    // Close ones need rescheduling.
    assertThat(dimCalculator.autoDim(zone, 57).needsReschedule).isTrue()
    assertThat(dimCalculator.autoDim(zone, 56).needsReschedule).isFalse()
    assertThat(dimCalculator.autoDim(zone, 55).needsReschedule).isFalse()
  }

  @Test fun testAutoDimLightNotNearCurrentDoNothing() {
    val testFrame = zone.timeFrames[2]
    val currentValue = testFrame.lowLevel + 10

    now = testFrame.endTime().minusMinutes(10)
    assertThat(dimCalculator.autoDim(zone, currentValue).dimLevel).isEqualTo(-1)
    assertThat(dimCalculator.autoDim(zone, currentValue).needsReschedule).isFalse()
  }

  @Test fun testAutoDimForReal() {
    val currentFrame = zone.timeFrames[2]

    now = currentFrame.endTime().minusMinutes(10)
    assertThat(dimCalculator.autoDim(zone, 58).dimLevel).isEqualTo(59)
    assertThat(dimCalculator.autoDim(zone, 58).needsReschedule).isFalse()
  }

  @Test fun testAutoDimForSameValue() {
    val currentFrame = zone.timeFrames[2]

    now = currentFrame.endTime().minusMinutes(10)
    assertThat(dimCalculator.autoDim(zone, 59).dimLevel).isEqualTo(59)
    assertThat(dimCalculator.autoDim(zone, 59).needsReschedule).isFalse()
  }

  @Test fun testToggleLightsWhenOffInFirstFrame() {
    val currentFrame = zone.timeFrames[0]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.toggleLights(zone, 0).results.first().value
    assertThat(result).isEqualTo(10)
  }

  @Test fun testToggleLightsWhenOff() {
    val currentFrame = zone.timeFrames[2]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.toggleLights(zone, 0).results.first().value
    assertThat(result).isEqualTo(50)
  }

  @Test fun testToggleLightsWhenOffInEvening() {
    val currentFrame = zone.timeFrames[3]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.toggleLights(zone, 0).results.first().value
    assertThat(result).isEqualTo(38)
  }

  @Test fun testToggleLightsWhenWellBelowLevelTakesYouToLowLevel() {
    val currentFrame = zone.timeFrames[2]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.toggleLights(zone, 32).results.first().value
    assertThat(result).isEqualTo(50)
  }

  @Test fun testToggleLightsWhenAtLowLevel() {
    val currentFrame = zone.timeFrames[2]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.toggleLights(zone, 50).results.first().value
    assertThat(result).isEqualTo(currentFrame.highLevel)
  }

  @Test fun testToggleLightsWhenJustAboveLowLevelTakesYouToHigh() {
    val currentFrame = zone.timeFrames[2]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.toggleLights(zone, 52).results.first().value
    assertThat(result).isEqualTo(currentFrame.highLevel)
  }

  @Test fun testToggleLightsWhenAtLowLevelInEvening() {
    val currentFrame = zone.timeFrames[3]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.toggleLights(zone, 38).results.first().value
    assertThat(result).isEqualTo(currentFrame.highLevel)
  }

  @Test fun testToggleLightsWhenJustAboveLowLevelTakesYouToHighLevel() {
    val currentFrame = zone.timeFrames[2]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.toggleLights(zone, 52).results.first().value
    assertThat(result).isEqualTo(currentFrame.highLevel)
  }

  @Test fun testToggleLightsWhenJustAboveHighLevelInEvening() {
    val currentFrame = zone.timeFrames[3]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.toggleLights(zone, currentFrame.highLevel + 3).results.first().value
    assertThat(result).isEqualTo(38)
  }

  @Test fun testToggleLightsWhenNearHighTakesYouToHigh() {
    val currentFrame = zone.timeFrames[2]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.toggleLights(zone, currentFrame.highLevel - 2).results.first().value
    assertThat(result).isEqualTo(85)
  }

  @Test fun testToggleLightsWhenOnHigh() {
    val currentFrame = zone.timeFrames[2]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.toggleLights(zone, currentFrame.highLevel).results.first().value
    assertThat(result).isEqualTo(50)
  }

  @Test fun testKitchenLightThatWontDim() {
    val currentFrame = zone.timeFrames[3]

    now = currentFrame.endTime().minusMinutes(30)
    val result = dimCalculator.toggleLights(zone, currentFrame.highLevel).results.first().value
    assertThat(result).isEqualTo(33)
  }

  @Test fun testToggleLightsWhenAboveHigh() {
    val currentFrame = zone.timeFrames[2]

    now = currentFrame.endTime().minusMinutes(100)
    val result = dimCalculator.toggleLights(zone, currentFrame.highLevel + 7).results.first().value
    assertThat(result).isEqualTo(50)
  }

  @Test fun testMultipleResultsWhenLightsOn() {
    val currentFrame = zone.timeFrames[0]
    now = currentFrame.endTime().minusMinutes(100)

    val result = dimCalculator.toggleLights(zone, 20)
    assertThat(result.results.size).isEqualTo(1)
  }

  @Test fun testMultipleResultsWhenLightsOff() {
    val currentFrame = zone.timeFrames[0]
    now = currentFrame.endTime().minusMinutes(100)

    val result = dimCalculator.toggleLights(zone, 0)
    assertThat(result.results.size).isEqualTo(2)
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
