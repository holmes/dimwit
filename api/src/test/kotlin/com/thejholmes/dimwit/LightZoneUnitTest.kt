package com.thejholmes.dimwit

import com.google.common.truth.Truth.assertThat
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Test
import java.time.LocalTime
import java.time.LocalTime.MAX
import java.time.LocalTime.MIDNIGHT
import java.time.LocalTime.of

class LightZoneUnitTest {
  private lateinit var zone: LightZone
  private lateinit var twilight: BehaviorSubject<Twilight>
  private lateinit var now: BehaviorSubject<LocalTime>

  @Before fun setUp() {
    val timeFrames = listOf(
            TimeFrame(MIDNIGHT.plusHours(6).toString(), 10, 35),
            TimeFrame(MIDNIGHT.plusHours(8).toString(), 30, 60),
            TimeFrame(MIDNIGHT.plusHours(13).toString(), 60, 85),
            TimeFrame(MIDNIGHT.plusHours(20).toString(), 30, 60),
            TimeFrame(MAX.toString(), 1, 35)
    )

    twilight = BehaviorSubject.createDefault(Twilight.DEFAULT)
    now = BehaviorSubject.createDefault(of(6, 1))

    zone = LightZone("11", timeFrames, twilight, now)
  }

  @Test fun firstZone() {
    now.onNext(of(2, 10))
    val (_, lowLevel, highLevel) = zone.calculateLevels(twilight.value, now.value)

    assertThat(lowLevel).isEqualTo(10)
    assertThat(highLevel).isEqualTo(35)
  }

  @Test fun lastZone() {
    now.onNext(of(23, 59))
    val (_, lowLevel, highLevel) = zone.calculateLevels(twilight.value, now.value)

    assertThat(lowLevel).isEqualTo(1)
    assertThat(highLevel).isEqualTo(35)
  }

  @Test fun justBeforeBorder() {
    now.onNext(of(12, 59))
    val (_, lowLevel, highLevel) = zone.calculateLevels(twilight.value, now.value)

    assertThat(lowLevel).isEqualTo(59)
    assertThat(highLevel).isEqualTo(85)
  }

  @Test fun onBorder() {
    now.onNext(of(13, 0))
    val (_, lowLevel, highLevel) = zone.calculateLevels(twilight.value, now.value)

    assertThat(lowLevel).isEqualTo(60)
    assertThat(highLevel).isEqualTo(60)
  }

  @Test fun justAfterBorder() {
    now.onNext(of(13, 1))
    val (_, lowLevel, highLevel) = zone.calculateLevels(twilight.value, now.value)

    assertThat(lowLevel).isEqualTo(60)
    assertThat(highLevel).isEqualTo(60)
  }

  @Test fun slightlyAfterBorder() {
    now.onNext(of(13, 30))
    val (_, lowLevel, highLevel) = zone.calculateLevels(twilight.value, now.value)

    assertThat(lowLevel).isEqualTo(58)
    assertThat(highLevel).isEqualTo(60)
  }
}
