package com.budgetapp.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categorization_rules",
    foreignKeys = [
        ForeignKey(
            entity = EnvelopeEntity::class,
            parentColumns = ["id"],
            childColumns = ["envelopeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["envelopeId"]),
        Index(value = ["pattern", "matchField"], unique = true),
    ],
)
data class CategorizationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val matchField: String,
    val envelopeId: Long,
    val priority: Int = 0,
    val isEnabled: Boolean = true,
    val createdAtMillis: Long,
)
