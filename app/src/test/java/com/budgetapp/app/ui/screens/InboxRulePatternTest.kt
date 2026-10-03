package com.budgetapp.app.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class InboxRulePatternTest {
    @Test
    fun punctuationIsStrippedFromTheDerivedPattern() {
        // "Coffee, large" used to produce "Coffee," which then only matched rows containing a comma.
        assertEquals("Coffee", "Coffee, large".toInboxRulePattern())
        assertEquals("Book", "\"Book\" Special".toInboxRulePattern())
        assertEquals("Cafe", "(Cafe) Riga".toInboxRulePattern())
        assertEquals("UAB", "UAB - Mokejimas".toInboxRulePattern())
    }

    @Test
    fun plainAndUnicodeMerchantsKeepTheirFirstWord() {
        assertEquals("MAXIMA", "MAXIMA XXX".toInboxRulePattern())
        assertEquals("Mokėjimas", "Mokėjimas kortele".toInboxRulePattern())
        assertEquals("1234", "1234 5678".toInboxRulePattern())
    }

    @Test
    fun aTitleWithoutWordCharactersFallsBackToItsCleanText() {
        assertEquals("", "!!!".toInboxRulePattern())
        assertEquals("Cafe", "  Cafe  ".toInboxRulePattern())
    }
}
