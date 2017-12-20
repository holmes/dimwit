package com.thejholmes.dimwit.twilight

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TwilightUnitTest {
  lateinit var twilightResult: TwilightResult
  lateinit var twilight: Twilight

  @Before fun setUp() {
    // Twilight begins at 6:58
    val twilightBegin = LocalTime.of(6, 58)

    // Sunrise is at 7:21
    val sunrise = LocalTime.of(7, 21)

    // Solar Noon is at 12:26
    val solarNoon = LocalTime.of(12, 26)

    // Sunset is at 5:41
    val sunset = LocalTime.of(17, 41)

    // Twilight ends at 18:21
    val twilightEnd = LocalTime.of(18, 21)

    twilightResult = TwilightResult(LocalDate.now(), twilightBegin, sunrise, solarNoon, sunset, twilightEnd)
    twilight = Twilight({ twilightResult })
  }

  @Test fun testSunrise() {
    val sunrise = twilight.sunrise()()
    assertThat(sunrise.hour).isEqualTo(7)
    assertThat(sunrise.minute).isEqualTo(21)
  }

  @Test fun testSunriseOffset() {
    val sunrise = twilight.sunrise(-30)()
    assertThat(sunrise.hour).isEqualTo(6)
    assertThat(sunrise.minute).isEqualTo(51)
  }

  @Test fun testSolarNoon() {
    val solarNoon = twilight.solarNoon()()
    assertThat(solarNoon.hour).isEqualTo(12)
    assertThat(solarNoon.minute).isEqualTo(26)
  }

  @Test fun testSolarNoonOffset() {
    val solarNoon = twilight.solarNoon(-30)()
    assertThat(solarNoon.hour).isEqualTo(11)
    assertThat(solarNoon.minute).isEqualTo(56)
  }

  @Test fun testSunset() {
    val sunset = twilight.sunset()()
    assertThat(sunset.hour).isEqualTo(17)
    assertThat(sunset.minute).isEqualTo(41)
  }

  @Test fun testSunsetOffset() {
    val sunset = twilight.sunset(30)()
    assertThat(sunset.hour).isEqualTo(18)
    assertThat(sunset.minute).isEqualTo(11)
  }

  @Test fun testTwilightBegin() {
    val localTime = twilight.twilightBegin()()
    assertThat(localTime.hour).isEqualTo(6)
    assertThat(localTime.minute).isEqualTo(58)
  }

  @Test fun testTwilightBeginOffset() {
    val localTime = twilight.twilightBegin(-30)()
    assertThat(localTime.hour).isEqualTo(6)
    assertThat(localTime.minute).isEqualTo(28)
  }

  @Test fun testTwilightEnd() {
    val localTime = twilight.twilightEnd()()
    assertThat(localTime.hour).isEqualTo(18)
    assertThat(localTime.minute).isEqualTo(21)
  }

  @Test fun testTwilightEndOffset() {
    val localTime = twilight.twilightEnd(30)()
    assertThat(localTime.hour).isEqualTo(18)
    assertThat(localTime.minute).isEqualTo(51)
  }
}
