package com.budgetapp.app.ui.screens

import androidx.lifecycle.ViewModel
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.data.repository.BudgetRepository

class ProfileViewModel(
    private val repository: BudgetRepository,
) : ViewModel() {
    val settings = repository.observeProfileSettings()

    fun save(settings: ProfileSettings) {
        repository.saveProfileSettings(settings)
    }
}
