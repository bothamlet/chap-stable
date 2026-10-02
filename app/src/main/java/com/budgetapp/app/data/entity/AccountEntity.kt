package com.budgetapp.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val balanceCents: Long = 0,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
