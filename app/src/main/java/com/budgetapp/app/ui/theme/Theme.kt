package com.budgetapp.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.budgetapp.app.R

private val BudgetColorScheme = darkColorScheme(
    primary = ChapPrimaryBright,
    onPrimary = ChapWhite,
    secondary = ChapPrimaryLight,
    onSecondary = ChapInk,
    background = ChapSurfaceDeep,
    onBackground = ChapWhiteSoft,
    surface = ChapPanel,
    onSurface = ChapWhiteSoft,
    surfaceVariant = ChapPanelRaised,
    onSurfaceVariant = ChapTextLight,
    error = ChapDanger,
    onError = ChapWhite,
)

@OptIn(ExperimentalTextApi::class)
private val SpaceGrotesk = FontFamily(
    Font(
        resId = R.font.space_grotesk,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.space_grotesk,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.space_grotesk,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.space_grotesk,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

private fun TextStyle.withSpaceGrotesk(): TextStyle = copy(fontFamily = SpaceGrotesk)

private val BudgetTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.withSpaceGrotesk(),
        displayMedium = base.displayMedium.withSpaceGrotesk(),
        displaySmall = base.displaySmall.withSpaceGrotesk(),
        headlineLarge = base.headlineLarge.withSpaceGrotesk(),
        headlineMedium = base.headlineMedium.withSpaceGrotesk(),
        headlineSmall = base.headlineSmall.withSpaceGrotesk(),
        titleLarge = base.titleLarge.withSpaceGrotesk(),
        titleMedium = base.titleMedium.withSpaceGrotesk(),
        titleSmall = base.titleSmall.withSpaceGrotesk(),
        bodyLarge = base.bodyLarge.withSpaceGrotesk(),
        bodyMedium = base.bodyMedium.withSpaceGrotesk(),
        bodySmall = base.bodySmall.withSpaceGrotesk(),
        labelLarge = base.labelLarge.withSpaceGrotesk(),
        labelMedium = base.labelMedium.withSpaceGrotesk(),
        labelSmall = base.labelSmall.withSpaceGrotesk(),
    )
}

@Composable
fun BudgetTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BudgetColorScheme,
        typography = BudgetTypography,
        content = content,
    )
}
