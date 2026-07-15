package com.pointcounter.tezzyruok.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FFDarkScheme = darkColorScheme(
    primary = FFOrange,
    secondary = FFOrangeDark,
    tertiary = FFYellow,
    background = FFDark,
    surface = FFCard,
    surfaceVariant = FFCard2,
    onBackground = FFText,
    onSurface = FFText,
    error = FFRed,
    outline = FFBorder
)

@Composable
fun PointCounterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FFDarkScheme,
        typography = FFTypography,
        content = content
    )
}
