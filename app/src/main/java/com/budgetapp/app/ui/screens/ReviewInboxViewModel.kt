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
            val pattern = group.title.toInboxRulePattern()
            // A blank pattern would match every future row (contains("") is always true), so a
            // merchant name with no word characters simply creates no rule.
            if (createRule && pattern.isNotBlank()) {
                runCatching {
                    repository.createRule(
                        pattern = pattern,
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

/**
 * Rule text derived from a merchant name. The first word is enough, but it has to lose its
 * punctuation: "Coffee, large" used to produce the pattern "Coffee," which then only matched rows
 * that also contained a comma, so sibling merchants kept landing in the inbox.
 */
internal fun String.toInboxRulePattern(): String {
    val firstWord = trim().split(Regex("\\s+")).firstOrNull().orEmpty()
    val cleaned = firstWord.trim { !it.isLetterOrDigit() }
    return cleaned.ifBlank { trim().trim { !it.isLetterOrDigit() } }
}
