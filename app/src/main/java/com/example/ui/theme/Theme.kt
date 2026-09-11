package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TradingColorScheme = darkColorScheme(
    primary = BullGreen,
    onPrimary = TerminalBg,
    primaryContainer = SurfaceCard,
    onPrimaryContainer = BullGreen,
    secondary = Ema9Cyan,
    onSecondary = TerminalBg,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = Ema9Cyan,
    tertiary = Ema21Orange,
    onTertiary = TerminalBg,
    background = TerminalBg,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    error = BearRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek trading dark theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TradingColorScheme,
        typography = Typography,
        content = content
    )
}

