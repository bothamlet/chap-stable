package com.budgetapp.app.domain

import com.budgetapp.app.data.entity.CategorizationRuleEntity
import com.budgetapp.app.data.model.CsvTransactionDraft
import com.budgetapp.app.data.model.RuleMatchField
import java.util.Locale

class CategorizationEngine {
    fun categorize(
        draft: CsvTransactionDraft,
        rules: List<CategorizationRuleEntity>,
    ): Long? {
        return rules.firstOrNull { rule -> rule.matches(draft) }?.envelopeId
    }

    private fun CategorizationRuleEntity.matches(draft: CsvTransactionDraft): Boolean {
        val normalizedPattern = pattern.lowercase(Locale.US)
        val description = draft.description.lowercase(Locale.US)
        val merchant = draft.merchantName?.lowercase(Locale.US).orEmpty()

        return when (matchField) {
            RuleMatchField.Description -> description.contains(normalizedPattern)
            RuleMatchField.Merchant -> merchant.contains(normalizedPattern)
            else -> description.contains(normalizedPattern) || merchant.contains(normalizedPattern)
        }
    }
}
