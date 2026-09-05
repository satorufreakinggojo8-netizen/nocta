package com.nocta.app.ui.theme

import androidx.compose.ui.graphics.Color

// Nocta palette — near-black / charcoal / soft white, one accent.
// Accent: "Dusk Violet" — calm, distinct from typical health-app green/blue,
// reads well on near-black without feeling alarming.

val NoctaBackground = Color(0xFF0A0A0D)      // near-black
val NoctaSurface = Color(0xFF16161B)         // charcoal card surface
val NoctaSurfaceElevated = Color(0xFF1F1F26) // frosted/glass card, slightly lighter
val NoctaSurfaceBorder = Color(0x1AFFFFFF)   // 10% white hairline border

val NoctaTextPrimary = Color(0xFFF5F3F0)     // soft off-white
val NoctaTextSecondary = Color(0xFFA6A3AE)   // muted gray
val NoctaTextTertiary = Color(0xFF6E6B77)

val NoctaAccent = Color(0xFF8B7CF6)          // dusk violet — primary actions, key metrics
val NoctaAccentMuted = Color(0x298B7CF6)     // 16% accent, for subtle fills
val NoctaAccentGradientEnd = Color(0xFF5B4FD1)

val NoctaSuccess = Color(0xFF6FCF97)         // used sparingly: "well rested" states
val NoctaWarning = Color(0xFFE8B563)         // "slightly behind" sleep debt
val NoctaAlert = Color(0xFFE07A6B)           // "catching up" / attention states

// Score band colors (kept desaturated so they don't compete with the accent)
val NoctaScoreExcellent = NoctaAccent
val NoctaScoreGood = Color(0xFF8FD6C4)
val NoctaScoreFair = NoctaWarning
val NoctaScorePoor = NoctaAlert
