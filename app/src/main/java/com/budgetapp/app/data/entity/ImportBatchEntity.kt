package com.budgetapp.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val importedAtMillis: Long,
    val totalRows: Int,
    val acceptedRows: Int,
    val duplicateRows: Int,
)
