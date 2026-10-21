package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.MorningCheckIn
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.profile.SleepProfile
import java.time.Duration
import javax.inject.Inject

/** One factor of the sleep score with its weight and a data-grounded note. */
data class NightlyFactor(
    val name: String,
    val score: Int,
    val weightPercent: Int,
    val bandLabel: String
)

/**
 * A complete, deterministic report for a single recorded night. Every number
 * comes from stored records — nothing here estimates what was not measured.
 */
data class NightlyReport(
    val session: SleepSession,
    val checkIn: MorningCheckIn?,
    val timeInBedMinutes: Long,
    val asleepMinutes: Long,
    /** Null when the data source does not support awake-time reporting. */
    val efficiencyPercent: Int?,
    /** Score over the 14-night window ending at this night (matches Home). */
    val breakdown: SleepScoreBreakdown,
    val nightsInScoreWindow: Int,
    val baselineNightsCompared: Int,
    val baselineAvgDurationMinutes: Long?,
    /** This night's sleep minus the baseline average; null without baseline. */
    val durationDeltaMinutes: Long?,
    /** Circular mean bedtime of the baseline, minutes since midnight. */
    val baselineMeanBedtimeMinutes: Int?,
    /** Signed circular delta of this bedtime vs baseline; negative = earlier. */
    val bedtimeDeltaMinutes: Int?,
    val strongestFactor: NightlyFactor,
    val improvementFactor: NightlyFactor,
    val recommendation: String,
    val missingInputs: List<String>
)

/**
 * Builds the detailed nightly report shown on Sleep Details.
 *
 * The score shown for a night is exactly the score the app would have shown
 * for the 14-night window ending at that night — score history is therefore
 * always consistent with the stored nightly records.
 */
class BuildNightlyReport @Inject constructor(
    private val calculateSleepScore: CalculateSleepScore
) {

    operator fun invoke(
        session: SleepSession,
        recentSessions: List<SleepSession>,
        checkIn: MorningCheckIn?,
        profile: SleepProfile = SleepProfile(age = 18)
    ): NightlyReport {
        // Nights up to and including this one; dedupe by date, oldest first.
        val upToNight = recentSessions
            .filter { !it.date.isAfter(session.date) }
            .groupBy { it.date }
            .values
            .map { it.last() }
            .sortedBy { it.date }

        val scoreWindow = upToNight.takeLast(SCORE_WINDOW_NIGHTS)
        val breakdown = calculateSleepScore(scoreWindow, profile)
        val baseline = upToNight.filter { it.id != session.id }.takeLast(BASELINE_NIGHTS)

        val timeInBed = Duration.between(
            session.estimatedSleepTime,
            session.wakeTime
        ).toMinutes().coerceAtLeast(0)
        val asleep = session.durationMinutes

        // Efficiency needs credible awake-time data, which today only the
        // self-reported path provides.
        val efficiency = if (session.source == DataSource.SELF_REPORTED && timeInBed >= MINUTES_FOR_EFFICIENCY) {
            ((asleep.toDouble() / timeInBed) * 100).toInt().coerceIn(0, 100)
        } else null

        val baselineAvgDuration = baseline
            .takeIf { it.isNotEmpty() }
            ?.map { it.durationMinutes }
            ?.average()
            ?.toLong()

        val durationDelta = baselineAvgDuration?.let { asleep - it }

        val baselineBedtimes = baseline.map { clockMinutesOf(it) }
        val baselineMeanBedtime = baselineBedtimes
            .takeIf { it.isNotEmpty() }
            ?.let { CircularClockStats.meanClockMinutes(it).toInt() }
        val bedtimeDelta = baselineMeanBedtime?.let {
            CircularClockStats.signedDelta(clockMinutesOf(session), it.toDouble()).toInt()
        }

        val factors = factorsOf(breakdown)
        val strongest = factors.maxBy { it.score }
        val improvement = factors.minBy { it.score }

        val missing = buildList {
            if (checkIn == null) add("No morning check-in was saved for this night.")
            if (session.source != DataSource.SELF_REPORTED) {
                add("Time asleep is an estimate from ${session.source.name.lowercase().replace('_', ' ')}; efficiency is not available.")
            }
            if (scoreWindow.size < 3) {
                add("Fewer than 3 nights in this window — consistency and timing are neutral placeholders, not penalties.")
            }
        }

        return NightlyReport(
            session = session,
            checkIn = checkIn,
            timeInBedMinutes = timeInBed,
            asleepMinutes = asleep,
            efficiencyPercent = efficiency,
            breakdown = breakdown,
            nightsInScoreWindow = scoreWindow.size,
            baselineNightsCompared = baseline.size,
            baselineAvgDurationMinutes = baselineAvgDuration,
            durationDeltaMinutes = durationDelta,
            baselineMeanBedtimeMinutes = baselineMeanBedtime,
            bedtimeDeltaMinutes = bedtimeDelta,
            strongestFactor = strongest,
            improvementFactor = improvement,
            recommendation = buildRecommendation(improvement, session, baseline, profile),
            missingInputs = missing
        )
    }

    private fun factorsOf(b: SleepScoreBreakdown): List<NightlyFactor> = listOf(
        NightlyFactor("Duration", b.durationScore, 40, band(b.durationScore)),
        NightlyFactor("Bedtime consistency", b.consistencyScore, 25, band(b.consistencyScore)),
        NightlyFactor("Sleep quality", b.qualityScore, 15, band(b.qualityScore)),
        NightlyFactor("Schedule timing", b.timingScore, 10, band(b.timingScore)),
        NightlyFactor("Night awakenings", b.routineScore, 10, band(b.routineScore))
    )

    private fun band(score: Int): String = when (score) {
        in 85..100 -> "Strong"
        in 70..84 -> "Solid"
        in 50..69 -> "Room to improve"
        else -> "Biggest opportunity"
    }

    private fun buildRecommendation(
        weakest: NightlyFactor,
        session: SleepSession,
        baseline: List<SleepSession>,
        profile: SleepProfile
    ): String {
        val preferred = (profile.recommendedMinMinutes + profile.recommendedMaxMinutes) / 2.0
        return when (weakest.name) {
            "Duration" -> {
                val median = baseline.map { it.durationMinutes }.plus(session.durationMinutes)
                    .sorted().let { list -> list[list.size / 2] }.toDouble()
                val gap = preferred - median
                when {
                    gap > 15 -> {
                        val shift = minOf(60.0, maxOf(15.0, gap / 2)).toInt()
                        "You're averaging ${formatDuration(median.toLong())} against about " +
                            "${formatDuration(preferred.toLong())} for your age. Try lights-out " +
                            "${shift} minutes earlier for the next few nights."
                    }
                    gap < -60 -> "You're sleeping well beyond your target range. Keep your schedule steady rather than cutting sleep."
                    else -> "Your duration sits inside your target range — protecting your bedtime is the priority now."
                }
            }
            "Bedtime consistency" -> {
                val deviation = if (baseline.size >= 2) {
                    val mins = (baseline + session).map { clockMinutesOf(it) }
                    CircularClockStats.meanAbsoluteDeviation(mins).toInt()
                } else 0
                "Your bedtime has varied by about ${deviation}m night to night. " +
                    "Pick one lights-out time and hold it within ±15 minutes for the next week."
            }
            "Sleep quality" -> {
                val avgQuality = (baseline + session).map { it.sleepQuality }.filter { it in 1..5 }
                    .takeIf { it.isNotEmpty() }
                    ?.average()
                    ?.let { "%.1f".format(it) }
                    ?: "—"
                "Your check-ins average ${avgQuality}/5. A consistent wind-down and a cool, dark room " +
                    "are the most reliable first levers for how rested you feel."
            }
            "Schedule timing" ->
                "Your sleep midpoint keeps moving. A regular wake time anchors the whole schedule " +
                    "more strongly than a regular bedtime does."
            else -> {
                val avgAwakenings = (baseline + session).map { it.nightAwakenings }
                    .average().let { "%.1f".format(it) }
                "You average ${avgAwakenings} awakenings a night. Evening caffeine timing, light, " +
                    "and room temperature are the common controllable factors."
            }
        }
    }

    private fun clockMinutesOf(session: SleepSession): Double =
        session.bedtime.hour * 60.0 + session.bedtime.minute

    private fun formatDuration(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0L) "${h}h" else "${h}h ${m}m"
    }

    private companion object {
        const val SCORE_WINDOW_NIGHTS = 14
        const val BASELINE_NIGHTS = 14
        const val MINUTES_FOR_EFFICIENCY = 60L
    }
}
