package com.budgetapp.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.app.data.repository.BudgetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class BackupRestoreState(
    val lastBackupAtMillis: Long? = null,
    val isCreatingBackup: Boolean = false,
    val isRestoringBackup: Boolean = false,
    val isDeletingData: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val isBusy: Boolean = isCreatingBackup || isRestoringBackup || isDeletingData
}

class BackupRestoreViewModel(
    private val repository: BudgetRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        BackupRestoreState(lastBackupAtMillis = repository.getLastBackupAtMillis()),
    )
    val state: StateFlow<BackupRestoreState> = mutableState

    fun suggestedBackupFileName(): String =
        "chap-backup-${DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now())}.chapbackup"

    fun createBackup(writeBackup: suspend (ByteArray) -> Unit) {
        if (mutableState.value.isBusy) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(isCreatingBackup = true, message = null, error = null)
            }
            runCatching {
                val export = repository.createEncryptedBackup()
                withContext(Dispatchers.IO) {
                    writeBackup(export.bytes)
                    repository.recordBackupCreated(export.exportedAtMillis)
                }
                export
            }.onSuccess { export ->
                mutableState.update {
                    it.copy(
                        lastBackupAtMillis = export.exportedAtMillis,
                        isCreatingBackup = false,
                        message = "Encrypted backup created with ${export.transactionCount} transactions.",
                    )
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        isCreatingBackup = false,
                        error = error.userMessage("Could not create backup."),
                    )
                }
            }
        }
    }

    fun restoreBackup(readBackup: suspend () -> ByteArray) {
        if (mutableState.value.isBusy) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(isRestoringBackup = true, message = null, error = null)
            }
            runCatching {
                val bytes = withContext(Dispatchers.IO) { readBackup() }
                repository.restoreEncryptedBackup(bytes)
            }.onSuccess { result ->
                mutableState.update {
                    it.copy(
                        lastBackupAtMillis = result.exportedAtMillis,
                        isRestoringBackup = false,
                        message = "Restored ${result.transactionCount} transactions and ${result.envelopeCount} envelopes.",
                    )
                }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        isRestoringBackup = false,
                        error = error.userMessage("Could not restore backup."),
                    )
                }
            }
        }
    }

    fun deleteAllData(onDone: () -> Unit) {
        if (mutableState.value.isBusy) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(isDeletingData = true, message = null, error = null)
            }
            runCatching {
                repository.deleteAllLocalData()
            }.onSuccess {
                mutableState.update {
                    it.copy(
                        lastBackupAtMillis = null,
                        isDeletingData = false,
                        message = "Local data deleted.",
                    )
                }
                onDone()
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        isDeletingData = false,
                        error = error.userMessage("Could not delete local data."),
                    )
                }
            }
        }
    }

    private fun Throwable.userMessage(fallback: String): String =
        message?.takeIf { it.isNotBlank() } ?: fallback
}
