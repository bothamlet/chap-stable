package com.budgetapp.app.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.ui.components.CompactHeaderBottomPadding
import com.budgetapp.app.ui.components.CompactHeaderHeroGap
import com.budgetapp.app.ui.components.CompactHeaderTopPadding
import com.budgetapp.app.ui.components.StandardEmptyState
import com.budgetapp.app.ui.components.StandardEmptyStateViewport
import com.budgetapp.app.ui.theme.MockBackground
import com.budgetapp.app.ui.theme.MockDanger
import com.budgetapp.app.ui.theme.MockInk
import com.budgetapp.app.ui.theme.MockLavenderAlt
import com.budgetapp.app.ui.theme.MockLavenderTint
import com.budgetapp.app.ui.theme.MockMaterialSurface
import com.budgetapp.app.ui.theme.MockNeutral
import com.budgetapp.app.ui.theme.MockPrimaryAlt
import com.budgetapp.app.ui.theme.MockPrimaryGlow
import com.budgetapp.app.ui.theme.MockPrimaryWash
import com.budgetapp.app.ui.theme.MockSurface
import com.budgetapp.app.ui.theme.MockSurfaceBarely
import com.budgetapp.app.ui.theme.MockSurfaceLight
import com.budgetapp.app.ui.theme.MockSurfaceLightAlt
import com.budgetapp.app.ui.theme.MockSurfacePale
import com.budgetapp.app.ui.theme.MockSurfaceTintAlt
import com.budgetapp.app.ui.theme.MockTextLight
import com.budgetapp.app.ui.theme.MockTextLightMuted
import com.budgetapp.app.ui.theme.MockTextMutedBlue
import com.budgetapp.app.ui.theme.MockTextSecondaryPurple
import com.budgetapp.app.ui.theme.MockWhite
import com.budgetapp.app.ui.theme.MockWhiteSoft
import java.util.Locale

@Composable
fun CategorizationRulesScreen(
    viewModel: CategorizationRulesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onModalVisibleChange: (Boolean) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf<RuleRow?>(null) }
    LaunchedEffect(editing != null) {
        onModalVisibleChange(editing != null)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (state.rules.isEmpty()) MockSurface else MockBackground),
    ) {
        if (state.rules.isEmpty()) {
            RulesEmptyContent(onBack = onBack)
        } else {
            RulesFullContent(
                state = state,
                onBack = onBack,
                onSearch = viewModel::setQuery,
                onEditRule = { editing = it },
            )
        }
        if (editing != null) {
            BackHandler { editing = null }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MockInk.copy(alpha = 0.46f))
                    .clickable { editing = null }
                    .semantics { contentDescription = "Dismiss rule editor" },
            )
            RuleEditorDrawer(
                row = editing,
                envelopes = state.envelopes,
                onEnvelopeSelected = { envelopeId ->
                    editing?.let { row ->
                        viewModel.moveRule(row.rule, envelopeId) { editing = null }
                    }
                },
                onDelete = {
                    editing?.let { row ->
                        viewModel.deleteRule(row.rule.id) { editing = null }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun RulesEmptyContent(
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = CompactHeaderTopPadding),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Spending Rules",
                color = MockWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        StandardEmptyStateViewport(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(),
        ) {
            StandardEmptyState(
                title = "No rules yet",
                copy = "Rules are created automatically when you categorize transactions.",
                copyColor = MockLavenderAlt,
                icon = {
                    RulesIcon(RulesIconKind.Sparkles, Modifier.size(36.dp), MockPrimaryGlow)
                },
            )
        }
    }
}

@Composable
private fun RulesFullContent(
    state: CategorizationRulesState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onEditRule: (RuleRow) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MockBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MockSurface)
                .padding(start = 24.dp, end = 24.dp, top = CompactHeaderTopPadding, bottom = CompactHeaderBottomPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Spending Rules",
                    color = MockWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(CompactHeaderHeroGap))
            Text(
                text = "Automate your sorting",
                color = MockWhite,
                fontSize = 33.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Assign merchants to envelopes so new transactions land where they belong.",
                color = MockTextLightMuted,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 20.dp,
                bottom = 164.dp,
            ),
        ) {
            item {
                SearchBar(value = state.query, onValueChange = onSearch)
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "ACTIVE RULES",
                        color = MockTextMutedBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.17.sp,
                    )
                    Text(
                        text = "${state.filteredRules.size} ${if (state.filteredRules.size == 1) "rule" else "rules"}",
                        color = MockNeutral,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
            items(state.filteredRules, key = { it.rule.id }) { row ->
                RuleCard(row = row, onClick = { onEditRule(row) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfacePale, RoundedCornerShape(99.dp))
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RulesIcon(RulesIconKind.Search, Modifier.size(21.dp), MockNeutral)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MockMaterialSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            text = "Search merchants...",
                            color = MockNeutral,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun RuleCard(
    row: RuleRow,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfacePale, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.rule.pattern,
            color = MockMaterialSurface,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        EnvelopePill(row.envelopeName)
        Text(
            text = "\u203a",
            color = MockTextLight,
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EnvelopePill(
    name: String,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(MockSurfaceTintAlt)
            .border(1.dp, MockSurfaceLight, RoundedCornerShape(99.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MockPrimaryAlt),
        )
        Text(
            text = name,
            color = MockTextMutedBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RuleEditorDrawer(
    row: RuleRow?,
    envelopes: List<EnvelopeBalance>,
    onEnvelopeSelected: (Long) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (row == null) return

    var selectedEnvelopeId by remember(row.rule.id) { mutableLongStateOf(row.rule.envelopeId) }
    var isDeleteConfirming by remember(row.rule.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfacePale, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
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
        if (isDeleteConfirming) {
            RuleDeleteConfirmation(
                pattern = row.rule.pattern,
                onCancel = { isDeleteConfirming = false },
                onDelete = onDelete,
            )
            return@Column
        }
        Text(
            text = "Edit rule",
            color = MockMaterialSurface,
            fontSize = 23.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${row.rule.pattern} will be assigned automatically.",
            color = MockNeutral,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "ENVELOPE",
            color = MockNeutral,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.96.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(7.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MockWhiteSoft)
                .border(1.dp, MockSurfaceLightAlt, RoundedCornerShape(16.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            envelopes.take(6).forEach { envelope ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (envelope.id == selectedEnvelopeId) MockPrimaryWash else MockWhiteSoft)
                        .clickable { selectedEnvelopeId = envelope.id }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = envelope.name,
                        color = if (envelope.id == selectedEnvelopeId) MockPrimaryAlt else MockMaterialSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = { onEnvelopeSelected(selectedEnvelopeId) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MockPrimaryAlt,
                contentColor = MockWhite,
            ),
        ) {
            Text("Save changes", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Delete rule",
            color = MockDanger,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { isDeleteConfirming = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun RuleDeleteConfirmation(
    pattern: String,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Text(
        text = "Delete rule?",
        color = MockMaterialSurface,
        fontSize = 23.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(9.dp))
    Text(
        text = "$pattern will no longer be categorized automatically.",
        color = MockNeutral,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(19.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MockSurfaceBarely,
                contentColor = MockMaterialSurface,
            ),
        ) {
            Text("Cancel", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onDelete,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MockMaterialSurface,
                contentColor = MockWhite,
            ),
        ) {
            Text("Delete", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RulesIcon(
    kind: RulesIconKind,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        when (kind) {
            RulesIconKind.Back -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.22f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.78f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            }
            RulesIconKind.Search -> {
                drawCircle(tint, radius = w * 0.32f, center = Offset(w * 0.43f, h * 0.43f), style = stroke)
                drawLine(tint, Offset(w * 0.66f, h * 0.66f), Offset(w * 0.86f, h * 0.86f), strokeWidth = 2.25.dp.toPx(), cap = StrokeCap.Round)
            }
            RulesIconKind.Sparkles -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.1f), Offset(w * 0.5f, h * 0.48f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.31f, h * 0.29f), Offset(w * 0.69f, h * 0.29f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.75f, h * 0.54f), Offset(w * 0.75f, h * 0.82f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.61f, h * 0.68f), Offset(w * 0.89f, h * 0.68f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.25f, h * 0.58f), Offset(w * 0.25f, h * 0.82f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.13f, h * 0.7f), Offset(w * 0.37f, h * 0.7f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

private enum class RulesIconKind {
    Back,
    Search,
    Sparkles,
}
