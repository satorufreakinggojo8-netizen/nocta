package com.nocta.app.domain.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

enum class SleepQualityCategory(val label: String, val color: Color) {
    EXCELLENT("Excellent", Color(0xFF1B5E20)),       // DARK GREEN
    GOOD("Good", Color(0xFF388E3C)),                 // GREEN
    FAIRLY_GOOD("Fairly Good", Color(0xFF81C784)),   // LIGHT GREEN
    POOR("Poor", Color(0xFFF57C00)),                 // ORANGE
    VERY_POOR("Very Poor", Color(0xFFD32F2F)),       // RED
    UNRECORDED("No Data", Color(0xFF26262E));        // NEUTRAL

    companion object {
        fun fromScore(score: Int): SleepQualityCategory = when {
            score >= 85 -> EXCELLENT
            score >= 70 -> GOOD
            score >= 55 -> FAIRLY_GOOD
            score >= 40 -> POOR
            else -> VERY_POOR
        }
    }
}

data class DailySleepRecord(
    val date: LocalDate,
    val session: SleepSession?,
    val score: Int?,
    val category: SleepQualityCategory
)
