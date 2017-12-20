package com.thejholmes.dimwit

import com.google.gson.Gson
import com.thejholmes.dimwit.twilight.FakeTwilight
import com.thejholmes.dimwit.twilight.Twilight
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import java.time.LocalTime

class DimwitSample {
    val twilight: BehaviorSubject<Twilight> = BehaviorSubject.create<Twilight>()
    val localTime: BehaviorSubject<LocalTime> = BehaviorSubject.create<LocalTime>()
    val lightZoneParser: LightZoneParser

    init {
        val gson = Gson()
        lightZoneParser = LightZoneParser(gson, twilight, localTime)
    }

    fun doSomething() {
        // Initialize w/ some values for twilight
        twilight.onNext(FakeTwilight.twilight)

        val inputString = javaClass.getResource("/zone.json").readText()
        println("Your input:\n $inputString")

        val lightZone = lightZoneParser.parse(inputString)
        lightZone
                .lightLevels.withLatestFrom(localTime,
                        BiFunction<LightLevels, LocalTime, Pair<LocalTime, LightLevels>> { levels, now -> Pair(now, levels) })
                .subscribe { println("${it.first}: ${it.second.lowLevel}/${it.second.highLevel}") }

        // Now increment the values and see what happens!
        Observable
                .range(0, LocalTime.MAX.toSecondOfDay() / 60)
                .map { LocalTime.of(it / 60, it % 60) }
                .subscribe(localTime)

        // Now update Twilight and make sure we see some changes
        val newTwilight = FakeTwilight.twilightResults
                .copy(twilightBegin = LocalTime.of(2, 12))
                .copy(sunrise = LocalTime.of(4, 52))
                .copy(solarNoon = LocalTime.of(9, 38))
                .copy(sunset = LocalTime.of(16, 15))
                .copy(sunset = LocalTime.of(23, 8))

        println("Should see another update:")
        twilight.onNext(Twilight(newTwilight))

        Thread.sleep(3000)
    }
}

fun main(args: Array<String>) {
    DimwitSample().doSomething()
}
