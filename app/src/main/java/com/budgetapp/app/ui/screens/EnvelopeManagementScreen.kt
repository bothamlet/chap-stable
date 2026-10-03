package com.budgetapp.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.data.model.EnvelopeType
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.domain.parseAmountMinorUnits
import com.budgetapp.app.ui.components.CompactHeaderBottomPadding
import com.budgetapp.app.ui.components.CompactHeaderHeroGap
import com.budgetapp.app.ui.components.CompactHeaderTopPadding
import com.budgetapp.app.ui.components.EnvelopeCategoryIcon
import com.budgetapp.app.ui.components.EnvelopeIconCatalog
import com.budgetapp.app.ui.components.suggestEnvelopeIconToken
import com.budgetapp.app.ui.components.StandardEmptyState
import com.budgetapp.app.ui.components.StandardEmptyStateViewport
import com.budgetapp.app.ui.theme.MockBackground
import com.budgetapp.app.ui.theme.MockDanger
import com.budgetapp.app.ui.theme.MockInk
import com.budgetapp.app.ui.theme.MockLavenderTint
import com.budgetapp.app.ui.theme.MockMaterialSurface
import com.budgetapp.app.ui.theme.MockNeutral
import com.budgetapp.app.ui.theme.MockPrimaryAlt
import com.budgetapp.app.ui.theme.MockPrimaryDeep
import com.budgetapp.app.ui.theme.MockPrimaryGlow
import com.budgetapp.app.ui.theme.MockPrimaryWash
import com.budgetapp.app.ui.theme.MockSurface
import com.budgetapp.app.ui.theme.MockSurfaceBarely
import com.budgetapp.app.ui.theme.MockSurfacePale
import com.budgetapp.app.ui.theme.MockTextLightMuted
import com.budgetapp.app.ui.theme.MockTextMutedBlue
import com.budgetapp.app.ui.theme.MockWhite
import com.budgetapp.app.ui.theme.MockWhiteSoft
import com.budgetapp.app.ui.theme.MockLavenderAlt
import java.util.Locale
import kotlinx.coroutines.delay

private val EnvelopeEditorStateSaver: Saver<EnvelopeEditorState?, List<Any?>> =
    Saver(
        save = { editor ->
            editor?.let {
                arrayListOf<Any?>(it.envelopeId, it.name, it.amountText, it.iconToken, it.type, it.isDeleteConfirming)
            }
        },
        restore = { saved ->
            EnvelopeEditorState(
                envelopeId = saved[0] as? Long,
                name = saved[1] as String,
                amountText = saved[2] as String,
                iconToken = saved[3] as String,
                type = saved[4] as String,
                isDeleteConfirming = saved[5] as Boolean,
            )
        },
    )

@Composable
fun EnvelopeManagementScreen(    viewModel: EnvelopeManagementViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    createRequestKey: Int = 0,
    onModalVisibleChange: (Boolean) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val mutation by viewModel.mutationStatus.collectAsState()
    MutationErrorDialog(mutation) { viewModel.mutationStatus.value = MutationStatus() }
    var editor by rememberSaveable(stateSaver = EnvelopeEditorStateSaver) { mutableStateOf<EnvelopeEditorState?>(null) }
    var handledCreateRequestKey by rememberSaveable { mutableIntStateOf(createRequestKey) }

    LaunchedEffect(createRequestKey) {
        if (createRequestKey > handledCreateRequestKey) {
            handledCreateRequestKey = createRequestKey
            editor = EnvelopeEditorState()
        }
    }
    SideEffect {
        onModalVisibleChange(editor != null)
    }

    EnvelopeManagementContent(
        state = state,
        editor = editor,
        onBack = onBack,
        onCreate = { editor = EnvelopeEditorState() },
        onEdit = { row ->
            editor = EnvelopeEditorState(
                envelopeId = row.id,
                name = row.name,
                amountText = row.ceilingCents.toMajorString(),
                iconToken = row.source.iconToken,
                type = row.type,
            )
        },
        onEditorChange = { editor = it },
        onDismissEditor = { editor = null },
        onSave = { current ->
            // Keep the submitted draft so the field errors can render when the save is refused.
            editor = current
            viewModel.saveEnvelope(current) { editor = null }
        },
        onDelete = { id -> viewModel.deleteEnvelope(id) { editor = null } },
        modifier = modifier,
    )
}
@Composable
internal fun EnvelopeManagementContent(
    state: EnvelopeManagementState,
    editor: EnvelopeEditorState?,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (EnvelopeRow) -> Unit,
    onEditorChange: (EnvelopeEditorState) -> Unit,
    onDismissEditor: () -> Unit,
    onSave: (EnvelopeEditorState) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (state.envelopes.isEmpty()) MockSurface else MockBackground),
    ) {
        if (state.envelopes.isEmpty()) {
            EmptyEnvelopeManagement(
                onBack = onBack,
                onCreate = onCreate,
            )
        } else {
            FullEnvelopeManagement(
                state = state,
                onBack = onBack,
                onCreate = onCreate,
                onEdit = onEdit,
            )
        }

        if (editor != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MockInk.copy(alpha = 0.46f))
                    .clickable(onClick = onDismissEditor)
                    .semantics { contentDescription = "Dismiss editor" },
            )
            EnvelopeEditorDrawer(
                editor = editor,
                errorMessage = state.errorMessage,
                profileSettings = state.profileSettings,
                onEditorChange = onEditorChange,
                onSave = onSave,
                onDelete = onDelete,
                onDismiss = onDismissEditor,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clickable(onClick = {}),
            )
        }
    }
}

@Composable
internal fun EmptyEnvelopeManagement(
    onBack: () -> Unit,
    onCreate: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        EnvelopeNavRow(
            onBack = onBack,
            onCreate = onCreate,
            showCreate = false,
            modifier = Modifier.padding(top = CompactHeaderTopPadding),
        )
        StandardEmptyStateViewport(
            modifier = Modifier
                .fillMaxSize(),
            horizontalPadding = 0.dp,
        ) {
            StandardEmptyState(
                title = "No envelopes yet",
                copy = "Create envelopes to plan where your money goes.",
                copyColor = MockLavenderAlt,
                actionText = "Create envelope",
                onAction = onCreate,
                icon = {
                    EnvelopeIcon(
                        icon = EnvelopeManagementIcon.Envelope,
                        tint = MockPrimaryGlow,
                        modifier = Modifier.size(36.dp),
                    )
                },
            )
        }
    }
}

@Composable
internal fun FullEnvelopeManagement(
    state: EnvelopeManagementState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (EnvelopeRow) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MockSurface)
                .padding(start = 24.dp, end = 24.dp, top = CompactHeaderTopPadding, bottom = CompactHeaderBottomPadding),
        ) {
        EnvelopeNavRow(
            onBack = onBack,
            onCreate = onCreate,
            showCreate = true,
        )
            Spacer(modifier = Modifier.height(CompactHeaderHeroGap))
            Text(
                text = "Plan your spending",
                color = MockWhite,
                fontSize = 34.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Set aside money for variable spending and bills.",
                color = MockTextLightMuted,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth(0.9f),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MockBackground),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 21.dp,
                bottom = 164.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            if (state.variable.isNotEmpty()) {
                item {
                    EnvelopeSection(
                        title = "Variable",
                        envelopes = state.variable,
                        profileSettings = state.profileSettings,
                        onEdit = onEdit,
                    )
                }
            }
            if (state.fixed.isNotEmpty()) {
                item {
                    EnvelopeSection(
                        title = "Fixed",
                        envelopes = state.fixed,
                        profileSettings = state.profileSettings,
                        onEdit = onEdit,
                    )
                }
            }
        }
    }
}

@Composable
internal fun EnvelopeNavRow(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    showCreate: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Envelopes",
                color = MockWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (showCreate) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MockPrimaryAlt.copy(alpha = 0.12f))
                    .clickable(onClick = onCreate)
                    .semantics { contentDescription = "Create envelope" },
                contentAlignment = Alignment.Center,
            ) {
                EnvelopeIcon(
                    icon = EnvelopeManagementIcon.Plus,
                    tint = MockPrimaryAlt,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@Composable
internal fun IconHitTarget(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun EnvelopeSection(
    title: String,
    envelopes: List<EnvelopeRow>,
    profileSettings: ProfileSettings,
    onEdit: (EnvelopeRow) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.uppercase(Locale.US),
                color = MockTextMutedBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.17.sp,
            )
            Text(
                text = if (envelopes.firstOrNull()?.type == EnvelopeType.Goal) {
                    "${profileSettings.formatMoney(envelopes.sumOf { it.spentCents })} saved"
                } else {
                    "${profileSettings.formatMoney(envelopes.sumOf { it.ceilingCents })} planned"
                },
                color = if (envelopes.any { it.remainingCents < 0L }) MockDanger else MockNeutral,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            envelopes.forEach { envelope ->
                EnvelopeManagementCard(
                    envelope = envelope,
                    profileSettings = profileSettings,
                    onClick = { onEdit(envelope) },
                )
            }
        }
    }
}

@Composable
internal fun EnvelopeManagementCard(
    envelope: EnvelopeRow,
    profileSettings: ProfileSettings,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfacePale, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EnvelopeCategoryIcon(
                    token = envelope.source.iconToken,
                    tint = MockPrimaryAlt,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = envelope.name,
                    color = MockMaterialSurface,
                    fontSize = 17.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = profileSettings.formatMoney(envelope.ceilingCents),
                color = MockMaterialSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(13.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(MockSurfaceBarely),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(envelope.progress)
                    .height(7.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MockPrimaryAlt),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (envelope.type == EnvelopeType.Goal) {
                    if (envelope.remainingCents < 0L) {
                        "${profileSettings.formatMoney(envelope.spentCents)} saved \u00b7 ${profileSettings.remainingLabel(envelope.remainingCents)}"
                    } else {
                        "${profileSettings.formatMoney(envelope.spentCents)} saved \u00b7 ${profileSettings.formatMoney(envelope.remainingCents)} to go"
                    }
                } else {
                    "${profileSettings.formatMoney(envelope.spentCents)} spent \u00b7 ${profileSettings.remainingLabel(envelope.remainingCents)}"
                },
                color = MockNeutral,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            EnvelopeIcon(
                icon = EnvelopeManagementIcon.ChevronRight,
                tint = MockTextLightMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun EnvelopeEditorDrawer(
    editor: EnvelopeEditorState,
    errorMessage: String?,
    profileSettings: ProfileSettings,
    onEditorChange: (EnvelopeEditorState) -> Unit,
    onSave: (EnvelopeEditorState) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
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
        label = "envelopeDrawerOffset",
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
        label = "envelopeDrawerDragOffset",
    )
    LaunchedEffect(editor.envelopeId) {
        if (editor.name.isBlank() && editor.envelopeId == null) {
            onEditorChange(editor.copy(type = EnvelopeType.Variable))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .onGloballyPositioned { coordinates ->
                drawerHeightPx = coordinates.size.height.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(onDismiss) {
                detectVerticalDragGestures(
                    onDragStart = { isDragging = true },
                    onVerticalDrag = { _, dragAmount ->
                        dragTarget = kotlin.math.max(0f, dragTarget + dragAmount)
                    },
                    onDragEnd = {
                        isDragging = false
                        if (dragTarget > drawerHeightPx * 0.3f) {
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
            .graphicsLayer { translationY = drawerOffset + dragOffset }
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MockWhite)
            .border(1.dp, MockSurfacePale, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .verticalScroll(rememberScrollState())
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
            DeleteConfirmation(
                name = editor.name.ifBlank { "this envelope" },
                onCancel = { onEditorChange(editor.copy(isDeleteConfirming = false)) },
                onDelete = {
                    val id = editor.envelopeId
                    if (id != null) {
                        onDelete(id)
                    }
                },
            )
        } else {
            EnvelopeEditorForm(
                editor = editor,
                errorMessage = errorMessage,
                profileSettings = profileSettings,
                onEditorChange = onEditorChange,
                onSave = onSave,
            )
        }
    }
}

@Composable
internal fun EnvelopeEditorForm(
    editor: EnvelopeEditorState,
    errorMessage: String?,
    profileSettings: ProfileSettings,
    onEditorChange: (EnvelopeEditorState) -> Unit,
    onSave: (EnvelopeEditorState) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (editor.isEditing) "Edit envelope" else "Create envelope",
            color = MockMaterialSurface,
            fontSize = 23.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(19.dp))
        DrawerField(
            label = "Envelope name",
            error = if (editor.isNameInvalid) "Give the envelope a name." else null,
        ) {
            EnvelopeInput(
                value = editor.name,
                onValueChange = { onEditorChange(editor.copy(name = it)) },
                placeholder = "Groceries",
                isError = editor.isNameInvalid,
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
        DrawerField(label = "Icon") {
            EnvelopeIconPicker(
                envelopeName = editor.name,
                selectedToken = editor.iconToken,
                onSelected = { onEditorChange(editor.copy(iconToken = it)) },
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
        DrawerField(
            label = if (editor.type == EnvelopeType.Goal) "Goal amount" else "Budget amount",
            error = if (editor.isAmountInvalid) "Enter an amount like 250 or 250,50." else null,
        ) {
            AmountInput(
                value = editor.amountText,
                currencySymbol = profileSettings.currencySymbol(),
                onValueChange = { value ->
                    onEditorChange(editor.copy(amountText = value.filter { it.isDigit() || it == '.' || it == ',' }))
                },
                isError = editor.isAmountInvalid,
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
        DrawerField(label = "Type") {
            SegmentedTypeControl(
                selectedType = editor.type,
                onSelected = { onEditorChange(editor.copy(type = it)) },
            )
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = errorMessage,
                color = MockDanger,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(19.dp))
        Button(
            onClick = { onSave(editor.copy(hasSubmitted = true)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MockPrimaryAlt,
                contentColor = MockWhite,
            ),
        ) {
            Text(
                text = "Save envelope",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (editor.isEditing) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onEditorChange(editor.copy(isDeleteConfirming = true)) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Delete envelope",
                    color = MockDanger,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun EnvelopeIconPicker(
    envelopeName: String,
    selectedToken: String,
    onSelected: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val suggestedToken = remember(envelopeName) { suggestEnvelopeIconToken(envelopeName) }
    val normalizedSelectedToken = EnvelopeIconCatalog.normalized(selectedToken)

    LaunchedEffect(suggestedToken) {
        val suggestedIndex =
            EnvelopeIconCatalog.options.indexOfFirst { it.token == suggestedToken }
        if (suggestedIndex >= 0 && suggestedToken != normalizedSelectedToken) {
            listState.animateScrollToItem((suggestedIndex - 2).coerceAtLeast(0))
        }
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(
            items = EnvelopeIconCatalog.options,
            key = { it.token },
        ) { option ->
            val selected = option.token == normalizedSelectedToken
            val suggested = option.token == suggestedToken && !selected
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        when {
                            selected -> MockPrimaryWash
                            suggested -> MockPrimaryWash.copy(alpha = 0.42f)
                            else -> MockWhiteSoft
                        },
                    )
                    .border(
                        width = if (selected || suggested) 2.dp else 1.dp,
                        color =
                            when {
                                selected -> MockPrimaryAlt
                                suggested -> MockPrimaryGlow
                                else -> MockSurfacePale
                            },
                        shape = RoundedCornerShape(14.dp),
                    )
                    .selectable(
                        selected = selected,
                        onClick = { onSelected(option.token) },
                    )
                    .semantics {
                        contentDescription =
                            when {
                                selected -> "${option.label} icon, selected"
                                suggested -> "${option.label} icon, suggested"
                                else -> "${option.label} icon"
                            }
                    },
                contentAlignment = Alignment.Center,
            ) {
                EnvelopeCategoryIcon(
                    token = option.token,
                    tint = if (selected) MockPrimaryAlt else if (suggested) MockPrimaryGlow else MockNeutral,
                    modifier = Modifier.size(24.dp),
                )
                if (suggested) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MockPrimaryGlow),
                    )
                }
            }
        }
    }
}

@Composable
internal fun DrawerField(
    label: String,
    error: String? = null,
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
        if (error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error,
                color = MockDanger,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun EnvelopeInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = MockMaterialSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MockWhiteSoft)
            .border(1.dp, if (isError) MockDanger else MockSurfacePale, RoundedCornerShape(16.dp))
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
internal fun AmountInput(
    value: String,
    currencySymbol: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MockWhiteSoft)
            .border(1.dp, if (isError) MockDanger else MockSurfacePale, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currencySymbol,
            color = MockNeutral,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp),
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
                .weight(1f)
                .fillMaxHeight(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    innerTextField()
                }
            },
        )
    }
}

@Composable
internal fun SegmentedTypeControl(
    selectedType: String,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MockSurfaceBarely)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EnvelopeSegment(
            text = "Variable",
            selected = selectedType == EnvelopeType.Variable,
            onClick = { onSelected(EnvelopeType.Variable) },
            modifier = Modifier.weight(1f),
        )
        EnvelopeSegment(
            text = "Fixed",
            selected = selectedType == EnvelopeType.Fixed,
            onClick = { onSelected(EnvelopeType.Fixed) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun EnvelopeSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MockWhite else MockSurfaceBarely)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) MockPrimaryDeep else MockNeutral,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun DeleteConfirmation(
    name: String,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Delete $name?",
            color = MockMaterialSurface,
            fontSize = 23.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(9.dp))
        Text(
            text = "This envelope will be removed from your budget.",
            color = MockNeutral,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
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
}

@Composable
internal fun EnvelopeIcon(
    icon: EnvelopeManagementIcon,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.3.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (icon) {
            EnvelopeManagementIcon.Back -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = 2.5.dp.toPx())
                drawLine(tint, Offset(w * 0.42f, h * 0.22f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx())
                drawLine(tint, Offset(w * 0.42f, h * 0.78f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx())
            }
            EnvelopeManagementIcon.Plus -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), strokeWidth = 2.5.dp.toPx())
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), strokeWidth = 2.5.dp.toPx())
            }
            EnvelopeManagementIcon.Envelope -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.1f, h * 0.2f),
                    size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = stroke,
                )
                drawLine(tint, Offset(w * 0.12f, h * 0.25f), Offset(w * 0.5f, h * 0.55f), strokeWidth = 2.1.dp.toPx())
                drawLine(tint, Offset(w * 0.88f, h * 0.25f), Offset(w * 0.5f, h * 0.55f), strokeWidth = 2.1.dp.toPx())
            }
            EnvelopeManagementIcon.ChevronRight -> {
                drawLine(tint, Offset(w * 0.36f, h * 0.24f), Offset(w * 0.64f, h * 0.5f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.64f, h * 0.5f), Offset(w * 0.36f, h * 0.76f), strokeWidth = 2.dp.toPx())
            }
        }
    }
}

internal enum class EnvelopeManagementIcon {
    Back,
    Plus,
    Envelope,
    ChevronRight,
}

internal fun String.toCentsOrNull(): Long? = parseAmountMinorUnits(this)?.takeIf { it > 0L }

internal fun Long.toMajorString(): String {
    val euros = this / 100.0
    return if (this % 100L == 0L) {
        "%d".format(Locale.US, this / 100L)
    } else {
        "%.2f".format(Locale.US, euros)
    }
}
