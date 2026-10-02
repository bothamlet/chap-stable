package com.budgetapp.app.domain

import com.budgetapp.app.data.model.ProfileSettings
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * The budget window a user is currently looking at. Everything the UI calls "spent", "left",
 * "remaining" or "forecast" is scoped to this window — with the exception of goal envelopes,
 * which accumulate savings across periods.
 */
data class BudgetPeriod(
    val start: LocalDate,
    val end: LocalDate,
) {
    val lengthInDays: Int = (end.toEpochDay() - start.toEpochDay() + 1).toInt().coerceAtLeast(1)

    /** First instant of the period, inclusive, in the device time zone. */
    fun startMillis(zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long =
        start.atStartOfDay(zone).toInstant().toEpochMilli()

    /** First instant after the period, exclusive, in the device time zone. */
    fun endExclusiveMillis(zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long =
        end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
}

fun ProfileSettings.budgetPeriod(today: LocalDate = LocalDate.now()): BudgetPeriod {
    val startDay = when (monthStartDay) {
        "15th of the month" -> 15
        else -> 1
    }
    val currentMonth = YearMonth.from(today)
    val currentStart = LocalDate.of(currentMonth.year, currentMonth.month, startDay.coerceAtMost(currentMonth.lengthOfMonth()))
    val start = if (today >= currentStart) {
        currentStart
    } else {
        val previousMonth = currentMonth.minusMonths(1)
        LocalDate.of(previousMonth.year, previousMonth.month, startDay.coerceAtMost(previousMonth.lengthOfMonth()))
    }
    val nextMonth = YearMonth.from(start).plusMonths(1)
    val nextStart = LocalDate.of(nextMonth.year, nextMonth.month, startDay.coerceAtMost(nextMonth.lengthOfMonth()))
    return BudgetPeriod(start = start, end = nextStart.minusDays(1))
}

fun ProfileSettings.periodLabel(period: BudgetPeriod): String {
    if (period.start.dayOfMonth == 1 && period.end == period.start.withDayOfMonth(period.start.lengthOfMonth())) {
        val month = period.start.month.getDisplayName(TextStyle.FULL, Locale.US)
        return "$month ${period.start.year}"
    }
    val start = "${period.start.month.getDisplayName(TextStyle.SHORT, Locale.US)} ${period.start.dayOfMonth}"
    val end = "${period.end.month.getDisplayName(TextStyle.SHORT, Locale.US)} ${period.end.dayOfMonth}"
    return if (period.start.year == period.end.year) {
        "$start - $end"
    } else {
        "$start ${period.start.year} - $end ${period.end.year}"
    }
}

fun ProfileSettings.dayLine(period: BudgetPeriod, today: LocalDate = LocalDate.now()): String {
    val day = (today.toEpochDay() - period.start.toEpochDay() + 1).toInt().coerceIn(1, period.lengthInDays)
    return "Day $day of ${period.lengthInDays}"
}
