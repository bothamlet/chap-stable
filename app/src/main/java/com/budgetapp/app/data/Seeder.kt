package com.budgetapp.app.data

import com.budgetapp.app.data.repository.BudgetRepository
import com.budgetapp.app.data.model.EnvelopeType
import kotlinx.coroutines.flow.first

class Seeder(
    private val repository: BudgetRepository,
) {
    suspend fun seedIfEmpty() {
        if (repositorySnapshotHasAccounts()) return

        repository.createAccount(
            name = "Cash",
            type = "MANUAL",
        )
        repository.createEnvelope("Groceries", 400_00, "MockPrimaryBright", EnvelopeType.Variable)
        repository.createEnvelope("Transport", 160_00, "MockWarningGold", EnvelopeType.Variable)
        repository.createEnvelope("Bills", 900_00, "MockPrimaryLight", EnvelopeType.Fixed)
    }

    private suspend fun repositorySnapshotHasAccounts(): Boolean {
        return repository.observeAccounts().first().isNotEmpty()
    }
}
