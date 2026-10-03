package com.budgetapp.app.ui.screens

import com.budgetapp.app.domain.parseAmountMinorUnits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The three amount entry points (cash sheet, envelope ceiling, transaction editor) share
 * parseAmountMinorUnits; these cases pin the behaviour a user typing a European amount expects.
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
        assertEquals(123400, "1234".toCashCentsOrNull())
    }

    @Test
    fun invalidAmountsAreRejected() {
        assertNull("".toCashCentsOrNull())
        assertNull("abc".toCashCentsOrNull())
        assertNull("0".toCashCentsOrNull())
        assertNull("-5".toCashCentsOrNull())
        assertNull("5,555".toCashCentsOrNull(), "more than two decimals")
        assertNull("1,234.56".toCashCentsOrNull(), "thousands separators are not written")
    }

    @Test
    fun subCentAmountsAreRejectedInsteadOfBecomingZero() {
        assertNull("0.001".toCashCentsOrNull())
        assertNull("0,004".toCashCentsOrNull())
    }

    @Test
    fun absurdAmountsAreRejectedInsteadOfOverflowing() {
        assertNull("999999999999".toCashCentsOrNull())
        assertNull("1000000000".toCashCentsOrNull(), "a billion euros is over the cap")
        assertEquals(99_999_999_999L, "999999999.99".toCashCentsOrNull())
    }

    @Test
    fun scientificNotationIsNotSilentlyAccepted() {
        assertNull("1e3".toCashCentsOrNull())
        assertNull("1E3".toCashCentsOrNull())
    }

    @Test
    fun envelopeCeilingUsesTheSameRules() {
        assertEquals(25050, "250.50".toCentsOrNull())
        assertEquals(25050, "250,50".toCentsOrNull())
        assertNull("".toCentsOrNull())
        assertNull("1,200".toCentsOrNull(), "thousands separators are not written")
        assertNull("0".toCentsOrNull(), "a zero ceiling is not a budget")
    }

    @Test
    fun transactionEditorUsesTheSameRules() {
        assertEquals(500, "5".toDetailCentsOrNull())
        assertEquals(105, "1,05".toDetailCentsOrNull())
        assertNull("0".toDetailCentsOrNull())
        assertNull("5,555".toDetailCentsOrNull())
    }

    @Test
    fun parserItselfDistinguishesUnparseableFromZero() {
        assertNull(parseAmountMinorUnits("nope"))
        assertEquals(0, parseAmountMinorUnits("0"))
        assertNull(parseAmountMinorUnits("0,001"), "more than two decimals is a parse error")
    }
}
