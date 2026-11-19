package com.budgetapp.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.ui.theme.ChapBorderDeep
import com.budgetapp.app.ui.theme.ChapNeutral
import com.budgetapp.app.ui.theme.ChapPrimaryAlt
import com.budgetapp.app.ui.theme.ChapSurface
import com.budgetapp.app.ui.theme.ChapWhite

@Composable
fun MainBottomNav(
    active: MainBottomNavDestination,
    onHome: () -> Unit,
    onAddTransaction: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ChapSurface)
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ChapBorderDeep),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 9.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            MainNavItem(
                label = "Home",
                icon = MainNavIcon.Home,
                isActive = active == MainBottomNavDestination.Home,
                onClick = onHome,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.width(78.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ChapPrimaryAlt)
                        .clickable(onClick = onAddTransaction)
                        .semantics { contentDescription = "Add transaction" },
                    contentAlignment = Alignment.Center,
                ) {
                    MainNavIconCanvas(
                        icon = MainNavIcon.Plus,
                        tint = ChapWhite,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            MainNavItem(
                label = "Menu",
                icon = MainNavIcon.Menu,
                isActive = active == MainBottomNavDestination.Menu,
                onClick = onMenu,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

enum class MainBottomNavDestination {
    Home,
    Menu,
    Other,
}

@Composable
private fun MainNavItem(
    label: String,
    icon: MainNavIcon,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (isActive) ChapPrimaryAlt else ChapNeutral
    Column(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MainNavIconCanvas(icon = icon, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MainNavIconCanvas(
    icon: MainNavIcon,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (icon) {
            MainNavIcon.Home -> {
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
            MainNavIcon.Plus -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.82f), strokeWidth = 2.6.dp.toPx())
                drawLine(tint, Offset(w * 0.18f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = 2.6.dp.toPx())
            }
            MainNavIcon.Menu -> {
                drawLine(tint, Offset(w * 0.18f, h * 0.28f), Offset(w * 0.82f, h * 0.28f), strokeWidth = 2.4.dp.toPx())
                drawLine(tint, Offset(w * 0.18f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = 2.4.dp.toPx())
                drawLine(tint, Offset(w * 0.18f, h * 0.72f), Offset(w * 0.82f, h * 0.72f), strokeWidth = 2.4.dp.toPx())
            }
        }
    }
}

private enum class MainNavIcon {
    Home,
    Plus,
    Menu,
}
