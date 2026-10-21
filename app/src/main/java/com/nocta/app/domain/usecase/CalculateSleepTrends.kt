package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.profile.SleepProfile
import java.time.LocalDate
import javax.inject.Inject

/** One point of the nightly score chart. */
data class ScorePoint(val date: LocalDate, val score: Int)

/** One point of the nightly duration chart, in minutes asleep. */
data class DurationPoint(val date: LocalDate, val minutes: Long)

/** Count of nightly scores inside one band. */
data class ScoreBandCount(val band: String, val count: Int)

/**
 * Summary of a trailing window of real sleep records. Every field is derived
 * from stored sessions only — missing nights are reported as counts, never
 * filled with placeholder values.
 */
data class TrendSummary(
    val windowDays: Int,
    val trackedNights: Int,
    val missedNights: Int,
    val averageDurationMinutes: Long?,
    val averageScore: Int?,
    /** Circular mean bedtime, minutes since midnight. */
    val averageBedtimeMinutes: Int?,
    /** 0–100 bedtime regularity on the score's own scale; null if <3 nights. */
    val consistencyPercent: Int?,
    /** Consecutive nights ending today or yesterday; 0 once a night was missed. */
    val currentStreakNights: Int,
    val longestStreakNights: Int,
    val scoreDistribution: List<ScoreBandCount>,
    val scoreSeries: List<ScorePoint>,
    val durationSeries: List<DurationPoint>
)

/**
 * Computes 7-, 30- and 90-day summaries from the stored sleep log.
 *
 * Each night's score is the same score Home would have shown for the 14-night
 * window ending at that night, so trend charts never contradict the nightly
 * records.
 */
class CalculateSleepTrends @Inject constructor(
    private val calculateSleepScore: CalculateSleepScore
) {

    operator fun invoke(
        sessions: List<SleepSession>,
        windowDays: Int,
        today: LocalDate,
        profile: SleepProfile = SleepProfile(age = 18)
    ): TrendSummary {
        val start = today.minusDays(windowDays - 1L)

        // One night per date, oldest first, across all history.
        val byDate = sessions
            .groupBy { it.date }
            .values
            .map { it.last() }
            .sortedBy { it.date }

        val nightsInWindow = byDate.filter { !it.date.isBefore(start) && !it.date.isAfter(today) }
        val trackedDates = nightsInWindow.map { it.date }.toSet()

        val scoreSeries = nightsInWindow.map { night ->
            val window = byDate.filter { !it.date.isAfter(night.date) }.takeLast(SCORE_WINDOW_NIGHTS)
            ScorePoint(night.date, calculateSleepScore(window, profile).overall)
        }

        val durationSeries = nightsInWindow.map { DurationPoint(it.date, it.durationMinutes) }

        val bedtimeMinutes = nightsInWindow.map { it.bedtime.hour * 60.0 + it.bedtime.minute }
        val avgBedtime = bedtimeMinutes
            .takeIf { it.isNotEmpty() }
            ?.let { CircularClockStats.meanClockMinutes(it).toInt() }

        val consistency = if (nightsInWindow.size >= MIN_NIGHTS_FOR_PATTERN) {
            val deviation = CircularClockStats.meanAbsoluteDeviation(bedtimeMinutes)
            (100.0 - maxOf(0.0, deviation - CONSISTENCY_GRACE_MINUTES) * CONSISTENCY_PENALTY_PER_MINUTE)
                .toInt().coerceIn(0, 100)
        } else null

        val distribution = listOf(
            ScoreBandCount("Excellent (85–100)", scoreSeries.count { it.score in 85..100 }),
            ScoreBandCount("Good (70–84)", scoreSeries.count { it.score in 70..84 }),
            ScoreBandCount("Fair (50–69)", scoreSeries.count { it.score in 50..69 }),
            ScoreBandCount("Needs attention (<50)", scoreSeries.count { it.score < 50 })
        )

        return TrendSummary(
            windowDays = windowDays,
            trackedNights = nightsInWindow.size,
            missedNights = (windowDays - nightsInWindow.size).coerceAtLeast(0),
            averageDurationMinutes = nightsInWindow
                .takeIf { it.isNotEmpty() }
                ?.map { it.durationMinutes }
                ?.average()
                ?.toLong(),
            averageScore = scoreSeries
                .takeIf { it.isNotEmpty() }
                ?.map { it.score }
                ?.average()
                ?.toInt(),
            averageBedtimeMinutes = avgBedtime,
            consistencyPercent = consistency,
            currentStreakNights = currentStreak(trackedDates, today),
            longestStreakNights = longestStreak(trackedDates),
            scoreDistribution = distribution,
            scoreSeries = scoreSeries,
            durationSeries = durationSeries
        )
    }

    /**
     * Counts consecutive tracked dates ending today; a record from yesterday
     * keeps the streak alive until tonight is logged. A gap older than that
     * resets the streak to zero — the app never invents continuity.
     */
    private fun currentStreak(trackedDates: Set<LocalDate>, today: LocalDate): Int {
        val end = when {
            today in trackedDates -> today
            today.minusDays(1) in trackedDates -> today.minusDays(1)
            else -> return 0
        }
        var count = 0
        var cursor = end
        while (cursor in trackedDates) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    private fun longestStreak(trackedDates: Set<LocalDate>): Int {
        if (trackedDates.isEmpty()) return 0
        val sorted = trackedDates.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1].plusDays(1)) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    private companion object {
        const val SCORE_WINDOW_NIGHTS = 14
        const val MIN_NIGHTS_FOR_PATTERN = 3
        const val CONSISTENCY_GRACE_MINUTES = 15.0
        const val CONSISTENCY_PENALTY_PER_MINUTE = 0.5
    }
}
