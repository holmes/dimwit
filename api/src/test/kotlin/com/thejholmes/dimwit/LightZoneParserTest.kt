package com.thejholmes.dimwit

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.thejholmes.dimwit.ParsedLightZone.ParsedTimeFrame
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import junit.framework.TestCase.fail
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class LightZoneParserTest {
    lateinit var gson: Gson
    lateinit var twilight: Twilight
    lateinit var parser: LightZoneParser

    lateinit var zone: ParsedLightZone

    @Before
    fun setUp() {
        gson = Gson()
        twilight = Twilight.DEFAULT
        parser = LightZoneParser(gson, Observable.just(twilight), Observable.just(LocalTime.now()))

        val timeFrames = arrayOf(
                ParsedTimeFrame("5:00", 80, 100),
                ParsedTimeFrame("sunrise:-30", 12, 32),
                ParsedTimeFrame("solarNoon:0", 80, 100),
                ParsedTimeFrame("15:00", 80, 100),
                ParsedTimeFrame("sunset:30", 32, 64)
        )
        zone = ParsedLightZone("AAA", timeFrames)
    }

    @Test
    fun failureWithUnknownBaseShouldShowSomethingUseful() {
        val timeFrames = arrayOf(
                ParsedTimeFrame("blahblah:30", 32, 64)
        )
        zone = ParsedLightZone("AAA", timeFrames)
        val zoneJson = gson.toJson(zone)

        try {
            parser.parse(zoneJson)
            fail("Should have found an error while parsing")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("blahblah")
        }
    }

    @Test
    fun failureWithNoBaseShouldShowSomethingUseful() {
        val timeFrames = arrayOf(
                ParsedTimeFrame(":30", 32, 64)
        )
        zone = ParsedLightZone("AAA", timeFrames)
        val zoneJson = gson.toJson(zone)

        try {
            parser.parse(zoneJson)
            fail("Should have found an error while parsing")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Unknown base")
        }
    }

    @Test
    fun failureWithHourOutOfRangeShouldShowSomethingUseful() {
        val timeFrames = arrayOf(
                ParsedTimeFrame("78:30", 32, 64)
        )
        zone = ParsedLightZone("AAA", timeFrames)
        val zoneJson = gson.toJson(zone)

        try {
            parser.parse(zoneJson)
            fail("Should have found an error while parsing")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Invalid value for HourOfDay")
        }
    }

    @Test
    fun failureWithMinuteOutOfRangeShouldShowSomethingUseful() {
        val timeFrames = arrayOf(
                ParsedTimeFrame("6:78", 32, 64)
        )
        zone = ParsedLightZone("AAA", timeFrames)
        val zoneJson = gson.toJson(zone)

        try {
            parser.parse(zoneJson)
            fail("Should have found an error while parsing")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Invalid value for MinuteOfHour")
        }
    }

    @Test
    fun addLargeOffsetsToTimes() {
        val timeFrames = arrayOf(
                ParsedTimeFrame("sunrise:78", 32, 64)
        )
        zone = ParsedLightZone("AAA", timeFrames)
        zone.lightZone(Observable.just(twilight), Observable.just(LocalTime.now()))
        // Just don't crash.
    }

    @Test
    fun twilightParser() {
        assertThat(twilight.parse("7:00")).isEqualTo(LocalTime.of(7, 0))
        assertThat(twilight.parse("15:30")).isEqualTo(LocalTime.of(15, 30))

        assertThat(twilight.parse("twilightBegin:-30")).isEqualTo(LocalTime.of(6, 0))
        assertThat(twilight.parse("sunrise:+75")).isEqualTo(LocalTime.of(8, 15))
        assertThat(twilight.parse("solarNoon:-15")).isEqualTo(LocalTime.of(12, 15))
        assertThat(twilight.parse("sunset:+30")).isEqualTo(LocalTime.of(19, 0))
        assertThat(twilight.parse("twilightEnd:+67")).isEqualTo(LocalTime.of(20, 7))
    }

    @Test
    fun testFailedNursery() {
        val file = javaClass.getResource("/nursery.json").readText()
        val parsedLightZone = gson.fromJson(file, ParsedLightZone::class.java)

        val zone = parsedLightZone.lightZone(Observable.just(twilight), Observable.just(LocalTime.of(8, 15)))
        assertThat(zone.timeFrames.size).isEqualTo(6)

        val observer = TestObserver.create<LightLevels>()
        zone.lightLevels.subscribe(observer)

        assertThat(observer.valueCount()).isAtLeast(1)
        assertThat(observer.values().first().lowLevel).isEqualTo(19)
    }

    @Test
    fun parseBackAndForth() {
        val zoneJson = gson.toJson(zone)
        val convertedZone = parser.parse(zoneJson)

        assertThat(convertedZone.deviceId).isEqualTo("AAA")
        assertThat(convertedZone.timeFrames).hasSize(5)

        // Being lazy and just testing the first one.
        val timeFrame = convertedZone.timeFrames.first()
        assertThat(timeFrame.endTimeValue).isEqualTo("5:00")
        assertThat(timeFrame.lowLevel).isEqualTo(80)
        assertThat(timeFrame.highLevel).isEqualTo(100)
    }

    @Test
    fun convertToZoneWithTimes() {
        val lightZone = zone.lightZone(Observable.just(twilight), Observable.just(LocalTime.now()))
        assertThat(lightZone.deviceId).isEqualTo("AAA")
        assertThat(lightZone.timeFrames).hasSize(5)

        val timeFrames = lightZone.timeFrames
        val firstFrame = timeFrames[1]
        assertThat(firstFrame.lowLevel).isEqualTo(12)
        assertThat(firstFrame.highLevel).isEqualTo(32)
        assertThat(firstFrame.endTimeValue).isEqualTo("sunrise:-30")
    }
}
