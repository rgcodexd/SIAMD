package com.ogpika.siama.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IndustrialBlue,
    onPrimary = Color.White,
    primaryContainer = IndustrialNavy,
    onPrimaryContainer = Color.White,
    secondary = IndustrialTeal,
    onSecondary = Color.White,
    background = IndustrialBgDark,
    onBackground = IndustrialTextPrimaryDark,
    surface = IndustrialSurfaceDark,
    onSurface = IndustrialTextPrimaryDark,
    surfaceVariant = IndustrialSurfaceElevatedDark,
    onSurfaceVariant = IndustrialTextSecondaryDark,
    outline = IndustrialBorderDark,
    outlineVariant = IndustrialBorderSubtleDark,
    error = StatusAbsentDark,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = IndustrialNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = IndustrialNavy,
    secondary = IndustrialTeal,
    onSecondary = Color.White,
    background = IndustrialBgLight,
    onBackground = IndustrialTextPrimaryLight,
    surface = IndustrialSurfaceLight,
    onSurface = IndustrialTextPrimaryLight,
    surfaceVariant = IndustrialSurfaceElevatedLight,
    onSurfaceVariant = IndustrialTextSecondaryLight,
    outline = IndustrialBorderLight,
    outlineVariant = IndustrialBorderSubtleLight,
    error = StatusAbsent,
    onError = Color.White
)

@Composable
fun SIAMATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}