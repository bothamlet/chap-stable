package com.budgetapp.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.data.entity.TransactionEntity
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.data.model.EnvelopeType
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.domain.parseAmountMinorUnits
import com.budgetapp.app.domain.BudgetPeriod
import com.budgetapp.app.domain.budgetPeriod
import com.budgetapp.app.ui.components.EnvelopeCategoryIcon
import com.budgetapp.app.ui.components.StandardEmptyState
import com.budgetapp.app.ui.components.EnvelopePickerList
import com.budgetapp.app.ui.theme.MockBackground
import com.budgetapp.app.ui.theme.MockBorderLightAlt
import com.budgetapp.app.ui.theme.MockDanger
import com.budgetapp.app.ui.theme.MockInk
import com.budgetapp.app.ui.theme.MockLavenderPale
import com.budgetapp.app.ui.theme.MockMaterialSurface
import com.budgetapp.app.ui.theme.MockNeutral
import com.budgetapp.app.ui.theme.MockPrimaryAlt
import com.budgetapp.app.ui.theme.MockPrimaryDeep
import com.budgetapp.app.ui.theme.MockPrimaryGlowAlt
import com.budgetapp.app.ui.theme.MockPrimaryWash
import com.budgetapp.app.ui.theme.MockSurface
import com.budgetapp.app.ui.theme.MockSurfaceBarelyAlt
import com.budgetapp.app.ui.theme.MockSurfaceLightAlt
import com.budgetapp.app.ui.theme.MockTextSoftPurple
import com.budgetapp.app.ui.theme.MockWarningBright
import com.budgetapp.app.ui.theme.MockWhite
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.max
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import com.budgetapp.app.ui.theme.MockLavenderTint
import com.budgetapp.app.ui.theme.MockWhiteSoft
import com.budgetapp.app.ui.theme.MockSurfacePale
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.collectAsState

@Composable
fun EnvelopeListScreen(
    viewModel: EnvelopeListViewModel,
    onCreateEnvelope: () -> Unit,
    onOpenEnvelope: (Long) -> Unit,
    onOpenInbox: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    // The add-transaction overlay lives in the NavHost, which owns the shared editor state for
    // every entry point; this screen only renders the dashboard.
    DashboardScreen(
        state = state,
        onCreateEnvelope = onCreateEnvelope,
        onOpenEnvelope = onOpenEnvelope,
        onOpenInbox = onOpenInbox,
        onOpenMenu = onOpenMenu,
        onPreviousPeriod = viewModel::showPreviousPeriod,
        onNextPeriod = viewModel::showNextPeriod,
        onCurrentPeriod = viewModel::showCurrentPeriod,
        modifier = modifier,
    )
}

@Composable
internal fun DashboardScreen(
    state: DashboardState,
    onCreateEnvelope: () -> Unit,
    onOpenEnvelope: (Long) -> Unit,
    onOpenInbox: () -> Unit,
    onOpenMenu: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MockInk),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MockSurface),
        ) {
            if (state.envelopes.isEmpty()) {
                EmptyDashboard(
                    state = state,
                    onCreateEnvelope = onCreateEnvelope,
                    onPreviousPeriod = onPreviousPeriod,
                    onNextPeriod = onNextPeriod,
                    onCurrentPeriod = onCurrentPeriod,
                )
            } else {
                PopulatedDashboard(
                    state = state,
                    onOpenEnvelope = onOpenEnvelope,
                    onOpenInbox = onOpenInbox,
                    onPreviousPeriod = onPreviousPeriod,
                    onNextPeriod = onNextPeriod,
                    onCurrentPeriod = onCurrentPeriod,
                )
            }
        }
    }
}

@Composable
fun AddTransactionOverlay(
    editor: CashTransactionEditorState,
    envelopes: List<EnvelopeBalance>,
    profileSettings: ProfileSettings,
    onEditorChange: (CashTransactionEditorState) -> Unit,
    onDismiss: () -> Unit,
    onSave: (CashTransactionEditorState) -> Unit,
) {
    var drawerHeightPx by remember { mutableFloatStateOf(900f) }
    var isClosing by remember { mutableStateOf(false) }
    fun requestClose() {
        if (!isClosing) {
            isClosing = true
        }
    }
    BackHandler(onBack = ::requestClose)
    LaunchedEffect(isClosing) {
        if (isClosing) {
            delay(340)
            onDismiss()
        }
    }
    var isShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isShown = true
    }
    val drawerOffset by animateFloatAsState(
        targetValue = if (isClosing) drawerHeightPx + 80f else if (isShown) 0f else drawerHeightPx + 80f,
        animationSpec = tween(durationMillis = 340, easing = EaseOut),
        label = "addTransactionDrawerOffset",
    )
    var dragTarget by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val dragOffset by animateFloatAsState(
        targetValue = dragTarget,
        animationSpec = if (isDragging) {
            tween(durationMillis = 0)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "addTransactionDragOffset",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MockInk.copy(alpha = 0.46f))
                .clickable(onClick = ::requestClose)
                .semantics { contentDescription = "Dismiss add transaction" },
        )
        CashTransactionDrawer(
            editor = editor,
            envelopes = envelopes,
            profileSettings = profileSettings,
            onEditorChange = onEditorChange,
            onSave = onSave,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { coordinates ->
                    drawerHeightPx = coordinates.size.height.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(onDismiss) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onVerticalDrag = { _, dragAmount ->
                            dragTarget = max(0f, dragTarget + dragAmount)
                        },
                        onDragEnd = {
                            isDragging = false
                            if (dragTarget > drawerHeightPx * 0.2f) {
                                requestClose()
                            } else {
                                dragTarget = 0f
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            dragTarget = 0f
                        },
                    )
                }
                .clickable(onClick = {})
                .graphicsLayer { translationY = drawerOffset + dragOffset },
        )
    }
}

@Composable
internal fun EmptyDashboard(
    state: DashboardState,
    onCreateEnvelope: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            HeaderBlock(
                monthLabel = state.monthLabel,
                dayLine = state.dayLine,
                isCurrentPeriod = state.isCurrentPeriod,
                canGoBack = state.canGoBack,
                canGoForward = state.canGoForward,
                onPreviousPeriod = onPreviousPeriod,
                onNextPeriod = onNextPeriod,
                onCurrentPeriod = onCurrentPeriod,
                modifier = Modifier.padding(top = 92.dp),
            )
            EmptyContent(
                onCreateEnvelope = onCreateEnvelope,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 112.dp),
            )
        }
    }
}

@Composable
internal fun PopulatedDashboard(
    state: DashboardState,
    onOpenEnvelope: (Long) -> Unit,
    onOpenInbox: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MockBackground),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MockSurface)
                    .padding(start = 24.dp, end = 24.dp, top = 92.dp, bottom = 54.dp),
            ) {
                HeaderBlock(
                    monthLabel = state.monthLabel,
                    dayLine = state.dayLine,
                    isCurrentPeriod = state.isCurrentPeriod,
                    canGoBack = state.canGoBack,
                    canGoForward = state.canGoForward,
                    onPreviousPeriod = onPreviousPeriod,
                    onNextPeriod = onNextPeriod,
                    onCurrentPeriod = onCurrentPeriod,
                )
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 164.dp),
            ) {
                SummaryCard(
                    envelopes = state.spendingEnvelopes,
                    transactions = state.transactions,
                    profileSettings = state.profileSettings,
                )
                if (state.inboxCount > 0) {
                    InboxPill(
                        count = state.inboxCount,
                        onClick = onOpenInbox,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                if (state.variableEnvelopes.isNotEmpty()) {
                    EnvelopeSection(
                        title = "Variable",
                        envelopes = state.variableEnvelopes,
                        profileSettings = state.profileSettings,
                        onOpenEnvelope = onOpenEnvelope,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
                if (state.fixedEnvelopes.isNotEmpty()) {
                    EnvelopeSection(
                        title = "Fixed",
                        envelopes = state.fixedEnvelopes,
                        profileSettings = state.profileSettings,
                        onOpenEnvelope = onOpenEnvelope,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
                if (state.goalEnvelopes.isNotEmpty()) {
                    GoalSection(
                        envelopes = state.goalEnvelopes,
                        profileSettings = state.profileSettings,
                        onOpenEnvelope = onOpenEnvelope,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun HeaderBlock(
    monthLabel: String,
    dayLine: String,
    isCurrentPeriod: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = monthLabel.uppercase(Locale.US),
                color = MockTextSoftPurple,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
            )
            Spacer(modifier = Modifier.width(6.dp))
            PeriodArrow(
                label = "\u2039",
                description = "Previous period",
                enabled = canGoBack,
                onClick = onPreviousPeriod,
            )
            PeriodArrow(
                label = "\u203a",
                description = "Next period",
                enabled = canGoForward,
                onClick = onNextPeriod,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = dayLine,
            color = MockWhite,
            fontSize = 36.sp,
            lineHeight = 39.sp,
            fontWeight = FontWeight.Bold,
        )
        if (!isCurrentPeriod) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Back to this period",
                color = MockPrimaryGlowAlt,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(onClick = onCurrentPeriod)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun PeriodArrow(
    label: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(99.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) MockPrimaryGlowAlt else MockTextSoftPurple.copy(alpha = 0.4f),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun EmptyContent(
    onCreateEnvelope: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardEmptyState(
        title = "No envelopes yet",
        copy = "Create envelopes to start budgeting this month.",
        actionText = "Create envelope",
        onAction = onCreateEnvelope,
        modifier = modifier,
        icon = {
            DashboardIconCanvas(
                icon = DashboardIcon.Book,
                modifier = Modifier.size(52.dp),
                tint = MockPrimaryGlowAlt,
            )
        },
    )
}

@Composable
internal fun SummaryCard(
    envelopes: List<EnvelopeBalance>,
    transactions: List<TransactionEntity>,
    profileSettings: ProfileSettings,
    modifier: Modifier = Modifier,
) {
    val budget = envelopes.sumOf { it.ceilingCents }
    val spent = envelopes.sumOf { it.spentCents }
    val remaining = budget - spent
    val period = profileSettings.budgetPeriod()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfacePale, RoundedCornerShape(22.dp))
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "${profileSettings.formatMoney(budget)} budget",
                color = MockNeutral,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${profileSettings.formatMoney(spent)} spent",
                    color = MockNeutral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = profileSettings.formatMoney(remaining),
                    color = MockPrimaryAlt,
                    fontSize = 32.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "remaining",
                    color = MockNeutral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        BurnRateChart(
            transactions = transactions,
            budgetCents = budget,
            period = period,
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
                .padding(top = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            chartTickLabels(period).forEach {
                Text(
                    text = it,
                    color = MockNeutral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun BurnRateChart(
    transactions: List<TransactionEntity>,
    budgetCents: Long,
    period: BudgetPeriod,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier.semantics { contentDescription = "Spending forecast" }) {
        val gridColor = MockSurfaceBarelyAlt
        val actualColor = MockPrimaryAlt
        val forecastColor = MockLavenderPale
        val actualStroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        val forecastStroke = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 8.dp.toPx())),
        )
        val width = size.width
        val height = size.height

        listOf(0.19f, 0.49f, 0.8f).forEach { y ->
            drawLine(
                color = gridColor,
                start = Offset(0f, height * y),
                end = Offset(width, height * y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val expenses = transactions.filter { transaction ->
            transaction.amountCents < 0 && transaction.envelopeId != null &&
                transaction.postedAtMillis.toDashboardDate().let { it >= period.start && it <= period.end }
        }
        val spent = expenses.sumOf { -it.amountCents }
        val maxValue = max(budgetCents, spent).coerceAtLeast(1L).toFloat()
        val todayIndex = (LocalDate.now().toEpochDay() - period.start.toEpochDay() + 1).toInt().coerceIn(1, period.lengthInDays)
        val byDay = expenses.groupBy { (it.postedAtMillis.toDashboardDate().toEpochDay() - period.start.toEpochDay() + 1).toInt() }
        var running = 0L
        val points = (1..todayIndex).map { day ->
            running += byDay[day]?.sumOf { -it.amountCents } ?: 0L
            Offset(
                x = width * (day - 1) / (period.lengthInDays - 1).coerceAtLeast(1),
                y = height - (height * 0.88f * (running / maxValue)) - height * 0.06f,
            )
        }

        if (points.size > 1) {
            val actual = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(actual, actualColor, style = actualStroke)
            val last = points.last()
            val forecastEndY = height - (height * 0.88f * ((running.toFloat() / todayIndex) * period.lengthInDays / maxValue)) - height * 0.06f
            val forecast = Path().apply {
                moveTo(last.x, last.y)
                lineTo(width, forecastEndY.coerceIn(height * 0.06f, height * 0.94f))
            }
            drawPath(forecast, forecastColor, style = forecastStroke)
            drawCircle(
                color = MockWhite,
                radius = 6.5.dp.toPx(),
                center = last,
            )
            drawCircle(
                color = MockPrimaryAlt,
                radius = 5.dp.toPx(),
                center = last,
            )
            val label = textMeasurer.measure(
                AnnotatedString("forecast"),
                style = TextStyle(color = MockNeutral, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            )
            drawText(
                textLayoutResult = label,
                topLeft = Offset(
                    x = (last.x + 10.dp.toPx()).coerceAtMost(width - label.size.width),
                    y = (last.y - label.size.height - 18.dp.toPx()).coerceAtLeast(0f),
                ),
            )
        }
    }
}

@Composable
internal fun InboxPill(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MockPrimaryWash)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = "Review transactions" },
    ) {
        Text(
            text = "\u2726 $count transactions to review \u2192",
            color = MockPrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun CashTransactionDrawer(
    editor: CashTransactionEditorState,
    envelopes: List<EnvelopeBalance>,
    profileSettings: ProfileSettings,
    onEditorChange: (CashTransactionEditorState) -> Unit,
    onSave: (CashTransactionEditorState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedEnvelope = envelopes.firstOrNull { it.id == editor.envelopeId }
    val isGoalContribution = selectedEnvelope?.type == EnvelopeType.Goal
    // A contribution already carries its direction, so the Expense/Income toggle only belongs on
    // the plain spend sheet.
    val isGoal = isGoalContribution && !editor.isIncome
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfaceLightAlt, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
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
        )
        Spacer(modifier = Modifier.height(19.dp))
        CashField(
            label = if (isGoal) "Source" else "Merchant",
            error = if (editor.isMerchantInvalid) {
                if (isGoal) "Source is required." else "Merchant is required."
            } else {
                null
            },
        ) {
            CashTextInput(
                value = editor.merchant,
                onValueChange = { onEditorChange(editor.copy(merchant = it)) },
                placeholder = if (isGoal) "Contribution source" else "Merchant name",
                isError = editor.isMerchantInvalid,
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
        CashField(
            label = "Amount",
            error = if (editor.isAmountInvalid) "Amount is required." else null,
        ) {
            CashAmountInput(
                value = editor.amountText,
                currencySymbol = profileSettings.currencySymbol(),
                onValueChange = { value ->
                    onEditorChange(editor.copy(amountText = value.filter { it.isDigit() || it == '.' || it == ',' }))
                },
                isError = editor.isAmountInvalid,
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
        if (!isGoalContribution) {
            CashKindToggle(
                isIncome = editor.isIncome,
                onSelected = { isIncome -> onEditorChange(editor.copy(isIncome = isIncome)) },
            )
            Spacer(modifier = Modifier.height(15.dp))
        }
        if (editor.isIncome) {
            Text(
                text = "Money coming in waits in the inbox until you save it to a goal envelope.",
                color = MockNeutral,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            CashField(label = "Envelope") {
                EnvelopeSelector(
                    envelopes = envelopes,
                    selectedId = editor.envelopeId,
                    onSelected = { onEditorChange(editor.copy(envelopeId = it)) },
                )
            }
        }
        Spacer(modifier = Modifier.height(15.dp))
        CashField(label = "Date") {
            CashDateControl(
                date = editor.date,
                profileSettings = profileSettings,
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
                    .clickable { onEditorChange(editor.copy(isNoteOpen = true)) }
                    .padding(vertical = 4.dp),
            )
        } else {
            CashField(label = "Note") {
                CashTextInput(
                    value = editor.note,
                    onValueChange = { onEditorChange(editor.copy(note = it)) },
                    placeholder = "Optional note",
                    isError = false,
                    minHeight = 54.dp,
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = { onSave(editor) },
            enabled = editor.canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MockPrimaryAlt,
                contentColor = MockWhite,
                disabledContainerColor = MockLavenderTint,
                disabledContentColor = MockNeutral,
            ),
        ) {
            Text(
                text = if (isGoal) "Add contribution" else "Add transaction",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CashKindToggle(
    isIncome: Boolean,
    onSelected: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Expense" to false, "Income" to true).forEach { (label, value) ->
            val isSelected = value == isIncome
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) MockPrimaryWash else MockWhiteSoft)
                    .border(
                        1.dp,
                        if (isSelected) MockPrimaryAlt else MockSurfaceLightAlt,
                        RoundedCornerShape(14.dp),
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelected(value) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSelected) MockPrimaryDeep else MockMaterialSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun CashField(
    label: String,
    error: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(Locale.US),
            color = if (error == null) MockNeutral else MockDanger,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.96.sp,
        )
        Spacer(modifier = Modifier.height(7.dp))
        content()
        if (error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error,
                color = MockDanger,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun CashTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    minHeight: androidx.compose.ui.unit.Dp = 54.dp,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = MockMaterialSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        singleLine = minHeight <= 54.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(minHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(MockWhiteSoft)
            .border(
                1.dp,
                if (isError) MockDanger else MockSurfaceLightAlt,
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 16.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        color = MockNeutral,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
internal fun CashAmountInput(
    value: String,
    currencySymbol: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MockWhiteSoft)
            .border(1.dp, if (isError) MockDanger else MockSurfaceLightAlt, RoundedCornerShape(16.dp)),
    ) {
        Text(
            text = currencySymbol,
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isBlank()) {
                        Text(
                            text = "0.00",
                            color = MockNeutral,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
internal fun EnvelopeSelector(
    envelopes: List<EnvelopeBalance>,
    selectedId: Long?,
    onSelected: (Long) -> Unit,
) {
    EnvelopePickerList(
        envelopes = envelopes,
        selectedId = selectedId,
        onSelected = onSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashDateControl(
    date: LocalDate,
    profileSettings: ProfileSettings,
    onDateChange: (LocalDate) -> Unit,
) {
    var isPickerOpen by remember { mutableStateOf(false) }
    val pickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = date.toStartOfDayMillis(),
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
        Text(
            text = profileSettings.formatDate(date),
            color = MockMaterialSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
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
                            onDateChange(millis.toLocalDate())
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
internal fun EnvelopeSection(
    title: String,
    envelopes: List<EnvelopeBalance>,
    profileSettings: ProfileSettings,
    onOpenEnvelope: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val remaining = envelopes.sumOf { it.ceilingCents - it.spentCents }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = MockNeutral,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            Text(
                text = "${profileSettings.formatMoney(remaining)} remaining",
                color = if (remaining < 0) MockDanger else MockNeutral,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        // Rows are laid out by hand instead of a LazyVerticalGrid: the grid needed a magic
        // height to survive being nested in the screen's LazyColumn, which clipped the last row
        // and cut card text at larger font scales.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            envelopes.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    row.forEach { envelope ->
                        EnvelopeCard(
                            envelope = envelope,
                            profileSettings = profileSettings,
                            onClick = { onOpenEnvelope(envelope.id) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                    repeat(2 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun EnvelopeCard(
    envelope: EnvelopeBalance,
    profileSettings: ProfileSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val remaining = envelope.ceilingCents - envelope.spentCents
    val progress = if (envelope.ceilingCents <= 0) {
        0f
    } else {
        (envelope.spentCents.toFloat() / envelope.ceilingCents.toFloat()).coerceIn(0f, 1f)
    }
    val statusColor = when {
        remaining < 0 -> MockDanger
        envelope.spentCents == 0L -> MockNeutral
        progress >= 0.75f -> MockWarningBright
        else -> MockPrimaryGlowAlt
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfacePale, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        EnvelopeCategoryIcon(
            token = envelope.iconToken,
            modifier = Modifier.size(24.dp),
            tint = MockPrimaryGlowAlt,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = envelope.name,
            color = MockInk,
            fontSize = 17.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = statusText(remaining, envelope.spentCents, profileSettings),
            color = statusColor,
            fontSize = 20.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${profileSettings.formatMoney(envelope.spentCents)} / ${profileSettings.formatMoney(envelope.ceilingCents)}",
            color = MockBorderLightAlt,
            fontSize = 11.sp,
            lineHeight = 13.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MockSurfaceLightAlt),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(7.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(statusColor),
            )
        }
    }
}

@Composable
internal fun GoalSection(
    envelopes: List<EnvelopeBalance>,
    profileSettings: ProfileSettings,
    onOpenEnvelope: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Goals",
            color = MockNeutral,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            envelopes.forEach { envelope ->
                GoalCard(
                    envelope = envelope,
                    profileSettings = profileSettings,
                    onClick = { onOpenEnvelope(envelope.id) },
                )
            }
        }
    }
}

@Composable
internal fun GoalCard(
    envelope: EnvelopeBalance,
    profileSettings: ProfileSettings,
    onClick: () -> Unit,
) {
    val progress = if (envelope.ceilingCents <= 0L) {
        0f
    } else {
        (envelope.spentCents.toFloat() / envelope.ceilingCents.toFloat()).coerceIn(0f, 1f)
    }
    val statusColor = if (envelope.spentCents == 0L) MockNeutral else MockPrimaryGlowAlt

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfaceLightAlt, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EnvelopeCategoryIcon(
                    token = envelope.iconToken,
                    modifier = Modifier.size(22.dp),
                    tint = MockPrimaryGlowAlt,
                )
                Text(
                    text = envelope.name,
                    color = MockInk,
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "${profileSettings.formatMoney(envelope.spentCents)} of ${profileSettings.formatMoney(envelope.ceilingCents)} saved",
                color = MockNeutral,
                fontSize = 13.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MockSurfaceLightAlt),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(7.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(statusColor),
            )
        }
    }
}

@Composable
internal fun DashboardIconCanvas(
    icon: DashboardIcon,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = MockWhite,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (icon) {
            DashboardIcon.Home -> {
                val roof = Path().apply {
                    moveTo(w * 0.14f, h * 0.48f)
                    lineTo(w * 0.5f, h * 0.16f)
                    lineTo(w * 0.86f, h * 0.48f)
                }
                drawPath(roof, tint, style = stroke)
                drawLine(tint, Offset(w * 0.24f, h * 0.44f), Offset(w * 0.24f, h * 0.84f), strokeWidth = 2.2.dp.toPx())
                drawLine(tint, Offset(w * 0.76f, h * 0.44f), Offset(w * 0.76f, h * 0.84f), strokeWidth = 2.2.dp.toPx())
                drawLine(tint, Offset(w * 0.24f, h * 0.84f), Offset(w * 0.76f, h * 0.84f), strokeWidth = 2.2.dp.toPx())
                drawLine(tint, Offset(w * 0.42f, h * 0.84f), Offset(w * 0.42f, h * 0.62f), strokeWidth = 2.2.dp.toPx())
                drawLine(tint, Offset(w * 0.58f, h * 0.84f), Offset(w * 0.58f, h * 0.62f), strokeWidth = 2.2.dp.toPx())
                drawLine(tint, Offset(w * 0.42f, h * 0.62f), Offset(w * 0.58f, h * 0.62f), strokeWidth = 2.2.dp.toPx())
            }
            DashboardIcon.Profile -> {
                drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.32f), style = stroke)
                drawArc(
                    color = tint,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.55f),
                    size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.54f),
                    style = stroke,
                )
            }
            DashboardIcon.Plus -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.82f), strokeWidth = 2.4.dp.toPx())
                drawLine(tint, Offset(w * 0.18f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = 2.4.dp.toPx())
            }
            DashboardIcon.Book -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.22f, h * 0.08f),
                    size = androidx.compose.ui.geometry.Size(w * 0.58f, h * 0.84f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = stroke,
                )
                drawLine(tint, Offset(w * 0.34f, h * 0.34f), Offset(w * 0.66f, h * 0.34f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.34f, h * 0.52f), Offset(w * 0.58f, h * 0.52f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.22f, h * 0.78f), Offset(w * 0.8f, h * 0.78f), strokeWidth = 2.dp.toPx())
            }
            DashboardIcon.Envelope -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = stroke,
                )
                drawLine(tint, Offset(w * 0.14f, h * 0.26f), Offset(w * 0.5f, h * 0.55f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.86f, h * 0.26f), Offset(w * 0.5f, h * 0.55f), strokeWidth = 2.dp.toPx())
            }
            DashboardIcon.Menu -> {
                drawLine(tint, Offset(w * 0.18f, h * 0.28f), Offset(w * 0.82f, h * 0.28f), strokeWidth = 2.4.dp.toPx())
                drawLine(tint, Offset(w * 0.18f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = 2.4.dp.toPx())
                drawLine(tint, Offset(w * 0.18f, h * 0.72f), Offset(w * 0.82f, h * 0.72f), strokeWidth = 2.4.dp.toPx())
            }
        }
    }
}

internal enum class DashboardIcon {
    Home,
    Profile,
    Plus,
    Book,
    Envelope,
    Menu,
}

internal fun statusText(
    remainingCents: Long,
    spentCents: Long,
    profileSettings: ProfileSettings,
): String {
    return when {
        remainingCents < 0 -> profileSettings.remainingLabel(remainingCents)
        spentCents == 0L -> "${profileSettings.formatMoney(0)} spent"
        else -> profileSettings.remainingLabel(remainingCents)
    }
}

internal fun String.toCashCentsOrNull(): Long? = parseAmountMinorUnits(this)?.takeIf { it > 0L }

internal fun Long.toDashboardDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}

internal fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}

internal fun chartTickLabels(period: BudgetPeriod): List<String> {
    val positions = listOf(0, period.lengthInDays / 3, (period.lengthInDays * 2) / 3, period.lengthInDays - 1)
        .distinct()
    return positions.map { offset -> period.start.plusDays(offset.toLong()).dayOfMonth.toString() }
}

internal fun LocalDate.toStartOfDayMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
