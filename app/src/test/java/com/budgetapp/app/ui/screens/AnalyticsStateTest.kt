package com.budgetapp.app.ui.screens

import com.budgetapp.app.data.entity.TransactionEntity
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.data.model.EnvelopeType
import com.budgetapp.app.data.model.ProfileSettings
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsStateTest {
    private val groceries = EnvelopeBalance(
        id = 1,
        name = "Groceries",
        ceilingCents = 40000,
        colorToken = "variable",
        iconToken = "wallet",
        type = EnvelopeType.Variable,
        sortOrder = 0,
        spentCents = 5000,
    )

    @Test
    fun mostActiveDayIgnoresSpendingFromEarlierPeriods() {
        val today = LocalDate.now()
        // 45 days back always lands in an earlier period and on a different weekday. Five expenses
        // there beat today's single one, so all-time grouping would name that day instead.
        val state = AnalyticsState(
            envelopes = listOf(groceries),
            transactions = listOf(expense(id = 1, daysAgo = 0, cents = 1200)) +
                (2L..6L).map { expense(id = it, daysAgo = 45, cents = 100) },
            profileSettings = ProfileSettings(),
        )

        assertEquals(1, state.periodExpenses.size, "only this period's expenses count")
        assertEquals(1200, state.spentCents)
        assertEquals(
            today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US),
            state.mostActiveDay,
        )
    }

    private fun expense(id: Long, daysAgo: Long, cents: Long): TransactionEntity {
        val day = LocalDate.now().minusDays(daysAgo)
        return TransactionEntity(
            id = id,
            accountId = 1,
            envelopeId = groceries.id,
            importBatchId = null,
            postedAtMillis = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            description = "Test",
            merchantName = "Test",
            amountCents = -cents,
            status = "categorized",
            createdAtMillis = 0,
            updatedAtMillis = 0,
        )
    }
}
