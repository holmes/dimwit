package com.thejholmes.dimwit

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.MINUTES

class LightZoneManager(twilightProvider: TwilightProvider) {
    private val logger = LoggerFactory.getLogger(LightZoneManager::class.java)

    val now: Observable<LocalTime>
    val today: Observable<LocalDate>
    val twilight: Observable<Twilight>

    var subscription = Disposables.empty()

    init {
        now = Observable
                .interval(1, MINUTES)
                .map { LocalTime.now() }
                .doOnNext { logger.debug("Time is now: {}", it) }

        today = Observable
                .interval(1, DAYS)
                .map { LocalDate.now() }
                .distinctUntilChanged()
                .doOnNext { logger.debug("Today is now: {}", it) }

        twilight = twilightProvider.twilight
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

private fun Disposable.addTo(disposable: Disposable) {
    this.addTo(disposable)
}
