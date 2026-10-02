package com.budgetapp.app.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnvelopeIconSuggestionTest {
    @Test
    fun suggestsEverydayFoodIcons() {
        assertEquals("groceries", suggestEnvelopeIconToken("Weekly groceries"))
        assertEquals("junk_food", suggestEnvelopeIconToken("Friday junk food"))
        assertEquals("sweets", suggestEnvelopeIconToken("Chocolate and sweets"))
        assertEquals("ice_cream", suggestEnvelopeIconToken("Ice cream"))
    }

    @Test
    fun prioritizesSpecificPhrases() {
        assertEquals("junk_food", suggestEnvelopeIconToken("Fast food dining"))
        assertEquals("dry_cleaning", suggestEnvelopeIconToken("Dry cleaning"))
        assertEquals("rent", suggestEnvelopeIconToken("Apartment rent"))
        assertEquals("taxes", suggestEnvelopeIconToken("Mokesciai valstybei"))
    }

    @Test
    fun supportsLithuanianNamesAndDiacritics() {
        assertEquals("groceries", suggestEnvelopeIconToken("Maistas"))
        assertEquals("sweets", suggestEnvelopeIconToken("\u0160okoladas"))
        assertEquals("health", suggestEnvelopeIconToken("Sveikata"))
        assertEquals("savings", suggestEnvelopeIconToken("Santaupos"))
    }

    @Test
    fun leavesUnknownNamesWithoutSuggestion() {
        assertNull(suggestEnvelopeIconToken(""))
        assertNull(suggestEnvelopeIconToken("Something personal"))
    }
}
