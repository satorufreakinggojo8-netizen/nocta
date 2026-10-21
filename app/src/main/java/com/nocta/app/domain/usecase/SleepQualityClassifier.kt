package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.SleepDayQuality
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.profile.SleepProfile
import kotlin.math.abs

/**
 * Classifies one saved night into a five-level sleep quality for the
 * sleep-history calendar. Deterministic and transparent, like
 * [CalculateSleepScore]:
 *
 *   night score = 0.5 × duration adequacy
 *               + 0.5 × self-reported quality (1–5 mapped to 0–100)
 *               − 3 points per night awakening (capped at 15)
 *
 * Duration adequacy uses the same 7–10 hour adult target the Sleep Score
 * uses (see [SleepProfile]). Nights outside the physically plausible
 * 90-minute … 20-hour window score zero, so an implausible record can never
 * read as a good night.
 */
class SleepQualityClassifier(
    private val profile: SleepProfile = SleepProfile(age = 18)
) {

    /** Level for a saved record; [SleepDayQuality.NO_RECORD] when absent. */
    fun classify(session: SleepSession?): SleepDayQuality {
        if (session == null) return SleepDayQuality.NO_RECORD
        return levelForScore(scoreNight(session))
    }

    /** 0–100 wellness score for a single night. */
    fun scoreNight(session: SleepSession): Double {
        val duration = session.durationMinutes
        if (duration < MIN_VALID_SLEEP_MINUTES || duration > MAX_VALID_SLEEP_MINUTES) {
            return 0.0
        }
        val selfReported =
            ((session.sleepQuality.coerceIn(1, 5) - 1) / 4.0) * 100.0
        val awakeningPenalty =
            session.nightAwakenings.coerceIn(0, MAX_PENALISED_AWAKENINGS) * AWAKENING_PENALTY_POINTS

        return (
            DURATION_WEIGHT * durationScore(duration) +
                QUALITY_WEIGHT * selfReported -
                awakeningPenalty
            ).coerceIn(0.0, 100.0)
    }

    /**
     * Maps a 0–100 night (or month-average) score to a level. Also used by the
     * yearly view to grade a month from the average of its saved nights.
     */
    fun levelForScore(score: Double): SleepDayQuality = when {
        score >= EXCELLENT_MINIMUM -> SleepDayQuality.EXCELLENT
        score >= GOOD_MINIMUM -> SleepDayQuality.GOOD
        score >= FAIR_MINIMUM -> SleepDayQuality.FAIR
        score >= POOR_MINIMUM -> SleepDayQuality.POOR
        else -> SleepDayQuality.VERY_POOR
    }

    private fun durationScore(durationMinutes: Long): Double {
        val minimum = profile.recommendedMinMinutes.toDouble()
        val maximum = profile.recommendedMaxMinutes.toDouble()
        val preferred = (minimum + maximum) / 2.0
        val halfRange = ((maximum - minimum) / 2.0).coerceAtLeast(1.0)
        return when {
            durationMinutes < minimum ->
                (100.0 * durationMinutes / minimum)
            durationMinutes <= maximum ->
                100.0 - abs(durationMinutes - preferred) / halfRange * 10.0
            else ->
                100.0 - (durationMinutes - maximum) * OVERSHOOT_PENALTY_PER_MINUTE
        }.coerceIn(0.0, 100.0)
    }

    companion object {
        const val MIN_VALID_SLEEP_MINUTES = 90L
        const val MAX_VALID_SLEEP_MINUTES = 20L * 60L

        const val EXCELLENT_MINIMUM = 85.0
        const val GOOD_MINIMUM = 70.0
        const val FAIR_MINIMUM = 50.0
        const val POOR_MINIMUM = 30.0

        private const val DURATION_WEIGHT = 0.5
        private const val QUALITY_WEIGHT = 0.5
        private const val AWAKENING_PENALTY_POINTS = 3.0
        private const val MAX_PENALISED_AWAKENINGS = 5
        private const val OVERSHOOT_PENALTY_PER_MINUTE = 0.15
    }
}
