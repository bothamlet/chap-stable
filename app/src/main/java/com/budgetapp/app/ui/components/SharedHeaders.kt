package com.budgetapp.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.ui.theme.MockSurface
import com.budgetapp.app.ui.theme.MockTextLightMuted
import com.budgetapp.app.ui.theme.MockWhite

val CompactHeaderTopPadding = 64.dp
val CompactHeaderBottomPadding = 24.dp
val CompactHeaderHeroGap = 22.dp

@Composable
fun StandardLightHeader(title: String, heading: String, copy: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MockSurface)
            .padding(start = 24.dp, end = 24.dp, top = CompactHeaderTopPadding, bottom = CompactHeaderBottomPadding),
    ) {
        Text(title, color = MockWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(CompactHeaderHeroGap))
        Text(heading, color = MockWhite, fontSize = 33.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(copy, color = MockTextLightMuted, fontSize = 16.sp, lineHeight = 22.sp)
    }
}

@Composable
fun SimpleDarkHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = CompactHeaderTopPadding),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = MockWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}
