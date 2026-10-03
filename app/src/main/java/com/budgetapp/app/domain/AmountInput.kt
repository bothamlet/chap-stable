package com.budgetapp.app.domain

/**
 * Parses an amount a user typed into minor units, or null when the text is not a plain amount.
 *
 * Accepted: "1234", "1234,56", "1234.56", "0,5", "1234,5". The comma and the dot are both decimal
 * separators (European typing is the default here), and thousands separators are not written — so
 * "1.234,56" and "1,234" are rejected rather than guessed at. Rejecting is deliberate: the previous
 * parser read "1,234" as 1.234 (= €1.23) and turned "1e3" into €1,000 without saying anything.
 */
fun parseAmountMinorUnits(input: String): Long? {
    val text = input.trim()
    val match = AmountPattern.matchEntire(text) ?: return null
    val whole = match.groupValues[1].ifEmpty { "0" }.toLong()
    val fraction = match.groupValues[3].padEnd(2, '0')
    val cents = fraction.toLongOrNull() ?: 0L
    val total = whole * 100 + cents
    return total.takeIf { it <= MaxAmountCents }
}

/**
 * Upper bound a typed amount has to respect: 999,999,999.99. Values above this are rejected instead
 * of silently saturating Int, which is how "999999999999" used to become €21,474,836.47.
 */
const val MaxAmountCents: Long = 99_999_999_999L

private val AmountPattern = Regex("^([0-9]{0,9})(?:([.,])([0-9]{0,2}))?$")
