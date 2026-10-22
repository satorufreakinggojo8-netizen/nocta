package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.profile.SleepProfile
import kotlin.math.max

/**
 * Calculates a transparent, non-medical score from recent sleep logs.
 * A 14-night input window is recommended. Absolute bedtime is not judged
 * against a one-size-fits-all clock window because no chronotype or schedule
 * preference is collected by the app.
 */
class CalculateSleepScore(
    private val sleepProfile: SleepProfile = SleepProfile(age = 18)
) {

    operator fun invoke(
        recentSessions: List<SleepSession>,
        profile: SleepProfile = sleepProfile
    ): SleepScoreBreakdown {
        val validSessions = recentSessions.filter { session ->
            session.durationMinutes in MIN_VALID_SLEEP_MINUTES..MAX_VALID_SLEEP_MINUTES &&
                session.nightAwakenings >= 0 &&
                session.awakeMinutes >= 0
        }
        if (validSessions.isEmpty()) return SleepScoreBreakdown(0, 0, 0, 0, 0, trackedNights = 0)

        return SleepScoreBreakdown(
            durationScore = scoreDuration(validSessions, profile),
            consistencyScore = scoreClockConsistency(validSessions) { it.bedtime },
            timingScore = scoreClockConsistency(validSessions) { midpoint(it) },
            qualityScore = scoreQuality(validSessions),
            routineScore = scoreAwakenings(validSessions),
            trackedNights = validSessions.size
        )
    }

    private fun scoreDuration(sessions: List<SleepSession>, profile: SleepProfile): Int {
        // Median keeps one unusually long or short night from dominating a fortnight.
        val typicalMinutes = sessions.map { it.durationMinutes }.sorted().let { sorted ->
            val middle = sorted.size / 2
            if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2.0
            else sorted[middle].toDouble()
        }
        val minimum = profile.recommendedMinMinutes.toDouble()
        val maximum = profile.recommendedMaxMinutes.toDouble()
        val preferred = (minimum + maximum) / 2.0
        return when {
            typicalMinutes < minimum ->
                (85.0 * typicalMinutes / minimum).toInt().coerceIn(0, 100)
            typicalMinutes <= maximum -> {
                val halfRange = ((maximum - minimum) / 2.0).coerceAtLeast(1.0)
                (100.0 - kotlin.math.abs(typicalMinutes - preferred) / halfRange * 15.0)
                    .toInt().coerceIn(0, 100)
            }
            else -> (85.0 - (typicalMinutes - maximum) * 0.25).toInt().coerceIn(0, 100)
        }
    }

    /**
     * Circular clock-time deviation: 11:50 p.m. and 12:10 a.m. are 20 minutes
     * apart, not almost 24 hours apart. With fewer than three nights there is
     * too little evidence to claim a reliable personal pattern.
     */
    private fun scoreClockConsistency(
        sessions: List<SleepSession>,
        timeOf: (SleepSession) -> java.time.LocalDateTime
    ): Int {
        if (sessions.size < MIN_NIGHTS_FOR_PATTERN) return INSUFFICIENT_HISTORY_SCORE
        val minutes = sessions.map { session ->
            val time = timeOf(session)
            time.hour * 60.0 + time.minute
        }
        val meanAbsoluteDeviation = CircularClockStats.meanAbsoluteDeviation(minutes)
        return (100.0 - max(0.0, meanAbsoluteDeviation - CONSISTENCY_GRACE_MINUTES) *
            CONSISTENCY_PENALTY_PER_MINUTE).toInt().coerceIn(0, 100)
    }

    private fun midpoint(session: SleepSession): java.time.LocalDateTime {
        val asleep = session.estimatedSleepTime
        val minutes = session.durationMinutes / 2
        return asleep.plusMinutes(minutes)
    }

    private fun scoreQuality(sessions: List<SleepSession>): Int {
        val reports = sessions.filter { it.sleepQuality in 1..5 }
        if (reports.isEmpty()) return INSUFFICIENT_HISTORY_SCORE
        val quality = reports.map { it.sleepQuality }.average()
        val energy = reports.map { it.morningEnergy.coerceIn(1, 5) }.average()
        val qualityPoints = (quality - 1.0) / 4.0 * 100.0
        val energyPoints = (energy - 1.0) / 4.0 * 100.0
        return (qualityPoints * 0.7 + energyPoints * 0.3).toInt().coerceIn(0, 100)
    }

    private fun scoreAwakenings(sessions: List<SleepSession>): Int {
        val awakeningPenalty = sessions.map { it.nightAwakenings.coerceAtMost(10) * 18.0 }.average()
        val awakePenalty = sessions.map { (it.awakeMinutes.coerceAtMost(240)).toDouble() / 3.0 }.average()
        return (100.0 - awakeningPenalty - awakePenalty).toInt().coerceIn(0, 100)
    }

    private companion object {
        const val MIN_VALID_SLEEP_MINUTES = 90L
        const val MAX_VALID_SLEEP_MINUTES = 20L * 60L
        const val MIN_NIGHTS_FOR_PATTERN = 3
        const val INSUFFICIENT_HISTORY_SCORE = 70
        const val CONSISTENCY_GRACE_MINUTES = 15.0
        const val CONSISTENCY_PENALTY_PER_MINUTE = 0.5
    }
}
