package com.budgetapp.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.data.entity.TransactionEntity
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.ui.components.EnvelopeCategoryIcon
import com.budgetapp.app.ui.theme.MockBackground
import com.budgetapp.app.ui.theme.MockDanger
import com.budgetapp.app.ui.theme.MockInk
import com.budgetapp.app.ui.theme.MockLavenderTint
import com.budgetapp.app.ui.theme.MockMaterialSurface
import com.budgetapp.app.ui.theme.MockNeutral
import com.budgetapp.app.ui.theme.MockPrimaryAlt
import com.budgetapp.app.ui.theme.MockPrimaryGlowAlt
import com.budgetapp.app.ui.theme.MockPrimaryWash
import com.budgetapp.app.ui.theme.MockSurface
import com.budgetapp.app.ui.theme.MockSurfaceBarely
import com.budgetapp.app.ui.theme.MockSurfaceLightAlt
import com.budgetapp.app.ui.theme.MockTextLightMuted
import com.budgetapp.app.ui.theme.MockTextSoftPurple
import com.budgetapp.app.ui.theme.MockWhite
import com.budgetapp.app.ui.theme.MockWhiteSoft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.runtime.collectAsState

@Composable
fun EnvelopeDetailScreen(
    envelopeId: Long,
    viewModel: EnvelopeDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onModalVisibleChange: (Boolean) -> Unit = {},
) {
    LaunchedEffect(envelopeId) {
        viewModel.load(envelopeId)
    }

    val state by viewModel.state.collectAsState()
    var editor by remember { mutableStateOf<TransactionEditorState?>(null) }
    var addEditor by remember { mutableStateOf<DetailAddTransactionEditorState?>(null) }
    var addDrawerHeightPx by remember { mutableFloatStateOf(900f) }
    LaunchedEffect(editor != null, addEditor != null) {
        onModalVisibleChange(editor != null || addEditor != null)
    }
    val openAddTransaction = {
        val currentEnvelopeId = state.envelope?.id ?: envelopeId
        addEditor = DetailAddTransactionEditorState(envelopeId = currentEnvelopeId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MockBackground),
    ) {
        EnvelopeDetailContent(
            state = state,
            onBack = onBack,
            onAddTransaction = openAddTransaction,
            onEditTransaction = { transaction ->
                editor = TransactionEditorState(
                    transaction = transaction,
                    envelopeId = transaction.envelopeId,
                    amountText = abs(transaction.amountCents).toDetailMajorString(),
                    date = transaction.postedAtMillis.toDetailLocalDate(),
                    note = transaction.note.orEmpty(),
                )
            },
        )
        if (addEditor != null) {
            BackHandler { addEditor = null }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MockInk.copy(alpha = 0.46f))
                    .clickable { addEditor = null }
                    .semantics { contentDescription = "Dismiss add transaction" },
            )
            DetailAddTransactionDrawer(
                editor = addEditor,
                envelopeName = state.envelope?.name ?: "Envelope",
                isGoal = state.isGoal,
                onEditorChange = { addEditor = it },
                onSave = { current ->
                    val submitted = current.copy(hasSubmitted = true)
                    addEditor = submitted
                    viewModel.addTransaction(submitted) { addEditor = null }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onGloballyPositioned { coordinates ->
                        addDrawerHeightPx = coordinates.size.height.toFloat().coerceAtLeast(1f)
                    }
                    .pointerInput(Unit) {
                        var dragDistance = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragDistance = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                if (dragAmount > 0f) {
                                    dragDistance += dragAmount
                                }
                            },
                            onDragEnd = {
                                if (dragDistance > addDrawerHeightPx * 0.2f) {
                                    addEditor = null
                                }
                            },
                        )
                    }
                    .clickable(onClick = {}),
            )
        }
        if (editor != null) {
            BackHandler { editor = null }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MockInk.copy(alpha = 0.46f))
                    .clickable { editor = null }
                    .semantics { contentDescription = "Dismiss transaction editor" },
            )
            TransactionEditorDrawer(
                editor = editor,
                envelopes = state.envelopes,
                onEditorChange = { editor = it },
                onSave = { current -> viewModel.saveTransaction(current) { editor = null } },
                onDelete = { id -> viewModel.deleteTransaction(id) { editor = null } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .pointerInput(Unit) {
                        var dragDistance = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragDistance = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                if (dragAmount > 0f) {
                                    dragDistance += dragAmount
                                }
                            },
                            onDragEnd = {
                                if (dragDistance > 280f) {
                                    editor = null
                                }
                            },
                        )
                    }
                    .clickable(onClick = {}),
            )
        }
    }
}
@Composable
internal fun EnvelopeDetailContent(
    state: EnvelopeDetailState,
    onBack: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
) {
    val envelope = state.envelope
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MockSurface)
                .padding(start = 24.dp, end = 24.dp, top = 76.dp, bottom = 35.dp),
        ) {
            DetailNavRow(
                title = envelope?.name ?: "Envelope",
                onBack = onBack,
                onAddTransaction = onAddTransaction,
                showAdd = envelope != null,
            )
            Spacer(modifier = Modifier.height(35.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EnvelopeCategoryIcon(
                    token = envelope?.iconToken.orEmpty(),
                    tint = MockPrimaryGlowAlt,
                    modifier = Modifier.size(21.dp),
                )
                Text(
                    text = "${envelope?.type?.replaceFirstChar { it.titlecase(Locale.US) } ?: "Variable"} envelope",
                    color = MockTextSoftPurple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.68.sp,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when {
                    state.isGoal && state.spentCents == 0L -> "${state.profileSettings.formatMoney(0)} saved"
                    state.isGoal -> "${state.profileSettings.formatMoney(state.remainingCents)} to go"
                    state.spentCents == 0L -> "${state.profileSettings.formatMoney(0)} spent"
                    else -> "${state.profileSettings.formatMoney(state.remainingCents)} left"
                },
                color = if (state.spentCents == 0L) MockTextLightMuted else MockPrimaryGlowAlt,
                fontSize = 41.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = if (state.isGoal) {
                    "${state.profileSettings.formatMoney(state.spentCents)} saved / ${state.profileSettings.formatMoney(state.ceilingCents)} goal"
                } else {
                    "${state.profileSettings.formatMoney(state.spentCents)} / ${state.profileSettings.formatMoney(state.ceilingCents)}"
                },
                color = MockTextLightMuted.copy(alpha = 0.58f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MockWhite.copy(alpha = 0.10f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.progress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (state.spentCents == 0L) MockTextLightMuted else MockPrimaryGlowAlt),
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MockBackground),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 26.dp,
                bottom = 164.dp,
            ),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "TRANSACTIONS",
                        color = MockMaterialSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.12.sp,
                    )
                    Text(
                        text = "${state.transactions.size} ${if (state.transactions.size == 1) "item" else "items"}",
                        color = MockNeutral,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
            if (state.transactions.isEmpty()) {
                item {
                    EmptyTransactions(
                        envelopeName = envelope?.name ?: "this envelope",
                        onAddTransaction = onAddTransaction,
                    )
                }
            } else {
                val grouped = state.transactions.groupBy { it.postedAtMillis.toDetailLocalDate() }
                grouped.forEach { (date, transactions) ->
                    item {
                        Text(
                            text = state.profileSettings.formatTransactionDate(date),
                            color = MockNeutral,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.96.sp,
                            modifier = Modifier.padding(start = 2.dp, top = 10.dp, bottom = 8.dp),
                        )
                    }
                    transactions.forEach { transaction ->
                        item {
                            TransactionCard(
                                transaction = transaction,
                                profileSettings = state.profileSettings,
                                onClick = { onEditTransaction(transaction) },
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DetailNavRow(
    title: String,
    onBack: () -> Unit,
    onAddTransaction: () -> Unit,
    showAdd: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                color = MockWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showAdd) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MockPrimaryAlt.copy(alpha = 0.12f))
                    .clickable(onClick = onAddTransaction)
                    .semantics { contentDescription = "Add transaction" },
                contentAlignment = Alignment.Center,
            ) {
                DetailIcon(DetailIconKind.Plus, MockPrimaryAlt, Modifier.size(21.dp))
            }
        }
    }
}

@Composable
internal fun EmptyTransactions(
    envelopeName: String,
    onAddTransaction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DetailIcon(DetailIconKind.Receipt, MockPrimaryAlt, Modifier.size(38.dp))
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "No transactions yet",
            color = MockMaterialSurface,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Transactions assigned to $envelopeName will appear here.",
            color = MockNeutral,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAddTransaction,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MockPrimaryAlt,
                contentColor = MockWhite,
            ),
        ) {
            Text("Add transaction", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun TransactionCard(
    transaction: TransactionEntity,
    profileSettings: ProfileSettings,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfaceLightAlt, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchantName ?: transaction.description,
                color = MockMaterialSurface,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${if (transaction.externalId == null) "Manual" else "Automatic"}${transaction.note?.let { " \u00b7 $it" } ?: ""}",
                color = MockNeutral,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = profileSettings.formatMoney(abs(transaction.amountCents)),
            color = MockMaterialSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun TransactionEditorDrawer(
    editor: TransactionEditorState?,
    envelopes: List<EnvelopeBalance>,
    onEditorChange: (TransactionEditorState) -> Unit,
    onSave: (TransactionEditorState) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (editor == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfaceLightAlt, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 5.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MockLavenderTint),
        )
        Spacer(modifier = Modifier.height(18.dp))
        if (editor.isDeleteConfirming) {
            DeleteTransactionConfirm(
                onCancel = { onEditorChange(editor.copy(isDeleteConfirming = false)) },
                onDelete = { onDelete(editor.transaction.id) },
            )
        } else {
            TransactionEditorForm(
                editor = editor,
                envelopes = envelopes,
                onEditorChange = onEditorChange,
                onSave = onSave,
            )
        }
    }
}

@Composable
internal fun DetailAddTransactionDrawer(
    editor: DetailAddTransactionEditorState?,
    envelopeName: String,
    isGoal: Boolean,
    onEditorChange: (DetailAddTransactionEditorState) -> Unit,
    onSave: (DetailAddTransactionEditorState) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (editor == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfaceLightAlt, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 5.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MockLavenderTint),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = if (isGoal) "Add contribution" else "Add transaction",
            color = MockMaterialSurface,
            fontSize = 23.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(19.dp))
        DetailField(if (isGoal) "Source" else "Merchant") {
            DetailTextInput(
                value = editor.merchant,
                onValueChange = { onEditorChange(editor.copy(merchant = it)) },
                placeholder = if (isGoal) "Contribution source" else "Merchant name",
                height = 54.dp,
                hasError = editor.merchantError,
            )
        }
        if (editor.merchantError) {
            DetailFieldError(if (isGoal) "Source is required" else "Merchant is required")
        }
        Spacer(modifier = Modifier.height(15.dp))
        DetailField("Amount") {
            DetailAmountInput(
                value = editor.amountText,
                onValueChange = { value ->
                    onEditorChange(editor.copy(amountText = value.filter { it.isDigit() || it == '.' || it == ',' }))
                },
                hasError = editor.amountError,
            )
        }
        if (editor.amountError) {
            DetailFieldError("Enter a positive amount")
        }
        Spacer(modifier = Modifier.height(15.dp))
        DetailField("Envelope") {
            DetailLockedValue(envelopeName)
        }
        Spacer(modifier = Modifier.height(15.dp))
        DetailField("Date") {
            DetailDateControl(
                date = editor.date,
                onDateChange = { onEditorChange(editor.copy(date = it)) },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (!editor.isNoteOpen) {
            Text(
                text = "+ Add note",
                color = MockPrimaryAlt,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditorChange(editor.copy(isNoteOpen = true)) }
                    .padding(vertical = 4.dp),
            )
        } else {
            DetailField("Note") {
                DetailTextInput(
                    value = editor.note,
                    onValueChange = { onEditorChange(editor.copy(note = it)) },
                    placeholder = "Optional note",
                    height = 54.dp,
                )
            }
        }
        Spacer(modifier = Modifier.height(19.dp))
        Button(
            onClick = { onSave(editor) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = editor.canSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = MockPrimaryAlt,
                contentColor = MockWhite,
                disabledContainerColor = MockLavenderTint,
                disabledContentColor = MockNeutral,
            ),
        ) {
            Text(if (isGoal) "Add contribution" else "Add transaction", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun TransactionEditorForm(
    editor: TransactionEditorState,
    envelopes: List<EnvelopeBalance>,
    onEditorChange: (TransactionEditorState) -> Unit,
    onSave: (TransactionEditorState) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Edit transaction",
            color = MockMaterialSurface,
            fontSize = 23.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(19.dp))
        DetailField("Move") {
            DetailEnvelopeSelector(
                envelopes = envelopes,
                selectedId = editor.envelopeId,
                onSelected = { onEditorChange(editor.copy(envelopeId = it)) },
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
        DetailField("Add note") {
            DetailTextInput(
                value = editor.note,
                onValueChange = { onEditorChange(editor.copy(note = it)) },
                placeholder = "Optional note",
                height = 74.dp,
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
        DetailField("Edit amount") {
            DetailAmountInput(
                value = editor.amountText,
                onValueChange = { value ->
                    onEditorChange(editor.copy(amountText = value.filter { it.isDigit() || it == '.' || it == ',' }))
                },
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
        DetailField("Edit date") {
            DetailDateControl(
                date = editor.date,
                onDateChange = { onEditorChange(editor.copy(date = it)) },
            )
        }
        Spacer(modifier = Modifier.height(19.dp))
        Button(
            onClick = { onSave(editor) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MockPrimaryAlt,
                contentColor = MockWhite,
            ),
        ) {
            Text("Save transaction", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Delete transaction",
            color = MockDanger,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onEditorChange(editor.copy(isDeleteConfirming = true)) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
internal fun DetailField(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(Locale.US),
            color = MockNeutral,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.96.sp,
        )
        Spacer(modifier = Modifier.height(7.dp))
        content()
    }
}

@Composable
internal fun DetailTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    height: androidx.compose.ui.unit.Dp,
    hasError: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = MockMaterialSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(MockWhiteSoft)
            .border(1.dp, if (hasError) MockDanger.copy(alpha = 0.62f) else MockSurfaceLightAlt, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isBlank()) {
                    Text(placeholder, color = MockNeutral, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                innerTextField()
            }
        },
    )
}

@Composable
internal fun DetailAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    hasError: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MockWhiteSoft)
            .border(1.dp, if (hasError) MockDanger.copy(alpha = 0.62f) else MockSurfaceLightAlt, RoundedCornerShape(16.dp)),
    ) {
        Text(
            text = "\u20ac",
            color = MockNeutral,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = MockMaterialSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 33.dp, end = 16.dp),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { innerTextField() }
            },
        )
    }
}

@Composable
internal fun DetailLockedValue(
    value: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MockWhiteSoft)
            .border(1.dp, MockSurfaceLightAlt, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = value,
            color = MockMaterialSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun DetailFieldError(
    text: String,
) {
    Text(
        text = text,
        color = MockDanger,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
    )
}

@Composable
internal fun DetailEnvelopeSelector(
    envelopes: List<EnvelopeBalance>,
    selectedId: Long?,
    onSelected: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MockWhiteSoft)
            .border(1.dp, MockSurfaceLightAlt, RoundedCornerShape(16.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        envelopes.take(4).forEach { envelope ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (envelope.id == selectedId) MockPrimaryWash else MockWhiteSoft)
                    .clickable { onSelected(envelope.id) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = envelope.name,
                    color = if (envelope.id == selectedId) MockPrimaryAlt else MockMaterialSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailDateControl(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
) {
    var isPickerOpen by remember { mutableStateOf(false) }
    val pickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = date.toDetailStartOfDayMillis(),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MockWhiteSoft)
            .border(1.dp, MockSurfaceLightAlt, RoundedCornerShape(16.dp))
            .clickable { isPickerOpen = true }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(date.toString(), color = MockMaterialSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = "Pick",
            color = MockPrimaryAlt,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
    if (isPickerOpen) {
        DatePickerDialog(
            onDismissRequest = { isPickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateChange(millis.toDetailLocalDate())
                        }
                        isPickerOpen = false
                    },
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { isPickerOpen = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
internal fun DeleteTransactionConfirm(
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Delete transaction?", color = MockMaterialSurface, fontSize = 23.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(9.dp))
        Text(
            text = "This transaction will be removed from the envelope.",
            color = MockNeutral,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(19.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MockSurfaceBarely, contentColor = MockMaterialSurface),
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MockMaterialSurface, contentColor = MockWhite),
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun DetailIcon(
    icon: DetailIconKind,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (icon) {
            DetailIconKind.Back -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = 2.5.dp.toPx())
                drawLine(tint, Offset(w * 0.42f, h * 0.22f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx())
                drawLine(tint, Offset(w * 0.42f, h * 0.78f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx())
            }
            DetailIconKind.Plus -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), strokeWidth = 2.5.dp.toPx())
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), strokeWidth = 2.5.dp.toPx())
            }
            DetailIconKind.Receipt -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.16f, h * 0.12f),
                    size = androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.76f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = stroke,
                )
                drawLine(tint, Offset(w * 0.3f, h * 0.36f), Offset(w * 0.7f, h * 0.36f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.3f, h * 0.56f), Offset(w * 0.58f, h * 0.56f), strokeWidth = 2.dp.toPx())
            }
        }
    }
}

internal enum class DetailIconKind {
    Back,
    Plus,
    Receipt,
}

internal fun Long.toDetailLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}

internal fun LocalDate.toDetailStartOfDayMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

internal fun String.toDetailCentsOrNull(): Long? {
    val value = trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (value <= 0.0) return null
    return (value * 100).roundToInt().toLong()
}

internal fun Long.toDetailMajorString(): String {
    return if (this % 100L == 0L) {
        "%d".format(Locale.US, this / 100L)
    } else {
        "%.2f".format(Locale.US, this / 100.0)
    }
}
