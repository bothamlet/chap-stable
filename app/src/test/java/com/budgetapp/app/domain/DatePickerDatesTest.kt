package com.budgetapp.app.domain

import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class DatePickerDatesTest {
    @Test
    fun migrationPreservesTheLegacyCalendarDateAndTime() {
        val date = java.time.LocalDateTime.of(2026, 3, 1, 12, 30)
        val homeZone = java.time.ZoneId.of("Europe/Minsk")
        val oldMillis = date.atZone(homeZone).toInstant().toEpochMilli()
        assertEquals(date.toInstant(ZoneOffset.UTC).toEpochMilli(), oldMillis.toCalendarTransactionMillis(homeZone))
    }

    @Test
    fun pickerDatesUseUtcAndRoundTripAcrossLeapDays() {
        val date = LocalDate.of(2028, 2, 29)
        assertEquals(date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), date.toDatePickerMillis())
        assertEquals(date, date.toDatePickerMillis().toDatePickerDate())
    }
}
