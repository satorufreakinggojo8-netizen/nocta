package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.profile.SleepProfile
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * A recommended sleep schedule with the reasoning attached.
 */
data class BedtimePlan(
    /** Lights-out time to aim for tonight. */
    val recommendedBedtime: LocalTime,
    /** The wake time the plan is built around. */
    val wakeTime: LocalTime,
    /** When to start winding down, before lights-out. */
    val windDownStart: LocalTime,
    /** Total time allowed in bed (sleep + onset allowance). */
    val sleepOpportunityMinutes: Long,
    /** How much earlier (positive) or later (negative) than recent habit. */
    val shiftFromRecentAverageMinutes: Int,
    /** Shift capped per night to change the schedule gradually; 0 if on target. */
    val suggestedShiftTonightMinutes: Int,
    /** Nights of real history backing this plan. */
    val nightsOfHistoryUsed: Int,
    /** Human-readable reasons, most important first. */
    val explanation: List<String>
)

/**
 * Builds a realistic bedtime plan from the user's age target, required wake
 * time, onset allowance and actual sleep history.
 *
 * Design notes:
 * - Sleep opportunity = target sleep (midpoint of the age range) + onset
 *   allowance, never below the age minimum.
 * - When at least [MIN_NIGHTS_FOR_HISTORY] nights exist, the plan is anchored
 *   to the user's *actual average duration*, capped inside the age range, so
 *   it recommends a small step rather than an impossible leap.
 * - Schedule changes are phased at [MAX_SHIFT_PER_NIGHT] minutes per night.
 * - Bedtimes are compared circularly so 11:50 PM vs 12:10 AM is 20 minutes.
 */
class PlanBedtime @Inject constructor() {

    operator fun invoke(
        today: LocalDate,
        wakeTime: LocalTime,
        recentSessions: List<SleepSession>,
        profile: SleepProfile,
        onsetAllowanceMinutes: Int = DEFAULT_ONSET_ALLOWANCE_MINUTES,
        windDownMinutes: Int = DEFAULT_WIND_DOWN_MINUTES
    ): BedtimePlan {
        val minimum = profile.recommendedMinMinutes.toLong()
        val maximum = profile.recommendedMaxMinutes.toLong()
        val preferred = (minimum + maximum) / 2

        val windowStart = today.minusDays(HISTORY_WINDOW_DAYS)
        val history = recentSessions
            .filter { !it.date.isBefore(windowStart) && !it.date.isAfter(today) }
            .sortedBy { it.date }

        val targetSleepMinutes = if (history.size >= MIN_NIGHTS_FOR_HISTORY) {
            // Anchor to actual recent sleep, kept inside the age range.
            val recentAverage = history.map { it.durationMinutes }.average().toLong()
            recentAverage.coerceIn(minimum, maximum)
        } else {
            preferred
        }

        val opportunity = targetSleepMinutes + onsetAllowanceMinutes
        val idealBedtime = wakeTime.minusMinutes(opportunity)

        val bedtimeMinutes = history.map { it.bedtime.hour * 60.0 + it.bedtime.minute }
        val recentAverageBedtime = bedtimeMinutes
            .takeIf { it.isNotEmpty() }
            ?.let { CircularClockStats.meanClockMinutes(it).toInt() }

        val shift = recentAverageBedtime?.let {
            CircularClockStats.signedDelta(
                idealBedtime.hour * 60.0 + idealBedtime.minute,
                it.toDouble()
            ).toInt()
        } ?: 0

        // Phase the change: step at most MAX_SHIFT_PER_NIGHT minutes toward the
        // ideal; if the gap is already inside a night's grace, aim for the ideal.
        val suggestedShift = when {
            shift == 0 -> 0
            kotlin.math.abs(shift) <= MAX_SHIFT_PER_NIGHT -> shift
            else -> MAX_SHIFT_PER_NIGHT * if (shift > 0) 1 else -1
        }

        val effectiveBedtime = if (suggestedShift == 0) {
            idealBedtime
        } else {
            // Apply the step to the recent average bedtime, not to the ideal.
            val stepped = (recentAverageBedtime ?: 0) + suggestedShift
            LocalTime.ofSecondOfDay((((stepped % 1440) + 1440) % 1440 * 60).toLong())
        }

        val windDownStart = effectiveBedtime.minusMinutes(windDownMinutes.toLong())

        val explanation = buildList {
            add(
                "Your age range suggests about ${formatRange(profile)} of sleep — " +
                    "the plan targets ${formatDuration(targetSleepMinutes)}."
            )
            add(
                "Built around your ${wakeTime.format(TIME)} wake time, plus " +
                    "${onsetAllowanceMinutes} min to fall asleep and " +
                    "${windDownMinutes} min wind-down."
            )
            if (history.size >= MIN_NIGHTS_FOR_HISTORY) {
                add(
                    "Based on your last ${history.size} recorded nights (average " +
                        "${formatDuration(history.map { it.durationMinutes }.average().toLong())} asleep)."
                )
                if (suggestedShift != 0) {
                    // shift > 0 means the ideal bedtime is later on the clock
                    // than the user's recent habit, i.e. they've been going
                    // to bed earlier than target, and vice versa.
                    val direction = if (suggestedShift > 0) "earlier" else "later"
                    add(
                        "You've been going to bed about ${kotlin.math.abs(shift)} min $direction " +
                            "than target. Move lights-out by only " +
                            "${kotlin.math.abs(suggestedShift)} min ${if (suggestedShift > 0) "later" else "earlier"} " +
                            "tonight — gradual shifts stick better than overnight changes."
                    )
                }
            } else {
                add(
                    "Only ${history.size} recorded night${if (history.size == 1) "" else "s"} so far — " +
                        "the plan will adapt as your history grows."
                )
            }
        }

        return BedtimePlan(
            recommendedBedtime = effectiveBedtime,
            wakeTime = wakeTime,
            windDownStart = windDownStart,
            sleepOpportunityMinutes = opportunity,
            shiftFromRecentAverageMinutes = shift,
            suggestedShiftTonightMinutes = suggestedShift,
            nightsOfHistoryUsed = history.size,
            explanation = explanation
        )
    }

    private fun formatRange(profile: SleepProfile): String {
        val minH = profile.recommendedMinMinutes / 60
        val maxH = profile.recommendedMaxMinutes / 60
        return "$minH–$maxH h"
    }

    private fun formatDuration(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0L) "${h}h" else "${h}h ${m}m"
    }

    private companion object {
        const val DEFAULT_ONSET_ALLOWANCE_MINUTES = 20
        const val DEFAULT_WIND_DOWN_MINUTES = 45
        const val MIN_NIGHTS_FOR_HISTORY = 3
        const val HISTORY_WINDOW_DAYS = 14L
        const val MAX_SHIFT_PER_NIGHT = 20
        val TIME: java.time.format.DateTimeFormatter =
            java.time.format.DateTimeFormatter.ofPattern("h:mm a")
    }
}
