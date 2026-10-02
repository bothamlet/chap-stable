package com.budgetapp.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.data.entity.TransactionEntity
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.domain.BudgetPeriod
import com.budgetapp.app.domain.budgetPeriod
import com.budgetapp.app.ui.components.CompactHeaderTopPadding
import com.budgetapp.app.ui.components.StandardEmptyState
import com.budgetapp.app.ui.components.StandardEmptyStateViewport
import com.budgetapp.app.ui.theme.MockAccentDeep
import com.budgetapp.app.ui.theme.MockBorderDeep
import com.budgetapp.app.ui.theme.MockDanger
import com.budgetapp.app.ui.theme.MockInk
import com.budgetapp.app.ui.theme.MockMaterialSurface
import com.budgetapp.app.ui.theme.MockNeutral
import com.budgetapp.app.ui.theme.MockPanel
import com.budgetapp.app.ui.theme.MockPrimaryAlt
import com.budgetapp.app.ui.theme.MockPrimaryLight
import com.budgetapp.app.ui.theme.MockSurface
import com.budgetapp.app.ui.theme.MockSurfaceDeep
import com.budgetapp.app.ui.theme.MockSurfaceMuted
import com.budgetapp.app.ui.theme.MockSurfaceRaisedAlt
import com.budgetapp.app.ui.theme.MockTextLightMuted
import com.budgetapp.app.ui.theme.MockTextSecondary
import com.budgetapp.app.ui.theme.MockTextTertiary
import com.budgetapp.app.ui.theme.MockWarningBright
import com.budgetapp.app.ui.theme.MockWhite
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.time.format.TextStyle
import java.time.YearMonth
import java.time.DayOfWeek
import kotlin.math.roundToInt
import kotlin.math.max

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    if (state.hasInsights) {
        AnalyticsFullContent(state = state, onBack = onBack, modifier = modifier)
    } else {
        AnalyticsEmptyContent(onBack = onBack, onImport = onImport, modifier = modifier)
    }
}

@Composable
private fun AnalyticsEmptyContent(
    onBack: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MockSurface),
    ) {
        AnalyticsTopBar(
            title = "Analytics",
            onBack = onBack,
            centered = false,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        StandardEmptyStateViewport(
            modifier = Modifier.align(Alignment.Center),
        ) {
            StandardEmptyState(
                title = "No insights yet",
                copy = "Import transactions to see spending analytics.",
                actionText = "Import transactions",
                onAction = onImport,
                icon = {
                    AnalyticsIcon(AnalyticsIconKind.Bars, Modifier.size(44.dp), MockPrimaryAlt)
                },
            )
        }
    }
}

@Composable
private fun AnalyticsFullContent(
    state: AnalyticsState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MockSurface),
    ) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = CompactHeaderTopPadding),
        ) {
            AnalyticsTopRow(onBack)
            Spacer(modifier = Modifier.height(22.dp))
            PeriodTabs()
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 36.dp,
                bottom = 164.dp,
            ),
        ) {
            item {
                AnalyticsSectionTitle("Highlights")
                HighlightGrid(state)
                Spacer(modifier = Modifier.height(36.dp))
            }
            item {
                AnalyticsSectionTitle("Spending trend")
                TrendPanel(state)
                Spacer(modifier = Modifier.height(12.dp))
                Legend()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Forecast estimates where spending lands if the current pace continues.",
                    color = MockTextTertiary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(36.dp))
            }
            item {
                AnalyticsSectionTitle("Envelope breakdown")
                BreakdownPanel(state.spendingEnvelopes, state.profileSettings)
                Spacer(modifier = Modifier.height(36.dp))
            }
            item {
                AnalyticsSectionTitle("Month comparison")
                ComparisonPanel(state)
                Spacer(modifier = Modifier.height(36.dp))
            }
            item {
                AnalyticsSectionTitle("Spending by day")
                HeatmapPanel(state.periodExpenses, state.profileSettings)
            }
        }
    }
}

@Composable
private fun AnalyticsTopBar(
    title: String,
    onBack: () -> Unit,
    centered: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = CompactHeaderTopPadding, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MockWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
    }
}

@Composable
private fun AnalyticsTopRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Analytics",
            color = MockWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PeriodTabs() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.dp, MockBorderDeep)
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf("This Month", "3 Months", "Year").forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(41.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (index == 0) MockSurfaceMuted else MockSurface)
                    .border(1.dp, if (index == 0) MockBorderDeep else MockSurface, RoundedCornerShape(99.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (index == 0) MockWhite else MockTextTertiary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AnalyticsSectionTitle(title: String) {
    Text(
        text = title.uppercase(Locale.US),
        color = MockTextTertiary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.43.sp,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

@Composable
private fun HighlightGrid(state: AnalyticsState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Total spent", state.profileSettings.formatMoney(state.spentCents), "${state.budgetPercent}% of budget", MockPrimaryAlt, Modifier.weight(1f))
            MetricCard("Forecast", state.profileSettings.formatMoney(state.forecastCents), forecastNote(state), MockWarningBright, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Most active", state.mostActiveDay, "Spending peak", MockWhite, Modifier.weight(1f))
            MetricCard("Top envelope", state.topEnvelope?.name ?: "None", topEnvelopeNote(state.topEnvelope, state.profileSettings), if ((state.topEnvelope?.spentCents ?: 0L) > (state.topEnvelope?.ceilingCents ?: Long.MAX_VALUE)) MockDanger else MockWhite, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    note: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MockSurfaceRaisedAlt)
            .border(1.dp, MockBorderDeep, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(label.uppercase(Locale.US), color = MockTextTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.96.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, color = valueColor, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(3.dp))
        Text(note, color = MockTextTertiary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TrendPanel(state: AnalyticsState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MockSurfaceRaisedAlt)
            .border(1.dp, MockBorderDeep, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text("${state.profileSettings.formatMoney(state.budgetCents)} budget", color = MockTextTertiary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End,
        ) {
            Text(state.profileSettings.formatMoney(state.spentCents), color = MockPrimaryAlt, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("spent so far", color = MockTextTertiary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        TrendChart(
            transactions = state.periodExpenses,
            budgetCents = state.budgetCents,
            period = state.period,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
        )
    }
}

@Composable
private fun TrendChart(
    transactions: List<TransactionEntity>,
    budgetCents: Long,
    period: BudgetPeriod,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val maxValue = max(budgetCents.toFloat(), transactions.sumOf { -it.amountCents }.toFloat()).coerceAtLeast(1f)
        val width = size.width
        val height = size.height
        repeat(4) { index ->
            val y = height * (index + 1) / 5f
            drawLine(MockBorderDeep, Offset(0f, y), Offset(width, y), strokeWidth = 1.dp.toPx())
        }
        val days = period.lengthInDays.coerceAtLeast(1)
        val byDay = transactions.groupBy { (it.postedAtMillis.toAnalyticsDate().toEpochDay() - period.start.toEpochDay() + 1).toInt() }
        var running = 0L
        val todayIndex = (LocalDate.now().toEpochDay() - period.start.toEpochDay() + 1).toInt().coerceIn(1, days)
        val points = (1..todayIndex).map { day ->
            running += byDay[day]?.sumOf { -it.amountCents } ?: 0L
            Offset(
                x = width * (day - 1) / (days - 1).coerceAtLeast(1),
                y = height - (height * (running / maxValue)),
            )
        }
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, MockPrimaryAlt, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            val last = points.last()
            drawCircle(MockPrimaryAlt, radius = 7.dp.toPx(), center = last)
            drawLine(MockPrimaryAlt.copy(alpha = 0.45f), Offset(last.x, 0f), Offset(last.x, height), strokeWidth = 1.dp.toPx())
        }
        val budgetY = height - (height * (budgetCents / maxValue))
        drawLine(MockTextLightMuted.copy(alpha = 0.55f), Offset(0f, budgetY), Offset(width, budgetY), strokeWidth = 1.2.dp.toPx())
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem("Actual", MockPrimaryAlt)
        LegendItem("Forecast", MockWarningBright)
    }
}

@Composable
private fun LegendItem(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(width = 20.dp, height = 3.dp).clip(RoundedCornerShape(99.dp)).background(color))
        Text(label, color = MockTextTertiary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BreakdownPanel(envelopes: List<EnvelopeBalance>, profileSettings: ProfileSettings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MockSurfaceRaisedAlt)
            .border(1.dp, MockBorderDeep, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        envelopes.take(6).forEach { envelope ->
            val progress = if (envelope.ceilingCents <= 0L) 0f else (envelope.spentCents.toFloat() / envelope.ceilingCents).coerceAtLeast(0f)
            val color = when {
                progress >= 1f -> MockDanger
                progress >= 0.7f -> MockWarningBright
                else -> MockPrimaryAlt
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(envelope.name, color = MockWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.36f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(modifier = Modifier.weight(0.38f).height(4.dp).clip(RoundedCornerShape(99.dp)).background(MockBorderDeep)) {
                    Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(color))
                }
                Text("${profileSettings.formatMoney(envelope.spentCents)}/${profileSettings.formatMoney(envelope.ceilingCents)}", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.26f), maxLines = 1)
            }
        }
    }
}

@Composable
private fun ComparisonPanel(state: AnalyticsState) {
    val monthSpending = state.monthComparison()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MockSurfaceRaisedAlt)
            .border(1.dp, MockBorderDeep, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text("budget", color = MockTextTertiary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.TopEnd))
        Canvas(modifier = Modifier.fillMaxSize().padding(top = 18.dp)) {
            val bars = monthSpending.map { it.spentCents.toFloat() }
            val maxValue = max(state.budgetCents.toFloat(), bars.maxOrNull() ?: 1f).coerceAtLeast(1f)
            val barWidth = size.width / 12f
            bars.forEachIndexed { index, value ->
                val x = size.width * (index + 0.8f) / 7f
                val barHeight = size.height * 0.72f * (value / maxValue)
                val color = if (value > state.budgetCents) {
                    MockDanger
                } else if (monthSpending[index].isCurrent) {
                    MockWarningBright
                } else {
                    MockPrimaryAlt.copy(alpha = 0.65f)
                }
                drawRoundRect(color, Offset(x, size.height * 0.82f - barHeight), androidx.compose.ui.geometry.Size(barWidth, barHeight), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx()))
            }
            val budgetY = size.height * 0.82f - size.height * 0.72f * (state.budgetCents / maxValue)
            drawLine(MockTextLightMuted.copy(alpha = 0.55f), Offset(0f, budgetY), Offset(size.width, budgetY), strokeWidth = 1.2.dp.toPx())
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            monthSpending.forEach { item ->
                Text(
                    text = item.label,
                    color = if (item.isCurrent) MockWarningBright else MockTextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeatmapPanel(transactions: List<TransactionEntity>, profileSettings: ProfileSettings) {
    val period = profileSettings.budgetPeriod()
    val amounts = transactions.groupBy { (it.postedAtMillis.toAnalyticsDate().toEpochDay() - period.start.toEpochDay() + 1).toInt() }.mapValues { (_, txs) -> txs.sumOf { -it.amountCents } }
    val maxAmount = amounts.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val weekdays = orderedWeekdays(profileSettings.firstDay())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MockSurfaceRaisedAlt)
            .border(1.dp, MockBorderDeep, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach {
                Text(it.getDisplayName(TextStyle.SHORT, Locale.US).uppercase(Locale.US), color = MockTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val leadingBlanks = ((period.start.dayOfWeek.value - profileSettings.firstDay().value) + 7) % 7
        val cells = List(leadingBlanks) { 0 } + (1..period.lengthInDays).toList()
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == 0) {
                        Spacer(modifier = Modifier.weight(1f).height(42.dp))
                    } else {
                        val amount = amounts[day] ?: 0L
                        val ratio = amount.toFloat() / maxAmount
                        val color = when {
                            amount == 0L -> MockAccentDeep.copy(alpha = 0.38f)
                            ratio > 0.66f -> MockPrimaryAlt
                            ratio > 0.33f -> MockAccentDeep
                            else -> MockPanel
                        }
                        Box(
                            modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(9.dp)).background(color),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(period.start.plusDays((day - 1).toLong()).dayOfMonth.toString(), color = if (ratio > 0.66f) MockWhite else MockTextLightMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f).height(42.dp)) }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
        Text("SPENDING LEVEL", color = MockTextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.88.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun AnalyticsIcon(
    kind: AnalyticsIconKind,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (kind) {
            AnalyticsIconKind.Back -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.22f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.78f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            }
            AnalyticsIconKind.Bars -> {
                drawLine(tint, Offset(w * 0.26f, h * 0.82f), Offset(w * 0.26f, h * 0.56f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.82f), Offset(w * 0.5f, h * 0.2f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.74f, h * 0.82f), Offset(w * 0.74f, h * 0.42f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

private enum class AnalyticsIconKind {
    Back,
    Bars,
}

private data class MonthSpend(
    val label: String,
    val spentCents: Long,
    val isCurrent: Boolean,
)

private fun AnalyticsState.monthComparison(): List<MonthSpend> {
    val currentPeriod = profileSettings.budgetPeriod()
    val byPeriodStart = assignedExpenses.groupBy { profileSettings.budgetPeriod(it.postedAtMillis.toAnalyticsDate()).start }
    return (5 downTo 0).map { offset ->
        val startMonth = YearMonth.from(currentPeriod.start).minusMonths(offset.toLong())
        val start = LocalDate.of(
            startMonth.year,
            startMonth.month,
            currentPeriod.start.dayOfMonth.coerceAtMost(startMonth.lengthOfMonth()),
        )
        MonthSpend(
            label = if (profileSettings.monthStartDay == "15th of the month") profileSettings.formatDate(start) else start.month.getDisplayName(TextStyle.SHORT, Locale.US),
            spentCents = byPeriodStart[start]?.sumOf { -it.amountCents } ?: 0L,
            isCurrent = start == currentPeriod.start,
        )
    }
}

private fun Long.toAnalyticsDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun forecast(spentCents: Long, period: BudgetPeriod): Long {
    val today = (LocalDate.now().toEpochDay() - period.start.toEpochDay() + 1).toInt().coerceIn(1, period.lengthInDays)
    return ((spentCents.toDouble() / today) * period.lengthInDays).roundToInt().toLong()
}

private fun forecastNote(state: AnalyticsState): String {
    val diff = state.forecastCents - state.budgetCents
    return if (diff > 0) "${state.profileSettings.formatMoney(diff)} above budget pace" else "${state.profileSettings.formatMoney(-diff)} below budget pace"
}

private fun topEnvelopeNote(envelope: EnvelopeBalance?, profileSettings: ProfileSettings): String {
    if (envelope == null) return "No spending yet"
    val diff = envelope.spentCents - envelope.ceilingCents
    return if (diff > 0) "${profileSettings.formatMoney(diff)} over budget" else "${profileSettings.formatMoney(envelope.ceilingCents - envelope.spentCents)} remaining"
}

private fun orderedWeekdays(firstDay: DayOfWeek): List<DayOfWeek> =
    (0..6).map { firstDay.plus(it.toLong()) }
