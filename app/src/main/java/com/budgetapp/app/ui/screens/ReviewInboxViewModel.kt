package com.budgetapp.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.app.data.entity.TransactionEntity
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.data.model.EnvelopeType
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.data.repository.BudgetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class ReviewInboxState(
    val envelopes: List<EnvelopeBalance> = emptyList(),
    val groups: List<InboxTransactionGroup> = emptyList(),
    val profileSettings: ProfileSettings = ProfileSettings(),
)

data class InboxTransactionGroup(
    val key: String,
    val title: String,
    val transactionIds: List<Long>,
    val amountCents: Long,
    val postedAtMillis: Long,
)

class ReviewInboxViewModel(
    private val repository: BudgetRepository,
) : ViewModel() {
    val state: StateFlow<ReviewInboxState> =
        combine(
            repository.observeEnvelopeBalances(),
            repository.observeInbox(),
            repository.observeProfileSettings(),
        ) { envelopes, inbox, profileSettings ->
            ReviewInboxState(
                envelopes = envelopes.filter { it.type in EnvelopeType.SpendingTypes },
                groups = inbox.toInboxGroups(),
                profileSettings = profileSettings,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReviewInboxState(),
        )

    fun assignGroup(
        group: InboxTransactionGroup,
        envelopeId: Long,
        createRule: Boolean,
    ) {
        viewModelScope.launch {
            group.transactionIds.forEach { transactionId ->
                repository.assignTransactionToEnvelope(transactionId, envelopeId)
            }
            if (createRule) {
                runCatching {
                    repository.createRule(
                        pattern = group.title.toInboxRulePattern(),
                        envelopeId = envelopeId,
                    )
                }
            }
        }
    }
}

private fun List<TransactionEntity>.toInboxGroups(): List<InboxTransactionGroup> =
    groupBy { transaction ->
        val merchant = transaction.merchantName ?: transaction.description
        merchant.trim().uppercase(Locale.US)
    }.map { (key, transactions) ->
        val newest = transactions.maxBy { it.postedAtMillis }
        InboxTransactionGroup(
            key = key,
            title = newest.merchantName ?: newest.description,
            transactionIds = transactions.map { it.id },
            amountCents = transactions.sumOf { it.amountCents },
            postedAtMillis = newest.postedAtMillis,
        )
    }.sortedByDescending { it.postedAtMillis }

private fun String.toInboxRulePattern(): String =
    trim()
        .split(Regex("\\s+"))
        .firstOrNull()
        .orEmpty()
        .ifBlank { trim() }
