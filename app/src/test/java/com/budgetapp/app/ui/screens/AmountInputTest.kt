package com.budgetapp.app.ui.screens

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Characterization tests for the three amount entry parsers, written to the behaviour a user
 * expects rather than the behaviour that exists. Every @Ignore here is a known defect: remove the
 * annotation when the parser is fixed.
 */
class AmountInputTest {
    @Test
    fun plainAndDecimalCommaAmountsParse() {
        assertEquals(1234, "12.34".toCashCentsOrNull())
        assertEquals(1234, "12,34".toCashCentsOrNull())
        assertEquals(50, ",5".toCashCentsOrNull())
        assertEquals(500, "5.".toCashCentsOrNull())
        assertEquals(50, "0.50".toCashCentsOrNull())
        assertEquals(1000, "10".toCashCentsOrNull())
        assertEquals(4200, "  42  ".toCashCentsOrNull())
    }

    @Test
    fun invalidAmountsAreRejected() {
        assertNull("".toCashCentsOrNull())
        assertNull("abc".toCashCentsOrNull())
        assertNull("0".toCashCentsOrNull())
        assertNull("-5".toCashCentsOrNull())
    }

    @Ignore
    @Test
    fun thousandsSeparatorsDoNotShrinkTheAmount() {
        // Documents a defect: "1,234" is read as 1.234 -> 123 cents instead of 123400.
        assertEquals(123400, "1,234".toCashCentsOrNull())
        assertEquals(123456, "1,234.56".toCashCentsOrNull())
        assertEquals(123456, "1 234,56".toCashCentsOrNull())
    }

    @Ignore
    @Test
    fun subCentAmountsAreRejectedInsteadOfBecomingZero() {
        // Documents a defect: "0.001" passes validation (> 0) and rounds to 0 cents, creating a
        // €0.00 transaction.
        assertNull("0.001".toCashCentsOrNull())
    }

    @Ignore
    @Test
    fun absurdAmountsAreRejectedInsteadOfOverflowing() {
        // Documents a defect: Double * 100 then Int roundToInt saturates at Int.MAX_VALUE, so
        // "999999999999" silently becomes €21,474,836.47.
        assertNull("999999999999".toCashCentsOrNull())
    }

    @Ignore
    @Test
    fun scientificNotationIsNotSilentlyAccepted() {
        // Documents a defect: "1e3" parses as 1000 and becomes a €1,000 transaction.
        assertNull("1e3".toCashCentsOrNull())
    }

    @Test
    fun envelopeCeilingUsesTheSameRules() {
        assertEquals(25050, "250.50".toCentsOrNull())
        assertEquals(25050, "250,50".toCentsOrNull())
        assertNull("".toCentsOrNull())
    }

    @Ignore
    @Test
    fun envelopeCeilingAlsoRejectsThousandsSeparators() {
        // Documents the same defect in the envelope editor: "1,200" becomes €1.20.
        assertEquals(120000, "1,200".toCentsOrNull())
    }
}
