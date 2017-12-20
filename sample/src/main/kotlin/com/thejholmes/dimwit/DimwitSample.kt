package com.thejholmes.dimwit

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class DimwitSample {
    val now: BehaviorSubject<LocalTime>
    val today: BehaviorSubject<LocalDate>
    val lightZoneParser: LightZoneParser
    val twilightProvider: TwilightProvider

    init {
        now = BehaviorSubject.create()
        today = BehaviorSubject.create()

        val gson = Gson()
        twilightProvider = TwilightProvider(gson, File("/work/sunrise-data/data"), today)
        lightZoneParser = LightZoneParser(gson, twilightProvider.twilight, now)
    }

    fun doSomething() {
        val inputString = javaClass.getResource("/zone.json").readText()
        println("Your input:\n $inputString")

        val lightZone = lightZoneParser.parse(inputString)
        lightZone.lightLevels
                .subscribe { println("${it.now}: ${it.lowLevel}/${it.highLevel}") }

        // Now increment the values and see what happens!
        today.onNext(LocalDate.now())
        Observable
                .range(0, LocalTime.MAX.toSecondOfDay() / 60)
                .map { LocalTime.of(it / 60, it % 60) }
                .subscribe(now)

        // Now update Twilight and make sure we see some changes.
        println("Should see another update:")
        today.onNext(LocalDate.now().plusDays(1))

        Thread.sleep(3000)
    }
}

fun main(args: Array<String>) {
    DimwitSample().doSomething()
}
