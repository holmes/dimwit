package com.thejholmes.dimwit

import com.google.common.truth.Truth.assertThat
import com.thejholmes.dimwit.twilight.FakeTwilight
import com.thejholmes.dimwit.twilight.Twilight
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class TimeFrameUnitTest {
  lateinit var twilight: Twilight
  lateinit var now: LocalTime

  @Before fun setUp() {
    twilight = FakeTwilight.twilight
    now = LocalTime.of(8, 0)
  }

  @Test fun testContainsTrue() {
    val timeFrame = TimeFrame(now.plusHours(3).toString(), 12, 65)
    assertThat(timeFrame.contains(twilight, now)).isTrue()
  }

  @Test fun testContainsFalse() {
    val timeFrame = TimeFrame(now.minusHours(3).toString(), 12, 65)
    assertThat(timeFrame.contains(twilight, now)).isFalse()
  }
}
