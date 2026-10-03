package com.budgetapp.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.app.data.entity.TransactionEntity
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.data.model.EnvelopeType
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.data.repository.BudgetRepository
import com.budgetapp.app.domain.budgetPeriod
import com.budgetapp.app.domain.BudgetPeriod
import com.budgetapp.app.domain.dayLine
import com.budgetapp.app.domain.periodLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardState(
    val period: BudgetPeriod = ProfileSettings().budgetPeriod(),
    val monthLabel: String = "",
    val dayLine: String = "",
    val isCurrentPeriod: Boolean = true,
    val canGoBack: Boolean = true,
    val canGoForward: Boolean = false,
    val envelopes: List<EnvelopeBalance> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val inboxCount: Int = 0,
    val profileSettings: ProfileSettings = ProfileSettings(),
) {
    val spendingEnvelopes: List<EnvelopeBalance> = envelopes.filter { it.type in EnvelopeType.SpendingTypes }
    val variableEnvelopes: List<EnvelopeBalance> = envelopes.filter { it.type == EnvelopeType.Variable }
    val fixedEnvelopes: List<EnvelopeBalance> = envelopes.filter { it.type == EnvelopeType.Fixed }
    val goalEnvelopes: List<EnvelopeBalance> = envelopes.filter { it.type == EnvelopeType.Goal }
}

class EnvelopeListViewModel(
    private val repository: BudgetRepository,
) : ViewModel() {
    val cashEditor = androidx.compose.runtime.mutableStateOf<CashTransactionEditorState?>(null)
    val mutationStatus = MutableStateFlow(MutationStatus())
    val state: StateFlow<DashboardState> =
        combine(
            repository.observeEnvelopeBalances(),
            repository.observeTransactions(),
            repository.observeInbox(),
            repository.observeProfileSettings(),
            repository.observeViewedPeriod(),
        ) { envelopes, transactions, inbox, profileSettings, period ->
            val offset = repository.observePeriodOffsetMonths().value
            DashboardState(
                period = period,
                monthLabel = profileSettings.periodLabel(period),
                // "Day 3 of 31" only makes sense for the period the user is actually in.
                dayLine = if (offset == 0) profileSettings.dayLine(period) else "${period.lengthInDays} days",
                isCurrentPeriod = offset == 0,
                canGoBack = offset > -120,
                canGoForward = offset < 0,
                envelopes = envelopes.filter { it.type in EnvelopeType.SpendingTypes },
                transactions = transactions,
                inboxCount = inbox.size,
                profileSettings = profileSettings,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardState(),
        )

    fun showPreviousPeriod() = repository.shiftPeriod(-1)

    fun showNextPeriod() = repository.shiftPeriod(1)

    fun showCurrentPeriod() = repository.showCurrentPeriod()

    fun addCashTransaction(
        editor: CashTransactionEditorState,
        onSaved: () -> Unit,
    ) {
        val amountCents = editor.amountText.toCashCentsOrNull()
        val envelopeId = editor.envelopeId
        if (!editor.canSave || amountCents == null) {
            return
        }
        performMutation(mutationStatus, onSaved) {
            repository.createCashTransaction(
                merchantName = editor.merchant,
                amountCents = amountCents,
                envelopeId = envelopeId,
                postedAtMillis = editor.date.toStartOfDayMillis(),
                note = editor.note,
                isIncome = false,
            )
        }
    }
}

data class CashTransactionEditorState(
    val merchant: String = "",
    val amountText: String = "",
    val envelopeId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val isNoteOpen: Boolean = false,
    val hasSubmitted: Boolean = false,
    val isIncome: Boolean = false,
) {
    val isMerchantInvalid: Boolean = hasSubmitted && merchant.trim().isBlank()
    val isAmountInvalid: Boolean = hasSubmitted && amountText.toCashCentsOrNull() == null
    val canSave: Boolean =
        merchant.trim().isNotBlank() &&
            amountText.toCashCentsOrNull() != null &&
            (!isIncome && envelopeId != null)
}
