package com.example.motionpulse

import com.example.motionpulse.ui.viewmodels.StatsViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StatsWeekNavigationTest {

    private val fixedToday = LocalDate.of(2026, 11, 4) // Wednesday Nov 4, 2026

    @Test
    fun weekRange_offsetZero_returnsCurrentWeekMondayToSunday() {
        val range = StatsViewModel.weekRange(0, fixedToday)
        assertEquals(LocalDate.of(2026, 11, 2), range.start) // Monday Nov 2
        assertEquals(LocalDate.of(2026, 11, 8), range.endInclusive) // Sunday Nov 8
        assertEquals(DayOfWeek.MONDAY, range.start.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, range.endInclusive.dayOfWeek)
    }

    @Test
    fun weekRange_offsetMinusOne_returnsPreviousWeekMondayToSunday() {
        val range = StatsViewModel.weekRange(-1, fixedToday)
        assertEquals(LocalDate.of(2026, 10, 26), range.start) // Monday Oct 26
        assertEquals(LocalDate.of(2026, 11, 1), range.endInclusive) // Sunday Nov 1
        assertEquals(DayOfWeek.MONDAY, range.start.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, range.endInclusive.dayOfWeek)
    }

    @Test
    fun weekRange_offsetPlusOne_returnsNextWeekMondayToSunday() {
        val range = StatsViewModel.weekRange(1, fixedToday)
        assertEquals(LocalDate.of(2026, 11, 9), range.start) // Monday Nov 9
        assertEquals(LocalDate.of(2026, 11, 15), range.endInclusive) // Sunday Nov 15
        assertEquals(DayOfWeek.MONDAY, range.start.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, range.endInclusive.dayOfWeek)
    }

    @Test
    fun weekRange_spansMonthBoundary_correctStartAndEnd() {
        val monthBoundaryToday = LocalDate.of(2026, 10, 30) // Friday Oct 30, 2026
        val range = StatsViewModel.weekRange(0, monthBoundaryToday)
        assertEquals(LocalDate.of(2026, 10, 26), range.start) // Monday Oct 26
        assertEquals(LocalDate.of(2026, 11, 1), range.endInclusive) // Sunday Nov 1
    }

    @Test
    fun weekRange_spansYearBoundary_correctStartAndEnd() {
        val yearEndToday = LocalDate.of(2026, 12, 31) // Thursday Dec 31, 2026
        val range = StatsViewModel.weekRange(0, yearEndToday)
        assertEquals(LocalDate.of(2026, 12, 28), range.start) // Monday Dec 28, 2026
        assertEquals(LocalDate.of(2027, 1, 3), range.endInclusive) // Sunday Jan 3, 2027
    }
}
