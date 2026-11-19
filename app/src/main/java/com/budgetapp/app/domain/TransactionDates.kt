package com.budgetapp.app.domain

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Preserve the old local calendar date/time when upgrading to zone-independent storage. */
fun Long.toCalendarTransactionMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDateTime().toInstant(ZoneOffset.UTC).toEpochMilli()
