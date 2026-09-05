package com.nocta.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class DataSource { SELF_REPORTED, ESTIMATED, HEALTH_CONNECT, WEARABLE }

enum class SleepGoalType {
    SLEEP_LONGER, FALL_ASLEEP_FASTER, WAKE_EASIER, IMPROVE_CONSISTENCY,
    IMPROVE_RECOVERY, BETTER_ROUTINE, REDUCE_SCREEN_TIME
}

data class SleepSession(
    val id: String,
    val date: LocalDate,
    val bedtime: LocalDateTime,
    val sleepAttemptTime: LocalDateTime,
    val estimatedSleepTime: LocalDateTime,
    val wakeTime: LocalDateTime,
    val nightAwakenings: Int,
    val sleepQuality: Int,      // 1-5, self-reported
    val morningEnergy: Int,     // 1-5, self-reported
    val source: DataSource
) {
    val durationMinutes: Long
        get() = java.time.Duration.between(estimatedSleepTime, wakeTime).toMinutes()
}

/**
 * Breakdown backing the Home screen's Sleep Score. Weights match the
 * documented, transparent formula — this is presented to users as a
 * wellness metric, never a medical diagnostic score.
 */
data class SleepScoreBreakdown(
    val durationScore: Int,      // 0-100, weight 30%
    val consistencyScore: Int,   // 0-100, weight 25%
    val timingScore: Int,        // 0-100, weight 20%
    val qualityScore: Int,       // 0-100, weight 15%
    val routineScore: Int        // 0-100, weight 10%
) {
    val overall: Int
        get() = (
            durationScore * 0.30 +
            consistencyScore * 0.25 +
            timingScore * 0.20 +
            qualityScore * 0.15 +
            routineScore * 0.10
        ).toInt().coerceIn(0, 100)

    val label: String get() = when (overall) {
        in 85..100 -> "Excellent"
        in 70..84 -> "Good"
        in 50..69 -> "Fair"
        else -> "Needs attention"
    }
}

enum class SleepDebtStatus { WELL_RESTED, SLIGHTLY_BEHIND, CATCHING_UP }

data class SleepDebt(
    val status: SleepDebtStatus,
    val approximateDeficitMinutes: Int // clearly an estimate, never shown as exact/clinical
)

data class Recommendation(
    val id: String,
    val title: String,
    val why: String,
    val whatToDo: String,
    val difficulty: Difficulty,
    val expectedBenefit: String,
    val category: RecommendationCategory
)

enum class Difficulty { EASY, MODERATE, CHALLENGING }
enum class RecommendationCategory { ENVIRONMENT, SCHEDULE, LIFESTYLE, WIND_DOWN, MORNING }
