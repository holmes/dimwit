package com.thejholmes.dimwit

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.thejholmes.dimwit.Twilight
import com.thejholmes.dimwit.TwilightProvider
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subscribers.TestSubscriber
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class TwilightProviderUnitTest {
    lateinit var twilightProvider: TwilightProvider
    lateinit var testDate: LocalDate
    lateinit var now: BehaviorSubject<LocalDate>

    @Before
    fun setUp() {
        val gson = Gson()
        now = BehaviorSubject.create()
        testDate = LocalDate.of(2017, 2, 12)

        twilightProvider = TwilightProvider(gson, File("src/test/resources"), now)
    }

    @Test
    fun testParsingDatesFromFile() {
        val testSubscriber = TestObserver.create<Twilight>()
        twilightProvider.twilight.subscribe(testSubscriber)
        now.onNext(testDate)

        val twilight = testSubscriber.values().first()

        assertThat(twilight).isNotNull()
        assertThat(twilight.twilightBegin).isEqualTo(LocalTime.of(6, 33, 27))
        assertThat(twilight.sunrise).isEqualTo(LocalTime.of(7, 0, 21))
        assertThat(twilight.solarNoon).isEqualTo(LocalTime.of(12, 22, 39))
        assertThat(twilight.sunset).isEqualTo(LocalTime.of(17, 44, 56))
        assertThat(twilight.twilightEnd).isEqualTo(LocalTime.of(18, 11, 51))
    }

    @Test
    fun testSubscriptions() {
        val testSubscriber = TestObserver.create<Twilight>()
        twilightProvider.twilight.subscribe(testSubscriber)

        testSubscriber.assertNoErrors()
        testSubscriber.assertNoValues()

        now.onNext(testDate)
        assertThat(testSubscriber.values().first()).isNotEqualTo(Twilight.DEFAULT)
    }

    @Test fun testInternals() {
        val tomorrow = testDate.plusDays(1)
        assertThat(twilightProvider.isDateLoaded(testDate)).isFalse()
        assertThat(twilightProvider.isDateLoaded(tomorrow)).isFalse()

        val testSubscriber = TestObserver.create<Twilight>()
        twilightProvider.twilight.subscribe(testSubscriber)
        now.onNext(testDate)

        assertThat(twilightProvider.isDateLoaded(testDate)).isTrue()
        assertThat(twilightProvider.isDateLoaded(tomorrow)).isTrue()

        // Now check that we're removing old dates.
        now.onNext(tomorrow)
        assertThat(twilightProvider.isDateLoaded(testDate)).isFalse()
        assertThat(twilightProvider.isDateLoaded(tomorrow)).isTrue()

        // We don't have day 3, so it's not loaded.
        assertThat(twilightProvider.isDateLoaded(tomorrow.plusDays(1))).isFalse()
    }
}

fun TwilightProvider.isDateLoaded(date: LocalDate): Boolean {
    return twilightData.containsKey(date)
}
