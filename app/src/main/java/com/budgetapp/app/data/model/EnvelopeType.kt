package com.budgetapp.app.data.model

object EnvelopeType {
    const val Variable = "variable"
    const val Fixed = "fixed"
    const val Goal = "goal"

    val SpendingTypes = setOf(Variable, Fixed)
}

fun String.toEnvelopeType(): String =
    when (lowercase()) {
        EnvelopeType.Fixed -> EnvelopeType.Fixed
        EnvelopeType.Goal -> EnvelopeType.Goal
        else -> EnvelopeType.Variable
    }
