package com.budgetapp.app.data.model

data class EnvelopeBalance(
    val id: Long,
    val name: String,
    val ceilingCents: Long,
    val colorToken: String,
    val iconToken: String,
    val type: String,
    val sortOrder: Int,
    val spentCents: Long,
)
