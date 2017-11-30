package com.thejholmes.dimwit

import java.time.LocalDate
import java.time.LocalTime

/**
 * TIME, low, high
 *
 * TIME
 *  twilightBegin
 *  sunrise
 *  sunset
 *  solarNoon
 *  twilightEnd
 *  min
 *  max
 */
//class LightZoneParser(val twilight: Twilight) {
//  fun parse(input: String): LightZone {
//    "twilightBegin".matches(Regex.fromLiteral("[a-zA-z]+[(0-9)+]*"))
//
//
//    val timeFrames = input
//        .split("\n")
//        .map { entry -> TimeFrame
//          val sections = entry.split(",")
//          val endPeriod = sections[0]
//
//          if (endPeriod.startsWith("twilightBegin")) {
//            val func = twilight::twilightBegin
//            func.invoke(0)
//          }
//
//          val end = { LocalTime.now() }
//          val low = sections[1].toInt()
//          val high = sections[2].toInt()
//          return@map TimeFrame(end, low, high)
//        }
//
//    return LightZone(timeFrames)
//  }
//}
