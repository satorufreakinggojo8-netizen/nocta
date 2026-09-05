package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepSession
import kotlin.math.max
import kotlin.math.min

/**
 * Pure function: no Android/DB dependencies, so this is directly unit-testable
 * (see app/src/test/.../CalculateSleepScoreTest.kt). Mirrors the documented
 * 30/25/20/15/10 weighting from the product spec.
 */
class CalculateSleepScore(private val targetSleepMinutes: Int = 8 * 60) {

    operator fun invoke(recentSessions: List<SleepSession>): SleepScoreBreakdown {
        if (recentSessions.isEmpty()) {
            return SleepScoreBreakdown(0, 0, 0, 0, 0)
        }

        val duration = scoreDuration(recentSessions)
        val consistency = scoreConsistency(recentSessions)
        val timing = scoreTiming(recentSessions)
        val quality = scoreQuality(recentSessions)
        val routine = scoreRoutineAdherence(recentSessions)

        return SleepScoreBreakdown(duration, consistency, timing, quality, routine)
    }

    private fun scoreDuration(sessions: List<SleepSession>): Int {
        val avgMinutes = sessions.map { it.durationMinutes }.average()
        val ratio = avgMinutes / targetSleepMinutes
        // Full credit at or above target; graceful falloff below it.
        return (min(ratio, 1.0) * 100).toInt().coerceIn(0, 100)
    }

    private fun scoreConsistency(sessions: List<SleepSession>): Int {
        if (sessions.size < 2) return 70 // neutral default until there's enough history
        val bedtimeMinutes = sessions.map { it.bedtime.hour * 60 + it.bedtime.minute }
        val mean = bedtimeMinutes.average()
        val variance = bedtimeMinutes.map { (it - mean) * (it - mean) }.average()
        val stdDevMinutes = Math.sqrt(variance)
        // 0 min stddev -> 100; 90+ min stddev -> near 0.
        return max(0, 100 - (stdDevMinutes / 0.9).toInt()).coerceIn(0, 100)
    }

    private fun scoreTiming(sessions: List<SleepSession>): Int {
        // Rewards bedtimes that fall in a commonly-recommended 21:00–00:00 window;
        // this is a heuristic, not a clinical rule, and is presented as such in the UI.
        val idealStart = 21 * 60
        val idealEnd = 24 * 60
        val avgBedtimeMinutes = sessions.map { it.bedtime.hour * 60 + it.bedtime.minute }.average()
        val distance = when {
            avgBedtimeMinutes in idealStart.toDouble()..idealEnd.toDouble() -> 0.0
            avgBedtimeMinutes < idealStart -> idealStart - avgBedtimeMinutes
            else -> avgBedtimeMinutes - idealEnd
        }
        return max(0, 100 - (distance / 1.2).toInt()).coerceIn(0, 100)
    }

    private fun scoreQuality(sessions: List<SleepSession>): Int {
        val avgQuality = sessions.map { it.sleepQuality }.average() // 1-5 scale
        return ((avgQuality - 1) / 4.0 * 100).toInt().coerceIn(0, 100)
    }

    private fun scoreRoutineAdherence(sessions: List<SleepSession>): Int {
        // Placeholder heuristic until SleepRoutine completion tracking is wired in:
        // uses low night-awakening counts as a weak proxy signal. Documented as
        // an approximation — replace with real routine-completion data (see
        // docs/03-SCREENS.md, Wind-Down Mode).
        val avgAwakenings = sessions.map { it.nightAwakenings }.average()
        return max(0, 100 - (avgAwakenings * 20).toInt()).coerceIn(0, 100)
    }
}
