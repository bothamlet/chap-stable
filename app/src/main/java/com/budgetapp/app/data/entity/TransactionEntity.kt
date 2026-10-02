package com.budgetapp.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["envelopeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ImportBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["importBatchId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["envelopeId"]),
        Index(value = ["importBatchId"]),
        Index(value = ["externalId"], unique = true),
        Index(value = ["postedAtMillis"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val envelopeId: Long?,
    val importBatchId: Long?,
    val postedAtMillis: Long,
    val description: String,
    val merchantName: String?,
    val amountCents: Long,
    val note: String? = null,
    val externalId: String? = null,
    val status: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
