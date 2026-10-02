package com.budgetapp.app.domain

import com.budgetapp.app.data.model.ProfileSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetPeriodTest {
    private val monthStart = ProfileSettings()
    private val fifteenth = ProfileSettings(monthStartDay = "15th of the month")
    private val zone = ZoneId.of("Europe/Vilnius")

    @Test
    fun monthStartPeriodCoversTheWholeCalendarMonth() {
        val period = monthStart.budgetPeriod(LocalDate.of(2026, 10, 3))

        assertEquals(LocalDate.of(2026, 10, 1), period.start)
        assertEquals(LocalDate.of(2026, 10, 31), period.end)
        assertEquals(31, period.lengthInDays)
    }

    @Test
    fun fifteenthStartStraddlesTwoMonths() {
        val beforePayday = fifteenth.budgetPeriod(LocalDate.of(2026, 10, 3))
        assertEquals(LocalDate.of(2026, 9, 15), beforePayday.start)
        assertEquals(LocalDate.of(2026, 10, 14), beforePayday.end)
        assertEquals(30, beforePayday.lengthInDays)

        val afterPayday = fifteenth.budgetPeriod(LocalDate.of(2026, 10, 20))
        assertEquals(LocalDate.of(2026, 10, 15), afterPayday.start)
        assertEquals(LocalDate.of(2026, 11, 14), afterPayday.end)
    }

    @Test
    fun periodWindowIsHalfOpenAcrossADstChange() {
        // Lithuania leaves DST on 2026-10-25, so October is 31 days but 745 hours long.
        val period = monthStart.budgetPeriod(LocalDate.of(2026, 10, 3))

        assertEquals(
            LocalDate.of(2026, 10, 1),
            Instant.ofEpochMilli(period.startMillis(zone)).atZone(zone).toLocalDate(),
        )
        assertEquals(
            LocalDate.of(2026, 11, 1),
            Instant.ofEpochMilli(period.endExclusiveMillis(zone)).atZone(zone).toLocalDate(),
        )
    }

    @Test
    fun dayLineCountsInclusiveDaysAndClamps() {
        val period = monthStart.budgetPeriod(LocalDate.of(2026, 10, 3))

        assertEquals("Day 3 of 31", monthStart.dayLine(period, LocalDate.of(2026, 10, 3)))
        assertEquals("Day 1 of 31", monthStart.dayLine(period, LocalDate.of(2026, 9, 20)))
        assertEquals("Day 31 of 31", monthStart.dayLine(period, LocalDate.of(2026, 12, 5)))
    }

    @Test
    fun periodLabelNamesWholeMonthsAndRanges() {
        assertEquals("October 2026", monthStart.periodLabel(monthStart.budgetPeriod(LocalDate.of(2026, 10, 3))))
        assertEquals("Sep 15 - Oct 14", fifteenth.periodLabel(fifteenth.budgetPeriod(LocalDate.of(2026, 10, 3))))
    }
}
