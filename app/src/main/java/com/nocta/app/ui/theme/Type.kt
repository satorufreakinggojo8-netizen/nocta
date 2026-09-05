package com.nocta.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Uses the system default variable font family; swap FontFamily.Default for a
// custom licensed font (e.g. a variable sans like "General Sans" or "Inter")
// before shipping — placeholder kept deliberately generic here.
private val NoctaFontFamily = FontFamily.Default

// Large numeric display style used ONLY for the sleep score and similarly
// weighted single numbers — not part of Material's default Typography, so it
// lives as a standalone token consumed directly by SleepScoreCard.
val NoctaScoreDisplay = TextStyle(
    fontFamily = NoctaFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 72.sp,
    letterSpacing = (-1.5).sp
)

val NoctaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NoctaFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NoctaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NoctaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NoctaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NoctaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NoctaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    )
)
