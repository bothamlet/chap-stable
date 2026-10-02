package com.budgetapp.app.data.model

data class ProfileSettings(
    val name: String = "Chap",
    val avatarUri: String? = null,
    val currency: String = "EUR - Euro",
    val dateFormat: String = "DD/MM/YYYY",
    val firstDayOfWeek: String = "Monday",
    val monthStartDay: String = "1st of the month",
)
