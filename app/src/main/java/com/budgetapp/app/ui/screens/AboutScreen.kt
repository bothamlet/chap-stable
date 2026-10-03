package com.budgetapp.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.ui.components.SimpleDarkHeader
import com.budgetapp.app.ui.theme.MockPrimaryAlt
import com.budgetapp.app.ui.theme.MockSurface
import com.budgetapp.app.ui.theme.MockTextLightMuted
import com.budgetapp.app.ui.theme.MockTextSecondary
import com.budgetapp.app.ui.theme.MockWhite

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MockSurface),
    ) {
        SimpleDarkHeader("About", onBack)
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MenuIcon(MenuIconKind.Info, Modifier.size(48.dp), MockPrimaryAlt)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Chap", color = MockWhite, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Preview - Envelope budgeting, simplified.", color = MockPrimaryAlt, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "No data leaves your device.\nAll transactions are stored locally.",
                color = MockTextLightMuted,
                fontSize = 17.sp,
                lineHeight = 27.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text("Built with Jetpack Compose & Room.", color = MockTextSecondary, fontSize = 14.sp)
        }
    }
}
