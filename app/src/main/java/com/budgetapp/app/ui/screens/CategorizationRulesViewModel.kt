package com.budgetapp.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.app.data.entity.CategorizationRuleEntity
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.data.model.EnvelopeType
import com.budgetapp.app.data.repository.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class CategorizationRulesState(
    val rules: List<RuleRow> = emptyList(),
    val envelopes: List<EnvelopeBalance> = emptyList(),
    val query: String = "",
) {
    val filteredRules: List<RuleRow> =
        if (query.isBlank()) {
            rules
        } else {
            rules.filter {
                it.rule.pattern.contains(query, ignoreCase = true) ||
                    it.envelopeName.contains(query, ignoreCase = true)
            }
        }
}

data class RuleRow(
    val rule: CategorizationRuleEntity,
    val envelopeName: String,
)

class CategorizationRulesViewModel(
    private val repository: BudgetRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    val state: StateFlow<CategorizationRulesState> =
        combine(
            repository.observeRules(),
            repository.observeEnvelopeBalances(),
            query,
        ) { rules, envelopes, search ->
            val spendingEnvelopes = envelopes.filter { it.type in EnvelopeType.SpendingTypes }
            CategorizationRulesState(
                rules =
                    rules.filter { it.isEnabled }.map { rule ->
                        RuleRow(
                            rule = rule,
                            envelopeName = spendingEnvelopes.firstOrNull { it.id == rule.envelopeId }?.name ?: "Unknown",
                        )
                    },
                envelopes = spendingEnvelopes,
                query = search,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategorizationRulesState(),
        )

    fun setQuery(value: String) {
        query.value = value
    }

    fun moveRule(
        rule: CategorizationRuleEntity,
        envelopeId: Long,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.updateRule(rule.copy(envelopeId = envelopeId))
            onDone()
        }
    }

    fun deleteRule(
        ruleId: Long,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.deleteRule(ruleId)
            onDone()
        }
    }
}
