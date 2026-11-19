package com.budgetapp.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Material date pickers represent calendar dates at midnight UTC, rather than in the
// device time zone. Local-midnight conversions can select the preceding date.
fun LocalDate.toDatePickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.toDatePickerDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
