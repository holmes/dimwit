package com.thejholmes.dimwit.twilight

import java.time.LocalDate
import java.time.LocalTime

class FakeTwilight {
    companion object {
        val twilightResults = TwilightResult(LocalDate.now(),
                LocalTime.of(6, 30),
                LocalTime.of(9, 0),
                LocalTime.of(12, 45),
                LocalTime.of(18, 30),
                LocalTime.of(20, 52)
        )

        val twilight = Twilight(twilightResults)
    }
}
