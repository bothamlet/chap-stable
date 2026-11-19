package com.budgetapp.app.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class MutationStatus(val isBusy: Boolean = false, val error: String? = null)

/** Set the guard before launching so two taps in the same frame cannot enqueue two writes. */
internal fun ViewModel.performMutation(
    status: MutableStateFlow<MutationStatus>,
    onDone: () -> Unit = {},
    action: suspend () -> Unit,
) {
    if (status.value.isBusy) return
    status.value = MutationStatus(isBusy = true)
    viewModelScope.launch {
        try {
            action()
            status.value = MutationStatus()
            onDone()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val message = when (error) {
                is android.database.sqlite.SQLiteConstraintException -> "An envelope or rule with these details already exists."
                is IllegalArgumentException -> error.message ?: "Please check your entries and try again."
                else -> "Could not save this change. Please try again."
            }
            status.value = MutationStatus(error = message)
        } finally {
            if (status.value.isBusy) status.value = MutationStatus()
        }
    }
}

@Composable
internal fun MutationErrorDialog(status: MutationStatus, onDismiss: () -> Unit) {
    if (status.error != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Could not save change") },
            text = { Text(status.error) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        )
    }
}
