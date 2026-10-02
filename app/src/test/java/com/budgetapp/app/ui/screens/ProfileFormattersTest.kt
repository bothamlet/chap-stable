package com.budgetapp.app.ui.screens

import com.budgetapp.app.data.model.ProfileSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileFormattersTest {
    private val euros = ProfileSettings()

    @Test
    fun formatMoneyKeepsCents() {
        assertEquals("€42.18", euros.formatMoney(4218))
        assertEquals("€112.40", euros.formatMoney(11240))
        assertEquals("€0.05", euros.formatMoney(5))
    }

    @Test
    fun formatMoneyDropsDecimalsOnWholeAmounts() {
        assertEquals("€743", euros.formatMoney(74300))
        assertEquals("€1,110", euros.formatMoney(111000))
        assertEquals("€0", euros.formatMoney(0))
    }

    @Test
    fun formatMoneyKeepsSignAndGrouping() {
        assertEquals("-€30.05", euros.formatMoney(-3005))
        assertEquals("-€1,234.56", euros.formatMoney(-123456))
        assertEquals("€1,234.56", euros.formatMoney(123456))
    }

    @Test
    fun remainingLabelSwitchesToOverWhenOverspent() {
        assertEquals("€30 over", euros.remainingLabel(-3000))
        assertEquals("€30.05 over", euros.remainingLabel(-3005))
        assertEquals("€214 left", euros.remainingLabel(21400))
        assertEquals("€0 left", euros.remainingLabel(0))
    }

    @Test
    fun formatMoneyPlacesCurrencyPerLocaleConvention() {
        assertEquals("$", ProfileSettings(currency = "USD - US Dollar").currencySymbol())
        assertEquals("1,234.56 zł", ProfileSettings(currency = "PLN - Polish Złoty").formatMoney(123456))
        assertEquals("£42.18", ProfileSettings(currency = "GBP - Pound Sterling").formatMoney(4218))
    }
}
