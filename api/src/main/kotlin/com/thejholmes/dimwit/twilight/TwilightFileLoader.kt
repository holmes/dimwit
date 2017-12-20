package com.thejholmes.dimwit.twilight

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Parses from sunrise.com date. Looks like:
 * <code>
 * {
 *   "results": {
 *   "sunrise": "2017-02-13T14:59:12+00:00",
 *   "sunset": "2017-02-14T01:46:02+00:00",
 *   "solar_noon": "2017-02-13T20:22:37+00:00",
 *   "day_length": 38810,
 *   "civil_twilight_begin": "2017-02-13T14:32:21+00:00",
 *   "civil_twilight_end": "2017-02-14T02:12:53+00:00",
 *   "nautical_twilight_begin": "2017-02-13T14:01:31+00:00",
 *   "nautical_twilight_end": "2017-02-14T02:43:43+00:00",
 *   "astronomical_twilight_begin": "2017-02-13T13:30:59+00:00",
 *   "astronomical_twilight_end": "2017-02-14T03:14:14+00:00"
 *   },
 *   "status": "OK"
 * }
 * </code>
 */
class TwilightProvider(private val gson: Gson, private val nowProvider: () -> LocalDate,
        private val dataLocation: File) {
    private val logger = LoggerFactory.getLogger(TwilightProvider::class.java)
    private val twilightData = HashMap<LocalDate, TwilightResult>()

    companion object Factory {
        private val DEFAULT: TwilightResult = TwilightResult(LocalDate.now(),
                LocalTime.of(6, 30),
                LocalTime.of(7, 0),
                LocalTime.of(12, 30),
                LocalTime.of(18, 30),
                LocalTime.of(19, 0)
        )
    }

    fun refresh() {
        val now = nowProvider()

        // Trim old stuff first.
        twilightData
                .filterKeys { it.isBefore(now) }
                .forEach { date, _ -> twilightData.remove(date) }

        logger.info("Loading twilight data from $dataLocation")
        loadData(now).apply { twilightData.put(date, this) }
        loadData(now.plusDays(1)).apply { twilightData.put(date, this) }
    }

    fun twilight(): TwilightResult {
        val now = nowProvider()
        return twilightData.getOrElse(now) {
            logger.error("No twilight data for $now. Did we really load it?")
            DEFAULT
        }
    }

    internal fun loadData(date: LocalDate): TwilightResult {
        return try {
            val inputStream = getFileStream(date)
            val sunriseData = gson.fromJson(inputStream.bufferedReader(),
                    SunriseSunsetProvider::class.java)

            TwilightResult(date,
                    sunriseData.results.civil_twilight_begin.toLocalTime(),
                    sunriseData.results.sunrise.toLocalTime(),
                    sunriseData.results.solar_noon.toLocalTime(),
                    sunriseData.results.sunset.toLocalTime(),
                    sunriseData.results.civil_twilight_end.toLocalTime()
            )
        } catch (e: RuntimeException) {
            logger.error("Unable to load data for $date", e)
            DEFAULT
        }
    }

    private fun getFileStream(date: LocalDate): InputStream {
        val yearValue = date.year
        val monthValue = date.monthValue
        val dayValue = date.dayOfMonth

        val fileName = "$yearValue-$monthValue-$dayValue.json"
        val actualFile = File(dataLocation, fileName)

        return actualFile.inputStream()
    }
}

fun String.toLocalTime(): LocalTime {
    return ZonedDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
            .withZoneSameInstant(ZoneId.of("America/Los_Angeles"))
            .toLocalTime()

}

@Suppress("PropertyName")
private class SunriseSunsetProvider {
    class Results {
        lateinit var civil_twilight_begin: String
        lateinit var sunrise: String
        lateinit var solar_noon: String
        lateinit var sunset: String
        lateinit var civil_twilight_end: String
    }

    lateinit var results: Results
}
