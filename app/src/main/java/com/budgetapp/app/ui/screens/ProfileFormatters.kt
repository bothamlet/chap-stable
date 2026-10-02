package com.budgetapp.app.ui.screens

import com.budgetapp.app.data.model.ProfileSettings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

fun ProfileSettings.currencySymbol(): String =
    when {
        currency.startsWith("USD") -> "$"
        currency.startsWith("GBP") -> "£"
        currency.startsWith("PLN") -> "zł"
        else -> "€"
    }

fun ProfileSettings.formatMoney(cents: Long): String {
    val sign = if (cents < 0L) "-" else ""
    val absolute = abs(cents)
    val whole = "%,d".format(Locale.US, absolute / 100)
    val remainder = absolute % 100
    val fraction = if (remainder == 0L) "" else ".%02d".format(Locale.US, remainder)
    val symbol = currencySymbol()
    // Show cents only when the amount has a fractional part.
    return if (symbol == "zł") {
        "$sign$whole$fraction $symbol"
    } else {
        "$sign$symbol$whole$fraction"
    }
}

fun ProfileSettings.remainingLabel(remainingCents: Long): String =
    if (remainingCents < 0L) {
        "${formatMoney(-remainingCents)} over"
    } else {
        "${formatMoney(remainingCents)} left"
    }

fun ProfileSettings.formatDate(date: LocalDate): String =
    date.format(
        DateTimeFormatter.ofPattern(
            when (dateFormat) {
                "MM/DD/YYYY" -> "MM/dd/yyyy"
                "YYYY-MM-DD" -> "yyyy-MM-dd"
                else -> "dd/MM/yyyy"
            },
            Locale.US,
        ),
    )

fun ProfileSettings.formatTransactionDate(date: LocalDate): String =
    "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)}, ${formatDate(date)}"

fun ProfileSettings.firstDay(): DayOfWeek =
    if (firstDayOfWeek == "Sunday") DayOfWeek.SUNDAY else DayOfWeek.MONDAY
