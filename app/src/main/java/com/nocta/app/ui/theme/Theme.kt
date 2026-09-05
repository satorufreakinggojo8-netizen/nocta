package com.nocta.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark is the primary, designed-for experience. A light scheme exists so the
// app doesn't hard-crash into an undesigned state on light-mode devices, and
// so a real light theme can be dropped in later without restructuring.
private val NoctaDarkColorScheme = darkColorScheme(
    background = NoctaBackground,
    surface = NoctaSurface,
    surfaceVariant = NoctaSurfaceElevated,
    primary = NoctaAccent,
    onPrimary = NoctaTextPrimary,
    onBackground = NoctaTextPrimary,
    onSurface = NoctaTextPrimary,
    secondary = NoctaTextSecondary,
    error = NoctaAlert
)

private val NoctaLightColorScheme = lightColorScheme(
    primary = NoctaAccent,
    background = Color(0xFFFAFAF9),
)

@Composable
fun NoctaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // intentionally off: brand palette is a design requirement
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NoctaDarkColorScheme else NoctaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NoctaTypography,
        shapes = NoctaShapes,
        content = content
    )
}
