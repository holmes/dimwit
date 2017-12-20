package com.thejholmes.dimwit

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.thejholmes.dimwit.ParsedLightZone.ParsedTimeFrame
import io.reactivex.Observable
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
    fun twilightParser() {
        assertThat(twilight.parse("15:30")).isEqualTo(LocalTime.of(15, 30))

        assertThat(twilight.parse("twilightBegin:-30")).isEqualTo(LocalTime.of(6, 0))
        assertThat(twilight.parse("sunrise:+30")).isEqualTo(LocalTime.of(7, 30))
        assertThat(twilight.parse("solarNoon:-15")).isEqualTo(LocalTime.of(12, 15))
        assertThat(twilight.parse("sunset:+30")).isEqualTo(LocalTime.of(19, 0))
        assertThat(twilight.parse("twilightEnd:+67")).isEqualTo(LocalTime.of(20, 7))
    }

    @Test
    fun parseBackAndForth() {
        val zoneJson = gson.toJson(zone)
        val convertedZone = parser.parse(zoneJson)

        assertThat(convertedZone.deviceId).isEqualTo("AAA")
        assertThat(convertedZone.timeFrames).hasSize(4)

        // Being lazy and just testing the first one.
        val timeFrame = convertedZone.timeFrames.first()
        assertThat(timeFrame.endTimeValue).isEqualTo("sunrise:-30")
        assertThat(timeFrame.lowLevel).isEqualTo(12)
        assertThat(timeFrame.highLevel).isEqualTo(32)
    }

    @Test
    fun convertToZoneWithTimes() {
        val lightZone = zone.lightZone(Observable.just(twilight), Observable.just(LocalTime.now()))
        assertThat(lightZone.deviceId).isEqualTo("AAA")
        assertThat(lightZone.timeFrames).hasSize(4)

        val timeFrames = lightZone.timeFrames
        val firstFrame = timeFrames[0]
        assertThat(firstFrame.lowLevel).isEqualTo(12)
        assertThat(firstFrame.highLevel).isEqualTo(32)
        assertThat(firstFrame.endTimeValue).isEqualTo("sunrise:-30")
    }
}
