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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.data.entity.TransactionEntity
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.ui.components.CompactHeaderBottomPadding
import com.budgetapp.app.ui.components.CompactHeaderHeroGap
import com.budgetapp.app.ui.components.CompactHeaderTopPadding
import com.budgetapp.app.ui.components.StandardEmptyState
import com.budgetapp.app.ui.components.StandardEmptyStateViewport
import com.budgetapp.app.ui.theme.MockBackground
import com.budgetapp.app.ui.theme.MockInk
import com.budgetapp.app.ui.theme.MockLavenderMuted
import com.budgetapp.app.ui.theme.MockMaterialSurface
import com.budgetapp.app.ui.theme.MockNeutral
import com.budgetapp.app.ui.theme.MockPrimaryAlt
import com.budgetapp.app.ui.theme.MockPrimaryBright
import com.budgetapp.app.ui.theme.MockPrimaryLight
import com.budgetapp.app.ui.theme.MockPurpleNight
import com.budgetapp.app.ui.theme.MockPurplePale
import com.budgetapp.app.ui.theme.MockSurface
import com.budgetapp.app.ui.theme.MockSurfaceBarely
import com.budgetapp.app.ui.theme.MockSurfaceLight
import com.budgetapp.app.ui.theme.MockSurfacePale
import com.budgetapp.app.ui.theme.MockSurfaceTintAlt
import com.budgetapp.app.ui.theme.MockTextLightMuted
import com.budgetapp.app.ui.theme.MockTextMutedBlue
import com.budgetapp.app.ui.theme.MockWarningGold
import com.budgetapp.app.ui.theme.MockWhite
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

@Composable
fun ReviewInboxScreen(
    viewModel: ReviewInboxViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val selectedEnvelopeIds = remember { mutableStateMapOf<String, Long>() }
    val ruleToggles = remember { mutableStateMapOf<String, Boolean>() }

    if (state.groups.isEmpty()) {
        InboxEmptyScreen(onBack = onBack, modifier = modifier)
    } else {
        ReviewInboxContent(
            state = state,
            selectedEnvelopeFor = { group ->
                selectedEnvelopeIds[group.key] ?: state.envelopes.firstOrNull()?.id
            },
            isRuleEnabled = { group -> ruleToggles[group.key] ?: true },
            onEnvelopeSelected = { group, envelopeId -> selectedEnvelopeIds[group.key] = envelopeId },
            onRuleToggle = { group -> ruleToggles[group.key] = !(ruleToggles[group.key] ?: true) },
            onAssign = { group, envelopeId ->
                viewModel.assignGroup(
                    group = group,
                    envelopeId = envelopeId,
                    createRule = ruleToggles[group.key] ?: true,
                )
            },
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun ReviewInboxContent(
    state: ReviewInboxState,
    selectedEnvelopeFor: (InboxTransactionGroup) -> Long?,
    isRuleEnabled: (InboxTransactionGroup) -> Boolean,
    onEnvelopeSelected: (InboxTransactionGroup, Long) -> Unit,
    onRuleToggle: (InboxTransactionGroup) -> Unit,
    onAssign: (InboxTransactionGroup, Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MockBackground),
    ) {
        InboxHeader(
            count = state.groups.sumOf { it.transactionIds.size },
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 18.dp,
                bottom = 164.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(state.groups, key = { it.key }) { group ->
                val selectedEnvelopeId = selectedEnvelopeFor(group)
                val selectedEnvelope = state.envelopes.firstOrNull { it.id == selectedEnvelopeId }
                InboxTransactionCard(
                    group = group,
                    envelopes = state.envelopes,
                    profileSettings = state.profileSettings,
                    selectedEnvelopeId = selectedEnvelopeId,
                    selectedEnvelopeName = selectedEnvelope?.name,
                    isRuleEnabled = isRuleEnabled(group),
                    onEnvelopeSelected = { onEnvelopeSelected(group, it) },
                    onRuleToggle = { onRuleToggle(group) },
                    onAssign = { envelopeId -> onAssign(group, envelopeId) },
                )
            }
        }
    }
}

@Composable
private fun InboxHeader(
    count: Int,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MockSurface)
            .padding(start = 24.dp, end = 24.dp, top = CompactHeaderTopPadding, bottom = CompactHeaderBottomPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Review inbox",
                color = MockWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .height(33.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MockPurpleNight)
                    .padding(horizontal = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    color = MockPrimaryLight,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(modifier = Modifier.height(CompactHeaderHeroGap))
        Text(
            text = "Assign transactions",
            color = MockWhite,
            fontSize = 32.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = "Assign each transaction to an envelope.",
            color = MockTextLightMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun InboxTransactionCard(
    group: InboxTransactionGroup,
    envelopes: List<EnvelopeBalance>,
    profileSettings: ProfileSettings,
    selectedEnvelopeId: Long?,
    selectedEnvelopeName: String?,
    isRuleEnabled: Boolean,
    onEnvelopeSelected: (Long) -> Unit,
    onRuleToggle: () -> Unit,
    onAssign: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfacePale, RoundedCornerShape(20.dp))
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    color = MockMaterialSurface,
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${group.postedAtMillis.toInboxDateLabel(profileSettings)} \u00b7 ${group.transactionIds.size} ${if (group.transactionIds.size == 1) "transaction" else "transactions"}",
                    color = MockNeutral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = profileSettings.formatMoney(abs(group.amountCents)),
                color = MockMaterialSurface,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = envelopes,
                key = { it.id },
            ) { envelope ->
                InboxChip(
                    text = envelope.name,
                    selected = envelope.id == selectedEnvelopeId,
                    onClick = { onEnvelopeSelected(envelope.id) },
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InboxIcon(InboxIconKind.Star, Modifier.size(15.dp), MockWarningGold)
                Text(
                    text = "Always assign ${group.title.toInboxRulePattern()} to ${selectedEnvelopeName ?: "this envelope"}",
                    color = MockTextMutedBlue,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            InboxSwitch(
                checked = isRuleEnabled,
                onClick = onRuleToggle,
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = { selectedEnvelopeId?.let(onAssign) },
            enabled = selectedEnvelopeId != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MockPrimaryAlt,
                contentColor = MockWhite,
                disabledContainerColor = MockSurfaceBarely,
                disabledContentColor = MockTextMutedBlue,
            ),
        ) {
            Text(
                text = "Assign to ${selectedEnvelopeName ?: "envelope"}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun InboxChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) MockPrimaryAlt else MockSurfaceTintAlt)
            .border(1.dp, if (selected) MockPrimaryAlt.copy(alpha = 0.35f) else MockSurfaceLight, RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) MockWhite else MockNeutral,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun InboxSwitch(
    checked: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 32.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (checked) MockPurplePale else MockSurfaceBarely)
            .clickable(onClick = onClick)
            .padding(4.dp)
            .semantics { contentDescription = "Always assign rule" },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) MockPrimaryBright else MockNeutral),
        )
    }
}

@Composable
private fun InboxEmptyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MockSurface),
    ) {
        StandardEmptyStateViewport(
            modifier = Modifier.align(Alignment.Center),
        ) {
            StandardEmptyState(
                title = "All caught up",
                copy = "New transactions will categorize automatically.",
                copyColor = MockLavenderMuted,
                icon = {
                    InboxIcon(InboxIconKind.Check, Modifier.size(42.dp), MockPrimaryAlt)
                },
            )
        }
    }
}

@Composable
private fun InboxIcon(
    kind: InboxIconKind,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (kind) {
            InboxIconKind.Back -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.22f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.78f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            }
            InboxIconKind.Check -> {
                drawLine(tint, Offset(w * 0.18f, h * 0.52f), Offset(w * 0.42f, h * 0.76f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.76f), Offset(w * 0.84f, h * 0.24f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            }
            InboxIconKind.Star -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.5f, h * 0.08f)
                    lineTo(w * 0.62f, h * 0.36f)
                    lineTo(w * 0.92f, h * 0.38f)
                    lineTo(w * 0.69f, h * 0.58f)
                    lineTo(w * 0.76f, h * 0.88f)
                    lineTo(w * 0.5f, h * 0.72f)
                    lineTo(w * 0.24f, h * 0.88f)
                    lineTo(w * 0.31f, h * 0.58f)
                    lineTo(w * 0.08f, h * 0.38f)
                    lineTo(w * 0.38f, h * 0.36f)
                    close()
                }
                drawPath(path, tint)
            }
        }
    }
}

private enum class InboxIconKind {
    Back,
    Check,
    Star,
}

private fun List<TransactionEntity>.toInboxGroups(): List<InboxTransactionGroup> {
    return groupBy { transaction ->
        val merchant = transaction.merchantName ?: transaction.description
        merchant.trim().uppercase(Locale.US)
    }.map { (key, transactions) ->
        val newest = transactions.maxBy { it.postedAtMillis }
        InboxTransactionGroup(
            key = key,
            title = newest.merchantName ?: newest.description,
            transactionIds = transactions.map { it.id },
            amountCents = transactions.sumOf { it.amountCents },
            postedAtMillis = newest.postedAtMillis,
        )
    }.sortedByDescending { it.postedAtMillis }
}

private fun String.toInboxRulePattern(): String {
    return trim().split(Regex("\\s+")).firstOrNull().orEmpty().ifBlank { trim() }
}

private fun Long.toInboxDateLabel(profileSettings: ProfileSettings): String {
    val date = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    return if (date == LocalDate.now()) {
        "Today"
    } else {
        profileSettings.formatDate(date)
    }
}
