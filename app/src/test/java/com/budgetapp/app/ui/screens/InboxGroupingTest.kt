package com.budgetapp.app.ui.screens

import com.budgetapp.app.data.entity.TransactionEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class InboxGroupingTest {
    @Test
    fun expensesAndRefundsFromTheSameMerchantStaySeparate() {
        val groups = listOf(row(1, -1000), row(2, 500), row(3, -200)).toInboxGroups()
        assertEquals(2, groups.size)
        assertEquals(setOf(-1200L, 500L), groups.map { it.amountCents }.toSet())
        assertNotEquals(groups[0].key, groups[1].key)
        assertEquals(listOf(1L, 3L), groups.single { it.amountCents < 0 }.transactionIds)
    }

    private fun row(id: Long, cents: Long) = TransactionEntity(
        id = id, accountId = 1, envelopeId = null, importBatchId = null,
        postedAtMillis = id, description = "Coffee", merchantName = "COFFEE",
        amountCents = cents, status = "inbox", createdAtMillis = 0, updatedAtMillis = 0,
    )
}
