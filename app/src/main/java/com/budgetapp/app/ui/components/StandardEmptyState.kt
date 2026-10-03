package com.budgetapp.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.ui.theme.MockPrimaryAlt
import com.budgetapp.app.ui.theme.MockTextLightMuted
import com.budgetapp.app.ui.theme.MockWhite

@Composable
fun StandardEmptyState(
    title: String,
    copy: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    copyColor: Color = MockTextLightMuted,
) {
    Column(
        modifier = modifier.widthIn(max = 330.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = MockWhite,
            fontSize = 26.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(13.dp))
        Text(
            text = copy,
            color = copyColor,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp)
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MockPrimaryAlt,
                    contentColor = MockWhite,
                ),
            ) {
                Text(
                    text = actionText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun StandardEmptyStateViewport(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 32.dp,
    bottomPadding: Dp = 118.dp,
    topPadding: Dp = 138.dp,
    verticalOffset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding)
            .padding(top = topPadding, bottom = bottomPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(vertical = verticalOffset.coerceAtLeast(0.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
    }
}
