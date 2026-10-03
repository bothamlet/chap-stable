package com.budgetapp.app.data.model

data class ImportResult(
    val importBatchId: Long,
    val totalRows: Int,
    val acceptedRows: Int,
    val duplicateRows: Int,
    val incomeRows: Int = 0,
)
