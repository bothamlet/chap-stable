package com.budgetapp.app.ui.screens

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.app.data.entity.EnvelopeEntity
import com.budgetapp.app.data.model.EnvelopeType
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.data.model.toEnvelopeType
import com.budgetapp.app.data.repository.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EnvelopeManagementState(
    val envelopes: List<EnvelopeRow> = emptyList(),
    val errorMessage: String? = null,
    val profileSettings: ProfileSettings = ProfileSettings(),
) {
    val variable: List<EnvelopeRow> = envelopes.filter { it.type == EnvelopeType.Variable }
    val fixed: List<EnvelopeRow> = envelopes.filter { it.type == EnvelopeType.Fixed }
    val goals: List<EnvelopeRow> = envelopes.filter { it.type == EnvelopeType.Goal }
}

data class EnvelopeRow(
    val id: Long,
    val name: String,
    val ceilingCents: Long,
    val spentCents: Long,
    val type: String,
    val source: EnvelopeEntity,
) {
    val remainingCents: Long = ceilingCents - spentCents
    val progress: Float = if (ceilingCents <= 0L) 0f else (spentCents.toFloat() / ceilingCents).coerceIn(0f, 1f)
}

data class EnvelopeEditorState(
    val envelopeId: Long? = null,
    val name: String = "",
    val amountText: String = "",
    val iconToken: String = "wallet",
    val type: String = EnvelopeType.Variable,
    val isDeleteConfirming: Boolean = false,
    val hasSubmitted: Boolean = false,
) {
    val isEditing: Boolean = envelopeId != null
    val isNameInvalid: Boolean = hasSubmitted && name.isBlank()
    val isAmountInvalid: Boolean = hasSubmitted && amountText.toCentsOrNull() == null
}

class EnvelopeManagementViewModel(
    private val repository: BudgetRepository,
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<EnvelopeManagementState> =
        combine(
            repository.observeActiveEnvelopes(),
            repository.observeEnvelopeBalances(),
            repository.observeProfileSettings(),
            errorMessage,
        ) { entities, balances, profileSettings, error ->
            val byId = balances.associateBy { it.id }
            EnvelopeManagementState(
                envelopes =
                    entities.map { entity ->
                        val balance = byId[entity.id]
                        EnvelopeRow(
                            id = entity.id,
                            name = entity.name,
                            ceilingCents = balance?.ceilingCents ?: entity.ceilingCents,
                            spentCents = balance?.spentCents ?: 0L,
                            type = entity.type.toEnvelopeType(),
                            source = entity,
                        )
                    },
                profileSettings = profileSettings,
                errorMessage = error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EnvelopeManagementState(),
        )

    fun saveEnvelope(
        editor: EnvelopeEditorState,
        onClose: () -> Unit,
    ) {
        val name = editor.name.trim()
        val amountCents = editor.amountText.toCentsOrNull()
        if (name.isBlank() || amountCents == null) {
            return
        }

        viewModelScope.launch {
            errorMessage.value = null
            try {
                if (editor.envelopeId == null) {
                    repository.createEnvelope(
                        name = name,
                        ceilingCents = amountCents,
                        type = editor.type,
                        iconToken = editor.iconToken,
                    )
                } else {
                    val current =
                        state.value.envelopes
                            .firstOrNull { it.id == editor.envelopeId }
                            ?.source
                    if (current != null) {
                        repository.updateEnvelope(
                            current.copy(
                                name = name,
                                ceilingCents = amountCents,
                                iconToken = editor.iconToken,
                                type = editor.type.toEnvelopeType(),
                            ),
                        )
                    }
                }
                onClose()
            } catch (_: SQLiteConstraintException) {
                // Keep the drawer open and say why, instead of silently doing nothing.
                errorMessage.value = "An envelope named \"$name\" already exists."
            }
        }
    }

    fun deleteEnvelope(
        envelopeId: Long,
        onClose: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.archiveEnvelope(envelopeId)
            onClose()
        }
    }
}
