package com.budgetapp.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.ui.components.StandardLightHeader
import com.budgetapp.app.ui.theme.MockBackground
import com.budgetapp.app.ui.theme.MockDanger
import com.budgetapp.app.ui.theme.MockInk
import com.budgetapp.app.ui.theme.MockMaterialSurface
import com.budgetapp.app.ui.theme.MockNeutral
import com.budgetapp.app.ui.theme.MockPrimaryAlt
import com.budgetapp.app.ui.theme.MockSurfaceNearWhite
import com.budgetapp.app.ui.theme.MockSurfacePale
import com.budgetapp.app.ui.theme.MockTextMuted
import com.budgetapp.app.ui.theme.MockWhite
import com.budgetapp.app.security.LocalTrustedExternalActivityCoordinator
import com.budgetapp.app.security.launchTrustedExternalActivity
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onModalVisibleChange: (Boolean) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val externalActivityCoordinator = LocalTrustedExternalActivityCoordinator.current
    var isDeleteConfirming by remember { mutableStateOf(false) }
    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupMimeType),
    ) { uri ->
        externalActivityCoordinator.end()
        uri?.let { targetUri ->
            viewModel.createBackup { bytes ->
                context.writeBackupBytes(targetUri, bytes)
            }
        }
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        externalActivityCoordinator.end()
        uri?.let { sourceUri ->
            viewModel.restoreBackup {
                context.readBackupBytes(sourceUri)
            }
        }
    }
    LaunchedEffect(isDeleteConfirming) {
        onModalVisibleChange(isDeleteConfirming)
    }
    LaunchedEffect(state.error) {
        if (state.error != null) {
            isDeleteConfirming = false
        }
    }
    if (isDeleteConfirming) {
        BackHandler { isDeleteConfirming = false }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MockBackground),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StandardLightHeader(
                title = "Backup",
                heading = "Keep your budget safe",
                copy = "Export a local backup or restore one from your device.",
                onBack = onBack,
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    androidx.compose.foundation.layout
                        .PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 164.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    BackupCard(
                        title = "Create backup",
                        copy = "Save an encrypted local backup for this device and app install.",
                        button = if (state.isCreatingBackup) "Creating..." else "Create backup",
                        buttonColor = MockPrimaryAlt,
                        enabled = !state.isBusy,
                        onClick = {
                            externalActivityCoordinator.launchTrustedExternalActivity {
                                createBackupLauncher.launch(viewModel.suggestedBackupFileName())
                            }
                        },
                    )
                    BackupCard(
                        title = "Restore backup",
                        copy = "Restore a backup created on this same device and replace current local data.",
                        button = if (state.isRestoringBackup) "Restoring..." else "Choose backup file",
                        buttonColor = MockSurfaceNearWhite,
                        textColor = MockTextMuted,
                        enabled = !state.isBusy,
                        onClick = {
                            externalActivityCoordinator.launchTrustedExternalActivity {
                                restoreBackupLauncher.launch(arrayOf(BackupMimeType, "application/octet-stream", "*/*"))
                            }
                        },
                    )
                    BackupCard(
                        title = "Delete data",
                        copy = "Permanently remove all local envelopes, rules, profile settings, and transactions.",
                        button = if (state.isDeletingData) "Deleting..." else "Delete data",
                        buttonColor = MockDanger.copy(alpha = 0.08f),
                        textColor = MockDanger,
                        enabled = !state.isBusy,
                        onClick = { isDeleteConfirming = true },
                    )
                    state.error?.let { error ->
                        BackupStatusText(text = error, isError = true)
                    } ?: state.message?.let { message ->
                        BackupStatusText(text = message, isError = false)
                    }
                    BackupMetaPanel(lastBackup = formatBackupTimestamp(state.lastBackupAtMillis))
                    Text(
                        text = "Backup files stay on your device unless you choose to share or store them elsewhere. Current backups are protected by this device's local key.",
                        color = MockNeutral,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(top = 8.dp),
                    )
                }
            }
        }
        if (isDeleteConfirming) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MockInk.copy(alpha = 0.54f))
                        .clickable { isDeleteConfirming = false },
                contentAlignment = Alignment.BottomCenter,
            ) {
                DeleteDataConfirmation(
                    onCancel = { isDeleteConfirming = false },
                    onDelete = {
                        viewModel.deleteAllData {
                            isDeleteConfirming = false
                        }
                    },
                    isDeleting = state.isDeletingData,
                    modifier = Modifier.clickable {},
                )
            }
        }
    }
}

@Composable
private fun BackupCard(
    title: String,
    copy: String,
    button: String,
    buttonColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color = MockWhite,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MockWhite)
                .border(1.dp, MockSurfacePale, RoundedCornerShape(20.dp))
                .padding(20.dp),
    ) {
        Text(title, color = MockMaterialSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(copy, color = MockNeutral, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = textColor,
                    disabledContainerColor = MockSurfaceNearWhite,
                    disabledContentColor = MockTextMuted,
                ),
        ) {
            Text(button, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BackupStatusText(
    text: String,
    isError: Boolean,
) {
    Text(
        text = text,
        color = if (isError) MockDanger else MockPrimaryAlt,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun BackupMetaPanel(lastBackup: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MockWhite.copy(alpha = 0.56f))
                .border(1.dp, MockSurfacePale, RoundedCornerShape(22.dp))
                .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BackupMetaRow("Last backup", lastBackup)
        BackupMetaRow("Storage", "Local device")
    }
}

@Composable
private fun BackupMetaRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MockNeutral, fontSize = 14.sp)
        Text(value, color = MockMaterialSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
private fun DeleteDataConfirmation(
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    isDeleting: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MockWhite)
                .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Delete all data?",
            color = MockMaterialSurface,
            fontSize = 22.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This permanently removes everything stored locally on this device.",
            color = MockNeutral,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onCancel,
                enabled = !isDeleting,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MockSurfaceNearWhite, contentColor = MockTextMuted),
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onDelete,
                enabled = !isDeleting,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MockDanger, contentColor = MockWhite),
            ) {
                Text(if (isDeleting) "Deleting..." else "Delete", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun Context.writeBackupBytes(
    uri: Uri,
    bytes: ByteArray,
) {
    contentResolver.openOutputStream(uri, "wt")?.use { output ->
        output.write(bytes)
    } ?: error("Could not open backup destination.")
}

private fun Context.readBackupBytes(uri: Uri): ByteArray {
    contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
        val size = descriptor.statSize
        require(size <= MaxBackupBytes || size == -1L) {
            "Backup file is too large."
        }
    }
    return contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            totalBytes += read
            require(totalBytes <= MaxBackupBytes) {
                "Backup file is too large."
            }
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    } ?: error("Could not open backup file.")
}

private fun formatBackupTimestamp(timestampMillis: Long?): String {
    if (timestampMillis == null) return "Never"
    val formatter = DateTimeFormatter
        .ofPattern("MMM d, yyyy HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(timestampMillis))
}

private const val BackupMimeType = "application/json"
private const val MaxBackupBytes = 25L * 1024L * 1024L
