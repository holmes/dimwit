package com.thejholmes.dimwit

import com.google.gson.Gson
import io.reactivex.Observable
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
class TwilightProvider(private val gson: Gson, private val dataLocation: File,
        today: Observable<LocalDate>) {
    private val logger = LoggerFactory.getLogger(TwilightProvider::class.java)
    val twilightData = HashMap<LocalDate, Twilight>()
    val twilight: Observable<Twilight>

    init {
        twilight = today.map { date ->
            refresh(date)

            twilightData.getOrElse(date) {
                logger.error("No twilight data for $date. Did we really load it?")
                Twilight.DEFAULT
            }
        }
    }

    private fun refresh(now: LocalDate) {
        // Trim old stuff first.
        twilightData
                .filterKeys { it.isBefore(now) }
                .forEach { date, _ -> twilightData.remove(date) }

        // Load today and tomorrow's data just in case we somehow land on a date boundary.
        logger.info("Loading twilight data from $dataLocation")
        arrayOf(now, now.plusDays(1))
                .forEach { date ->
                    loadData(date).apply { twilightData.put(this.date, this) }
                }
    }

    private fun loadData(date: LocalDate): Twilight {
        return try {
            val inputStream = getFileStream(date)
            val sunriseData = gson.fromJson(inputStream.bufferedReader(),
                    SunriseSunsetProvider::class.java)

            Twilight(date,
                    sunriseData.results.civil_twilight_begin.toLocalTime(),
                    sunriseData.results.sunrise.toLocalTime(),
                    sunriseData.results.solar_noon.toLocalTime(),
                    sunriseData.results.sunset.toLocalTime(),
                    sunriseData.results.civil_twilight_end.toLocalTime()
            )
        } catch (e: RuntimeException) {
            logger.error("Unable to load data for $date", e)
            Twilight.DEFAULT
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
