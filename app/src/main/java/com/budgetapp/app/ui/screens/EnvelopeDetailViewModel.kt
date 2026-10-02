package com.budgetapp.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.app.data.entity.TransactionEntity
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.data.model.EnvelopeType
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.data.repository.BudgetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

data class EnvelopeDetailState(
    val envelope: EnvelopeBalance? = null,
    val envelopes: List<EnvelopeBalance> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val profileSettings: ProfileSettings = ProfileSettings(),
) {
    val isGoal: Boolean = envelope?.type == EnvelopeType.Goal
    val spentCents: Long = envelope?.spentCents ?: 0L
    val ceilingCents: Long = envelope?.ceilingCents ?: 0L
    val remainingCents: Long = ceilingCents - spentCents
    val progress: Float = if (ceilingCents <= 0L) 0f else (spentCents.toFloat() / ceilingCents).coerceIn(0f, 1f)
}

data class TransactionEditorState(
    val transaction: TransactionEntity,
    val envelopeId: Long?,
    val amountText: String,
    val date: LocalDate,
    val note: String,
    val isDeleteConfirming: Boolean = false,
)

data class DetailAddTransactionEditorState(
    val envelopeId: Long,
    val merchant: String = "",
    val amountText: String = "",
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val isNoteOpen: Boolean = false,
    val hasSubmitted: Boolean = false,
) {
    val canSave: Boolean = merchant.trim().isNotBlank() && amountText.toDetailCentsOrNull() != null
    val merchantError: Boolean = hasSubmitted && merchant.trim().isBlank()
    val amountError: Boolean = hasSubmitted && amountText.toDetailCentsOrNull() == null
}

@OptIn(ExperimentalCoroutinesApi::class)
class EnvelopeDetailViewModel(
    private val repository: BudgetRepository,
) : ViewModel() {
    private val envelopeId = MutableStateFlow<Long?>(null)

    private val periodTransactions: Flow<List<TransactionEntity>> =
        envelopeId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.observePeriodTransactionsForEnvelope(id)
        }

    val state: StateFlow<EnvelopeDetailState> =
        combine(
            envelopeId,
            repository.observeEnvelopeBalances(),
            periodTransactions,
            repository.observeProfileSettings(),
        ) { id, envelopes, transactions, profileSettings ->
            EnvelopeDetailState(
                envelope = envelopes.firstOrNull { it.id == id },
                envelopes = envelopes,
                transactions = transactions,
                profileSettings = profileSettings,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EnvelopeDetailState(),
        )

    fun load(id: Long) {
        envelopeId.value = id
    }

    fun saveTransaction(
        editor: TransactionEditorState,
        onClose: () -> Unit,
    ) {
        val cents = editor.amountText.toDetailCentsOrNull() ?: return
        val selectedEnvelope = state.value.envelopes.firstOrNull { it.id == editor.envelopeId }
        val signedCents = if (selectedEnvelope?.type == EnvelopeType.Goal) abs(cents) else -abs(cents)
        viewModelScope.launch {
            repository.updateTransaction(
                editor.transaction.copy(
                    envelopeId = editor.envelopeId,
                    amountCents = signedCents,
                    note = editor.note.trim().ifBlank { null },
                    postedAtMillis = editor.date.toDetailStartOfDayMillis(),
                ),
            )
            onClose()
        }
    }

    fun deleteTransaction(
        transactionId: Long,
        onClose: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            onClose()
        }
    }

    fun addTransaction(
        editor: DetailAddTransactionEditorState,
        onClose: () -> Unit,
    ) {
        val cents = editor.amountText.toDetailCentsOrNull() ?: return
        val merchant = editor.merchant.trim()
        if (merchant.isBlank()) return

        viewModelScope.launch {
            repository.createCashTransaction(
                merchantName = merchant,
                amountCents = cents,
                envelopeId = editor.envelopeId,
                postedAtMillis = editor.date.toDetailStartOfDayMillis(),
                note = editor.note.trim(),
            )
            onClose()
        }
    }
}
