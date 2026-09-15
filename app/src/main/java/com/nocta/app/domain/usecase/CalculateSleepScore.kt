package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.profile.SleepProfile
import kotlin.math.max

class CalculateSleepScore(
    private val sleepProfile: SleepProfile = SleepProfile(age = 18)
) {

    operator fun invoke(recentSessions: List<SleepSession>): SleepScoreBreakdown {
        if (recentSessions.isEmpty()) {
            return SleepScoreBreakdown(0, 0, 0, 0, 0)
        }

        val duration = scoreDuration(recentSessions)
        val consistency = scoreConsistency(recentSessions)
        val timing = scoreTiming(recentSessions)
        val quality = scoreQuality(recentSessions)
        val routine = scoreRoutineAdherence(recentSessions)

        return SleepScoreBreakdown(
            durationScore = duration,
            consistencyScore = consistency,
            timingScore = timing,
            qualityScore = quality,
            routineScore = routine
        )
    }

    private fun scoreDuration(sessions: List<SleepSession>): Int {
        val averageMinutes = sessions
            .map { it.durationMinutes }
            .average()

        val minimum = sleepProfile.recommendedMinMinutes
        val maximum = sleepProfile.recommendedMaxMinutes

        return when {
            averageMinutes < minimum -> {
                val ratio = averageMinutes / minimum.toDouble()
                (ratio * 100).toInt().coerceIn(0, 100)
            }

            averageMinutes <= maximum -> {
                100
            }

            else -> {
                100
            }
        }
    }

    private fun scoreConsistency(sessions: List<SleepSession>): Int {
        if (sessions.size < 2) return 70

        val bedtimeMinutes = sessions.map {
            it.bedtime.hour * 60 + it.bedtime.minute
        }

        val mean = bedtimeMinutes.average()

        val variance = bedtimeMinutes
            .map { (it - mean) * (it - mean) }
            .average()

        val standardDeviationMinutes = Math.sqrt(variance)

        return max(
            0,
            100 - (standardDeviationMinutes / 0.9).toInt()
        ).coerceIn(0, 100)
    }

    private fun scoreTiming(sessions: List<SleepSession>): Int {
        val averageBedtimeMinutes = sessions
            .map { it.bedtime.hour * 60 + it.bedtime.minute }
            .average()

        val idealStart = 20 * 60
        val idealEnd = 24 * 60

        val distance = when {
            averageBedtimeMinutes in idealStart.toDouble()..idealEnd.toDouble() -> 0.0
            averageBedtimeMinutes < idealStart ->
                idealStart - averageBedtimeMinutes
            else ->
                averageBedtimeMinutes - idealEnd
        }

        return max(
            0,
            100 - (distance / 1.5).toInt()
        ).coerceIn(0, 100)
    }

    private fun scoreQuality(sessions: List<SleepSession>): Int {
        val averageQuality = sessions
            .map { it.sleepQuality }
            .average()

        return ((averageQuality - 1) / 4.0 * 100)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun scoreRoutineAdherence(sessions: List<SleepSession>): Int {
        val averageAwakenings = sessions
            .map { it.nightAwakenings }
            .average()

        return max(
            0,
            100 - (averageAwakenings * 20).toInt()
        ).coerceIn(0, 100)
    }
}
