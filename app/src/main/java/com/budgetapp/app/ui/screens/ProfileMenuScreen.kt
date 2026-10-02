package com.budgetapp.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.ui.components.ProfileAvatar
import com.budgetapp.app.ui.theme.MockBackground
import com.budgetapp.app.ui.theme.MockDanger
import com.budgetapp.app.ui.theme.MockInk
import com.budgetapp.app.ui.theme.MockMaterialSurface
import com.budgetapp.app.ui.theme.MockNeutral
import com.budgetapp.app.ui.theme.MockPanelRaised
import com.budgetapp.app.ui.theme.MockPrimary
import com.budgetapp.app.ui.theme.MockPrimaryAlt
import com.budgetapp.app.ui.theme.MockPrimaryDeep
import com.budgetapp.app.ui.theme.MockPrimaryPale
import com.budgetapp.app.ui.theme.MockPrimarySoft
import com.budgetapp.app.ui.theme.MockPrimaryWash
import com.budgetapp.app.ui.theme.MockSurface
import com.budgetapp.app.ui.theme.MockSurfaceBarely
import com.budgetapp.app.ui.theme.MockSurfaceDeep
import com.budgetapp.app.ui.theme.MockSurfaceLightAlt
import com.budgetapp.app.ui.theme.MockSurfaceNearWhite
import com.budgetapp.app.ui.theme.MockSurfacePale
import com.budgetapp.app.ui.theme.MockSurfaceRaised
import com.budgetapp.app.ui.theme.MockSurfaceTintAlt
import com.budgetapp.app.ui.theme.MockTextLight
import com.budgetapp.app.ui.theme.MockTextLightMuted
import com.budgetapp.app.ui.theme.MockTextMuted
import com.budgetapp.app.ui.theme.MockTextMutedBlue
import com.budgetapp.app.ui.theme.MockTextSecondary
import com.budgetapp.app.ui.theme.MockTextSoft
import com.budgetapp.app.ui.theme.MockWhite
import com.budgetapp.app.ui.theme.MockWhiteSoft

@Composable
fun ProfileMenuScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onInbox: () -> Unit,
    onEnvelopes: () -> Unit,
    onImport: () -> Unit,
    onRules: () -> Unit,
    onAnalytics: () -> Unit,
    onBackup: () -> Unit,
    onAbout: () -> Unit,
    onSupport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MockBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MockSurface)
                .padding(start = 32.dp, end = 32.dp, top = 92.dp, bottom = 48.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(
                    name = settings.name,
                    avatarUri = settings.avatarUri,
                    size = 56.dp,
                )
                Column {
                    Text(settings.name, color = MockWhite, fontSize = 32.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                    Text("Personal account", color = MockTextLightMuted, fontSize = 15.sp)
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MockBackground),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 164.dp),
        ) {
            item {
                MenuGroup {
                    MenuRow("Profile", MenuIconKind.Profile, onProfile)
                    MenuRow("Inbox", MenuIconKind.Inbox, onInbox)
                    MenuRow("Analytics", MenuIconKind.Analytics, onAnalytics)
                }
                MenuDivider()
                MenuGroup {
                    MenuRow("Envelopes", MenuIconKind.Envelope, onEnvelopes)
                    MenuRow("Linked Accounts", MenuIconKind.Calendar, onClick = {}, badge = "Soon", enabled = false)
                    MenuRow("Import Transactions", MenuIconKind.Import, onImport)
                    MenuRow("Spending Rules", MenuIconKind.List, onRules)
                }
                MenuDivider()
                MenuGroup {
                    MenuRow("Backup & Restore", MenuIconKind.Shield, onBackup)
                    MenuRow("About", MenuIconKind.Info, onAbout)
                    MenuRow("Support Chap", MenuIconKind.Heart, onSupport, badge = "Soon", enabled = false)
                }
                Spacer(modifier = Modifier.height(72.dp))
                Text(
                    text = "Local \u00b7 Private \u00b7 Yours alone\nPreview",
                    color = MockNeutral,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MenuGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        content = content,
    )
}

@Composable
private fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .height(1.dp)
            .background(MockSurfacePale),
    )
}

@Composable
private fun MenuRow(
    title: String,
    icon: MenuIconKind,
    onClick: () -> Unit,
    badge: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuIcon(icon, Modifier.size(22.dp), if (enabled) MockPrimaryAlt else MockTextLight)
        Text(
            text = title,
            color = if (enabled) MockMaterialSurface else MockNeutral,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MockPrimaryWash)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(badge.uppercase(), color = MockPrimaryDeep, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text("\u203a", color = MockTextLight, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MenuIcon(kind: MenuIconKind, modifier: Modifier = Modifier, tint: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.25.dp.toPx(), cap = StrokeCap.Round)
        when (kind) {
            MenuIconKind.Back -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.22f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.78f), Offset(w * 0.18f, h * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            }
            MenuIconKind.Profile -> {
                drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.32f), style = stroke)
                drawArc(tint, 205f, 130f, false, Offset(w * 0.18f, h * 0.55f), androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.54f), style = stroke)
            }
            MenuIconKind.Inbox, MenuIconKind.Envelope -> {
                drawRoundRect(tint, Offset(w * 0.12f, h * 0.25f), androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.52f), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()), style = stroke)
                drawLine(tint, Offset(w * 0.14f, h * 0.28f), Offset(w * 0.5f, h * 0.55f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.86f, h * 0.28f), Offset(w * 0.5f, h * 0.55f), strokeWidth = 2.dp.toPx())
            }
            MenuIconKind.Analytics -> {
                drawLine(tint, Offset(w * 0.28f, h * 0.78f), Offset(w * 0.28f, h * 0.5f), strokeWidth = 2.3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.78f), Offset(w * 0.5f, h * 0.22f), strokeWidth = 2.3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.72f, h * 0.78f), Offset(w * 0.72f, h * 0.38f), strokeWidth = 2.3.dp.toPx(), cap = StrokeCap.Round)
            }
            MenuIconKind.Calendar -> {
                drawRoundRect(tint, Offset(w * 0.15f, h * 0.22f), androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.62f), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()), style = stroke)
                drawLine(tint, Offset(w * 0.15f, h * 0.42f), Offset(w * 0.85f, h * 0.42f), strokeWidth = 2.dp.toPx())
            }
            MenuIconKind.Import -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.68f), strokeWidth = 2.3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.31f, h * 0.49f), Offset(w * 0.5f, h * 0.68f), strokeWidth = 2.3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.69f, h * 0.49f), Offset(w * 0.5f, h * 0.68f), strokeWidth = 2.3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.22f, h * 0.82f), Offset(w * 0.78f, h * 0.82f), strokeWidth = 2.3.dp.toPx(), cap = StrokeCap.Round)
            }
            MenuIconKind.List -> {
                listOf(0.28f, 0.5f, 0.72f).forEach {
                    drawLine(tint, Offset(w * 0.34f, h * it), Offset(w * 0.82f, h * it), strokeWidth = 2.1.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(tint, radius = 1.3.dp.toPx(), center = Offset(w * 0.18f, h * it))
                }
            }
            MenuIconKind.Shield -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.1f), Offset(w * 0.8f, h * 0.24f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.8f, h * 0.24f), Offset(w * 0.72f, h * 0.62f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.72f, h * 0.62f), Offset(w * 0.5f, h * 0.86f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.5f, h * 0.86f), Offset(w * 0.28f, h * 0.62f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.28f, h * 0.62f), Offset(w * 0.2f, h * 0.24f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.2f, h * 0.24f), Offset(w * 0.5f, h * 0.1f), strokeWidth = 2.dp.toPx())
            }
            MenuIconKind.Info -> {
                drawCircle(tint, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.48f), Offset(w * 0.5f, h * 0.68f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(tint, radius = 1.4.dp.toPx(), center = Offset(w * 0.5f, h * 0.34f))
            }
            MenuIconKind.Heart -> {
                drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.36f, h * 0.38f), style = stroke)
                drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.64f, h * 0.38f), style = stroke)
                drawLine(tint, Offset(w * 0.23f, h * 0.48f), Offset(w * 0.5f, h * 0.78f), strokeWidth = 2.dp.toPx())
                drawLine(tint, Offset(w * 0.77f, h * 0.48f), Offset(w * 0.5f, h * 0.78f), strokeWidth = 2.dp.toPx())
            }
        }
    }
}

enum class MenuIconKind {
    Back,
    Profile,
    Inbox,
    Analytics,
    Envelope,
    Calendar,
    Import,
    List,
    Shield,
    Info,
    Heart,
}
