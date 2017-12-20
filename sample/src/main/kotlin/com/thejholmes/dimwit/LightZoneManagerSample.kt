package com.thejholmes.dimwit

import com.google.gson.Gson
import java.io.File

fun main(args: Array<String>) {
    val manager = LightZoneManager(Gson(), File("/work/sunrise-data/data"))
    val parser = manager.parser

    val inputString = manager::class.java.getResource("/zone.json").readText()
    println("Your input:\n $inputString")

    val lightZone = parser.parse(inputString)
    lightZone.lightLevels
            .subscribe { println("${it.now}: ${it.lowLevel}/${it.highLevel}") }

    manager.start()
    Thread.sleep(999999)
    manager.stop()
}
