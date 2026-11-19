package com.budgetapp.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
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
import com.budgetapp.app.ui.theme.ChapBackground
import com.budgetapp.app.ui.theme.ChapLavenderMuted
import com.budgetapp.app.ui.theme.ChapMaterialSurface
import com.budgetapp.app.ui.theme.ChapNeutral
import com.budgetapp.app.ui.theme.ChapPrimaryAlt
import com.budgetapp.app.ui.theme.ChapPrimaryBright
import com.budgetapp.app.ui.theme.ChapPrimaryLight
import com.budgetapp.app.ui.theme.ChapPurpleNight
import com.budgetapp.app.ui.theme.ChapPurplePale
import com.budgetapp.app.ui.theme.ChapSurface
import com.budgetapp.app.ui.theme.ChapSurfaceBarely
import com.budgetapp.app.ui.theme.ChapSurfaceLight
import com.budgetapp.app.ui.theme.ChapSurfacePale
import com.budgetapp.app.ui.theme.ChapSurfaceTintAlt
import com.budgetapp.app.ui.theme.ChapTextLightMuted
import com.budgetapp.app.ui.theme.ChapTextMutedBlue
import com.budgetapp.app.ui.theme.ChapWarningGold
import com.budgetapp.app.ui.theme.ChapWhite
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
    val mutation by viewModel.mutationStatus.collectAsState()
    MutationErrorDialog(mutation) { viewModel.mutationStatus.value = MutationStatus() }
    var selectedEnvelopeIds by rememberSaveable { mutableStateOf(emptyMap<String, Long>()) }
    var ruleToggles by rememberSaveable { mutableStateOf(emptyMap<String, Boolean>()) }

    if (state.groups.isEmpty()) {
        InboxEmptyScreen(onBack = onBack, modifier = modifier)
    } else {
        ReviewInboxContent(
            state = state,
            selectedEnvelopeFor = { group ->
                selectedEnvelopeIds[group.key] ?: state.candidatesFor(group).firstOrNull()?.id
            },
            isRuleEnabled = { group -> ruleToggles[group.key] ?: true },
            onEnvelopeSelected = { group, envelopeId -> selectedEnvelopeIds = selectedEnvelopeIds + (group.key to envelopeId) },
            onRuleToggle = { group -> ruleToggles = ruleToggles + (group.key to !(ruleToggles[group.key] ?: true)) },
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
            .background(ChapBackground),
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
                val candidates = state.candidatesFor(group)
                val selectedEnvelopeId = selectedEnvelopeFor(group)
                val selectedEnvelope = candidates.firstOrNull { it.id == selectedEnvelopeId }
                InboxTransactionCard(
                    group = group,
                    envelopes = candidates,
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
            .background(ChapSurface)
            .padding(start = 24.dp, end = 24.dp, top = CompactHeaderTopPadding, bottom = CompactHeaderBottomPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Review inbox",
                color = ChapWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .height(33.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(ChapPurpleNight)
                    .padding(horizontal = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    color = ChapPrimaryLight,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(modifier = Modifier.height(CompactHeaderHeroGap))
        Text(
            text = "Assign transactions",
            color = ChapWhite,
            fontSize = 32.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = "Assign each transaction to an envelope.",
            color = ChapTextLightMuted,
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
            .background(ChapWhite)
            .border(1.dp, ChapSurfacePale, RoundedCornerShape(20.dp))
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
                    color = ChapMaterialSurface,
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${group.postedAtMillis.toInboxDateLabel(profileSettings)} \u00b7 ${group.transactionIds.size} ${if (group.transactionIds.size == 1) "transaction" else "transactions"}",
                    color = ChapNeutral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = if (group.amountCents > 0L) {
                    "+${profileSettings.formatMoney(group.amountCents)}"
                } else {
                    profileSettings.formatMoney(abs(group.amountCents))
                },
                color = if (group.amountCents > 0L) ChapPrimaryAlt else ChapMaterialSurface,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        if (envelopes.isEmpty()) {
            Text(
                text = if (group.amountCents > 0L) {
                    "Create a goal envelope to save this money."
                } else {
                    "Create an envelope first."
                },
                color = ChapNeutral,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(20.dp))
            return@Column
        }
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
                InboxIcon(InboxIconKind.Star, Modifier.size(15.dp), ChapWarningGold)
                Text(
                    text = "Always assign ${group.title.toInboxRulePattern()} to ${selectedEnvelopeName ?: "this envelope"}",
                    color = ChapTextMutedBlue,
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
                containerColor = ChapPrimaryAlt,
                contentColor = ChapWhite,
                disabledContainerColor = ChapSurfaceBarely,
                disabledContentColor = ChapTextMutedBlue,
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
            .background(if (selected) ChapPrimaryAlt else ChapSurfaceTintAlt)
            .border(1.dp, if (selected) ChapPrimaryAlt.copy(alpha = 0.35f) else ChapSurfaceLight, RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) ChapWhite else ChapNeutral,
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
    modifier: Modifier = Modifier,
) {
    // The visual track stays 54x32dp, but the touch target and the semantics are a real switch:
    // TalkBack needs the checked state (INBOX.html uses role="switch" aria-checked) and the
    // interactive area needs to be at least 48dp tall.
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { onClick() },
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 32.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (checked) ChapPurplePale else ChapSurfaceBarely)
                .padding(4.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (checked) ChapPrimaryBright else ChapNeutral),
            )
        }
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
            .background(ChapSurface),
    ) {
        StandardEmptyStateViewport(
            modifier = Modifier.align(Alignment.Center),
        ) {
            StandardEmptyState(
                title = "All caught up",
                copy = "New transactions will categorize automatically.",
                copyColor = ChapLavenderMuted,
                icon = {
                    InboxIcon(InboxIconKind.Check, Modifier.size(42.dp), ChapPrimaryAlt)
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

private fun Long.toInboxDateLabel(profileSettings: ProfileSettings): String {
    val date = Instant.ofEpochMilli(this).atZone(java.time.ZoneOffset.UTC).toLocalDate()
    return if (date == LocalDate.now()) {
        "Today"
    } else {
        profileSettings.formatDate(date)
    }
}
