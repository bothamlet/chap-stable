package com.budgetapp.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.budgetapp.app.data.model.EnvelopeType

@Entity(
    tableName = "envelopes",
    indices = [Index(value = ["name"], unique = true)],
)
data class EnvelopeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ceilingCents: Long,
    val colorToken: String,
    val iconToken: String = "wallet",
    val type: String = EnvelopeType.Variable,
    val sortOrder: Int,
    val isArchived: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
