package com.budgetapp.app.data.model

data class CsvTransactionDraft(
    val postedAtMillis: Long,
    val description: String,
    val merchantName: String?,
    val amountCents: Long,
    val externalId: String,
)
