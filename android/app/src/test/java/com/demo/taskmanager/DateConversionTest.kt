package com.demo.taskmanager

import com.demo.taskmanager.ui.isSelectableDay
import com.demo.taskmanager.ui.isoDateToUtcMillis
import com.demo.taskmanager.ui.utcMillisToIsoDate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateConversionTest {
    @Test
    fun utcMidnightKeepsItsCalendarDay() {
        assertEquals("2026-11-02", utcMillisToIsoDate(1_793_577_600_000L))
    }

    @Test
    fun roundTripKeepsTheDay() {
        assertEquals("2026-12-31", utcMillisToIsoDate(isoDateToUtcMillis("2026-12-31")))
    }

    @Test
    fun pastDaysAreNotSelectable() {
        val today = LocalDate.parse("2026-11-02")
        assertFalse(isSelectableDay(isoDateToUtcMillis("2026-11-01"), today))
        assertTrue(isSelectableDay(isoDateToUtcMillis("2026-11-02"), today))
        assertTrue(isSelectableDay(isoDateToUtcMillis("2026-11-03"), today))
    }
}
