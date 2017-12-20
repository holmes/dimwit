package com.thejholmes.dimwit.twilight

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.thejholmes.dimwit.Twilight
import com.thejholmes.dimwit.TwilightProvider
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class TwilightProviderUnitTest {
    lateinit var twilightProvider: TwilightProvider
    lateinit var testDate: LocalDate

    @Before
    fun setUp() {
        val gson = Gson()
        testDate = LocalDate.of(2017, 2, 12)

        twilightProvider = TwilightProvider(gson, File("src/test/resources"))
    }

    @Test
    fun testParsingDatesFromFile() {
        twilightProvider.refresh(testDate)
        val twilight = twilightProvider.twilight(testDate)

        assertThat(twilight).isNotNull()
        assertThat(twilight.twilightBegin).isEqualTo(LocalTime.of(6, 33, 27))
        assertThat(twilight.sunrise).isEqualTo(LocalTime.of(7, 0, 21))
        assertThat(twilight.solarNoon).isEqualTo(LocalTime.of(12, 22, 39))
        assertThat(twilight.sunset).isEqualTo(LocalTime.of(17, 44, 56))
        assertThat(twilight.twilightEnd).isEqualTo(LocalTime.of(18, 11, 51))
    }

    @Test
    fun testSubscriptions() {
        val now = BehaviorSubject.create<LocalDate>()
        val disposable = twilightProvider.start(now)

        assertThat(twilightProvider.twilight(testDate)).isEqualTo(Twilight.DEFAULT)
        now.onNext(testDate)
        assertThat(twilightProvider.twilight(testDate)).isNotEqualTo(Twilight.DEFAULT)

        // Refresh loads tomorrow as well.
        val tomorrow = testDate.plusDays(1)
        assertThat(twilightProvider.twilight(tomorrow)).isNotEqualTo(Twilight.DEFAULT)

        val today = LocalDate.now()
        assertThat(twilightProvider.twilight(today)).isEqualTo(Twilight.DEFAULT)
        disposable.dispose()
        now.onNext(testDate)
        assertThat(twilightProvider.twilight(today)).isEqualTo(Twilight.DEFAULT)
    }
}
