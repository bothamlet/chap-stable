package com.budgetapp.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.ui.components.SimpleDarkHeader
import com.budgetapp.app.ui.theme.ChapPrimaryAlt
import com.budgetapp.app.ui.theme.ChapPrimaryPale
import com.budgetapp.app.ui.theme.ChapSurface
import com.budgetapp.app.ui.theme.ChapSurfaceRaised
import com.budgetapp.app.ui.theme.ChapTextLightMuted
import com.budgetapp.app.ui.theme.ChapTextSoft
import com.budgetapp.app.ui.theme.ChapWhite

@Composable
fun SupportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(ChapSurface),
    ) {
        SimpleDarkHeader("Support Chap", onBack)
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MenuIcon(MenuIconKind.Heart, Modifier.size(44.dp), ChapPrimaryAlt)
            Spacer(modifier = Modifier.height(22.dp))
            Text("Support Chap", color = ChapWhite, fontSize = 38.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(11.dp))
            Text(
                "If this app helps you budget, you can leave a small thank you.",
                color = ChapTextLightMuted,
                fontSize = 16.sp,
                lineHeight = 23.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(ChapSurfaceRaised)
                        .border(1.dp, ChapWhite.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
                        .padding(20.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text("Buy me a coffee", color = ChapWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Processed by Buy Me a Coffee.", color = ChapTextSoft, fontSize = 14.sp)
                    }
                    Text("EUR 3+", color = ChapPrimaryPale, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChapPrimaryAlt, contentColor = ChapWhite),
                ) {
                    Text("Support with EUR 3", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
