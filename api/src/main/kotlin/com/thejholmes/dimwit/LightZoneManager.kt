package com.thejholmes.dimwit

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.MINUTES

class LightZoneManager(gson: Gson, dataLocation: File) {
    private val now: Observable<LocalTime>
    private val today: Observable<LocalDate>
    private var subscription = CompositeDisposable()

    val parser: LightZoneParser

    init {
        now = Observable
                .interval(0, 1, MINUTES)
                .map { LocalTime.now() }
                .distinctUntilChanged()

        today = Observable
                .interval(0, 1, DAYS)
                .map { LocalDate.now() }
                .distinctUntilChanged()

        val twilightProvider = TwilightProvider(gson, dataLocation, today)
        val twilight = twilightProvider.twilight

        parser = LightZoneParser(gson, twilight, now)
    }

    fun start() {
        subscription.dispose()
        subscription = CompositeDisposable()

        now.subscribe().addTo(subscription)
        today.subscribe().addTo(subscription)
    }

    fun stop() {
        subscription.dispose()
    }
}

private fun Disposable.addTo(disposable: CompositeDisposable) {
    disposable.add(this)
}
