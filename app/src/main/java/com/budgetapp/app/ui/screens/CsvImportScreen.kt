package com.budgetapp.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.ui.components.StandardEmptyState
import com.budgetapp.app.ui.components.StandardEmptyStateViewport
import com.budgetapp.app.security.LocalTrustedExternalActivityCoordinator
import com.budgetapp.app.security.launchTrustedExternalActivity
import com.budgetapp.app.ui.components.CompactHeaderBottomPadding
import com.budgetapp.app.ui.components.CompactHeaderHeroGap
import com.budgetapp.app.ui.components.CompactHeaderTopPadding
import com.budgetapp.app.ui.theme.MockBackground
import com.budgetapp.app.ui.theme.MockFillMuted
import com.budgetapp.app.ui.theme.MockMaterialSurface
import com.budgetapp.app.ui.theme.MockNeutral
import com.budgetapp.app.ui.theme.MockPrimaryAlt
import com.budgetapp.app.ui.theme.MockSurface
import com.budgetapp.app.ui.theme.MockSurfaceNearWhiteAlt
import com.budgetapp.app.ui.theme.MockSurfacePale
import com.budgetapp.app.ui.theme.MockTextLight
import com.budgetapp.app.ui.theme.MockTextLightMuted
import com.budgetapp.app.ui.theme.MockWhite
import java.io.ByteArrayOutputStream

@Composable
fun CsvImportScreen(
    viewModel: CsvImportViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onBottomActionVisibleChange: (Boolean) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val externalActivityCoordinator = LocalTrustedExternalActivityCoordinator.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        externalActivityCoordinator.end()
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "import.csv" }
            // Up to 10 MB are read and inspected, which must not happen on the main thread.
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { context.readText(uri) }
                }.onSuccess { content ->
                    viewModel.loadFile(fileName, content)
                }.onFailure { error ->
                    viewModel.showError(error.message ?: "Could not read CSV")
                }
            }
        }
    }
    LaunchedEffect(state.isMapping) {
        onBottomActionVisibleChange(state.isMapping)
    }

    if (state.isMapping) {
        CsvMappingContent(
            state = state,
            onBack = onBack,
            onToggleHeader = viewModel::toggleHeader,
            onDateIndex = viewModel::setDateIndex,
            onDescriptionIndex = viewModel::setDescriptionIndex,
            onAmountIndex = viewModel::setAmountIndex,
            onMerchantIndex = viewModel::setMerchantIndex,
            onDebitCreditIndex = viewModel::setDebitCreditIndex,
            onExternalIdIndex = viewModel::setExternalIdIndex,
            onImport = viewModel::import,
            modifier = modifier,
        )
    } else {
        CsvLandingContent(
            error = state.error,
            onBack = onBack,
            onChooseFile = {
                externalActivityCoordinator.launchTrustedExternalActivity {
                    launcher.launch(
                        arrayOf(
                            "text/*",
                            "text/csv",
                            "application/csv",
                            "application/vnd.ms-excel",
                            "application/octet-stream",
                            "*/*",
                        ),
                    )
                }
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun CsvLandingContent(
    error: String?,
    onBack: () -> Unit,
    onChooseFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MockSurface),
    ) {
        CsvDarkNav(title = "Import CSV", onBack = onBack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            StandardEmptyStateViewport {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StandardEmptyState(
                    title = "Select a file from your bank",
                    copy = "CSV, semicolon or comma separated",
                    actionText = "Choose file",
                    onAction = onChooseFile,
                    icon = {
                        CsvIcon(CsvIconKind.FileUpload, Modifier.size(36.dp), MockPrimaryAlt)
                    },
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = error,
                        color = MockTextLightMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun CsvMappingContent(
    state: CsvImportState,
    onBack: () -> Unit,
    onToggleHeader: () -> Unit,
    onDateIndex: (Int) -> Unit,
    onDescriptionIndex: (Int) -> Unit,
    onAmountIndex: (Int) -> Unit,
    onMerchantIndex: (Int) -> Unit,
    onDebitCreditIndex: (Int) -> Unit,
    onExternalIdIndex: (Int) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inspection = state.inspection ?: return
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MockBackground),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 164.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MockSurface)
                        .padding(start = 24.dp, end = 24.dp, top = CompactHeaderTopPadding, bottom = CompactHeaderBottomPadding),
                ) {
                    CsvNavRow(title = "Column mapping", onBack = onBack)
                    Spacer(modifier = Modifier.height(CompactHeaderHeroGap))
                    Text(
                        text = "Match your CSV columns",
                        color = MockWhite,
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = "Tell Chap which columns contain the date, merchant, and amount before importing.",
                        color = MockTextLightMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    HeaderToggleCard(checked = state.hasHeader, onClick = onToggleHeader)
                    // No column of any kind was recognised: this is not a CSV export, so showing a
                    // preview of binary rubbish and a mapping form would only confuse.
                    if (state.hasNoRecognisableColumns) {
                        UnrecognisedFileNotice()
                    } else {
                        MappingCard(
                            state = state,
                            onDateIndex = onDateIndex,
                            onDescriptionIndex = onDescriptionIndex,
                            onAmountIndex = onAmountIndex,
                            onMerchantIndex = onMerchantIndex,
                            onDebitCreditIndex = onDebitCreditIndex,
                            onExternalIdIndex = onExternalIdIndex,
                        )
                        PreviewSection(state = state, rows = inspection.rows)
                    }
                    Text(
                        text = state.result?.let {
                            importSummary(it)
                        }
                            ?: state.error
                            ?: "Only mapped fields will be imported.",
                        color = MockNeutral,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        // Column, not Box: the hint above the button would otherwise be drawn underneath it.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MockBackground)
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 32.dp),
        ) {
            if (!state.isMappingComplete) {
                Text(
                    text = "Choose which column holds the date, description and amount.",
                    color = MockNeutral,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
            }
            Button(
                onClick = onImport,
                enabled = !state.isImporting && state.columns.isNotEmpty() && state.isMappingComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MockPrimaryAlt,
                    contentColor = MockWhite,
                    disabledContainerColor = MockFillMuted,
                    disabledContentColor = MockNeutral,
                ),
            ) {
                Text(if (state.isImporting) "Importing..." else "Confirm & Import", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CsvDarkNav(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = CompactHeaderTopPadding, bottom = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = MockWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CsvNavRow(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = MockWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HeaderToggleCard(
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfacePale, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("First row is header", color = MockMaterialSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 32.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (checked) MockPrimaryAlt else MockSurfaceNearWhiteAlt)
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MockWhite),
            )
        }
    }
}

@Composable
private fun MappingCard(
    state: CsvImportState,
    onDateIndex: (Int) -> Unit,
    onDescriptionIndex: (Int) -> Unit,
    onAmountIndex: (Int) -> Unit,
    onMerchantIndex: (Int) -> Unit,
    onDebitCreditIndex: (Int) -> Unit,
    onExternalIdIndex: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfacePale, RoundedCornerShape(20.dp)),
    ) {
        MappingRow("Date", state, state.dateIndex, onDateIndex, required = true)
        MappingRow("Description", state, state.descriptionIndex, onDescriptionIndex, required = true)
        MappingRow("Amount", state, state.amountIndex, onAmountIndex, required = true)
        MappingRow("Merchant", state, state.merchantIndex, onMerchantIndex)
        MappingRow("Debit/Credit", state, state.debitCreditIndex, onDebitCreditIndex)
        MappingRow("Transaction ID", state, state.externalIdIndex, onExternalIdIndex, showDivider = false)
    }
}

@Composable
private fun MappingRow(
    label: String,
    state: CsvImportState,
    selectedIndex: Int?,
    onSelected: (Int) -> Unit,
    showDivider: Boolean = true,
    required: Boolean = false,
) {
    val displayIndex = selectedIndex ?: -1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showDivider) Modifier.border(0.dp, MockSurfacePale) else Modifier)
            .padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MockMaterialSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        ColumnOption(
            text = state.columnLabel(displayIndex, required),
            onClick = { onSelected(nextColumnIndex(displayIndex, state.columns.size)) },
        )
    }
}

@Composable
private fun ColumnOption(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .widthIn(min = 168.dp)
            .height(45.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(MockSurfaceNearWhiteAlt)
            .border(1.dp, MockFillMuted, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = MockMaterialSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PreviewSection(
    state: CsvImportState,
    rows: List<List<String>>,
) {
    val dataRows = if (state.hasHeader) rows.drop(state.headerRowIndex + 1) else rows
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("DATA PREVIEW", color = MockNeutral, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.56.sp)
            Text("${state.rowCount.coerceAtLeast(0)} rows", color = MockNeutral, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MockWhite)
                .border(1.dp, MockSurfacePale, RoundedCornerShape(20.dp)),
        ) {
            dataRows.take(5).forEachIndexed { index, row ->
                PreviewRow(
                    date = row.getOrNull(state.dateIndex).orEmpty(),
                    description = row.getOrNull(state.descriptionIndex).orEmpty(),
                    amount = row.getOrNull(state.amountIndex).orEmpty(),
                    muted = index >= 4,
                )
            }
        }
    }
}

@Composable
private fun PreviewRow(
    date: String,
    description: String,
    amount: String,
    muted: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(date, color = if (muted) MockTextLight else MockNeutral, fontSize = 14.sp, modifier = Modifier.weight(0.34f), maxLines = 1)
        Text(description, color = if (muted) MockNeutral else MockMaterialSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.44f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(amount, color = if (muted) MockNeutral else MockMaterialSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.22f), maxLines = 1)
    }
}

@Composable
private fun CsvIcon(
    kind: CsvIconKind,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        when (kind) {
            CsvIconKind.Back -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.22f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.78f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            }
            CsvIconKind.FileUpload -> {
                drawRoundRect(tint, topLeft = Offset(w * 0.18f, h * 0.08f), size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.84f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.72f), Offset(w * 0.5f, h * 0.42f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.37f, h * 0.54f), Offset(w * 0.5f, h * 0.4f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.63f, h * 0.54f), Offset(w * 0.5f, h * 0.4f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

private enum class CsvIconKind {
    Back,
    FileUpload,
}

private fun Context.readText(uri: Uri): String {
    contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
        val size = descriptor.statSize
        require(size <= MaxCsvBytes || size == -1L) {
            "CSV file is too large."
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
            require(totalBytes <= MaxCsvBytes) {
                "CSV file is too large."
            }
            output.write(buffer, 0, read)
        }
        output.toString(Charsets.UTF_8.name())
    } ?: error("Could not open CSV file.")
}

private const val MaxCsvBytes = 10L * 1024L * 1024L

private fun CsvImportState.columnLabel(
    index: Int,
    required: Boolean = false,
): String {
    if (index < 0) {
        return if (required) "Choose..." else "Skip"
    }
    val column = columns.getOrNull(index).orEmpty().ifBlank { "column" }
    return "Col $index  ($column)"
}

private fun nextColumnIndex(current: Int, columnCount: Int): Int {
    if (columnCount <= 0) return -1
    return when (current) {
        -1 -> 0
        else -> if (current >= columnCount - 1) -1 else current + 1
    }
}

private fun importSummary(result: com.budgetapp.app.data.model.ImportResult): String {
    val parts = mutableListOf("Imported ${result.acceptedRows} rows.")
    if (result.duplicateRows > 0) {
        parts += "${result.duplicateRows} duplicates skipped."
    }
    if (result.incomeRows > 0) {
        parts += "${result.incomeRows} income rows are waiting in the inbox."
    }
    return parts.joinToString(" ")
}

@Composable
private fun UnrecognisedFileNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfacePale, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text(
            text = "This file does not look like a CSV export",
            color = MockMaterialSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Chap could not find date, description or amount columns in it. Export your statement as CSV from your bank and try again.",
            color = MockNeutral,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}
