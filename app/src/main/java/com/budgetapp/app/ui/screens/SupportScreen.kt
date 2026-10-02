package com.budgetapp.app.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.data.model.ProfileSettings
import com.budgetapp.app.ui.components.SimpleDarkHeader
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
fun SupportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MockSurface),
    ) {
        SimpleDarkHeader("Support Chap", onBack)
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MenuIcon(MenuIconKind.Heart, Modifier.size(44.dp), MockPrimaryAlt)
            Spacer(modifier = Modifier.height(22.dp))
            Text("Support Chap", color = MockWhite, fontSize = 38.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(11.dp))
            Text(
                "If this app helps you budget, you can leave a small thank you.",
                color = MockTextLightMuted,
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
                        .background(MockSurfaceRaised)
                        .border(1.dp, MockWhite.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
                        .padding(20.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text("Buy me a coffee", color = MockWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Processed by Buy Me a Coffee.", color = MockTextSoft, fontSize = 14.sp)
                    }
                    Text("EUR 3+", color = MockPrimaryPale, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MockPrimaryAlt, contentColor = MockWhite),
                ) {
                    Text("Support with EUR 3", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
