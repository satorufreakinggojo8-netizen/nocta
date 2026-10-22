package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.domain.model.MorningCheckIn
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.model.WindDownRun
import com.nocta.app.domain.profile.SleepProfile
import javax.inject.Inject

/** Everything Vela is allowed to know, assembled from real stored records. */
data class CoachContextInput(
    val prompt: String,
    /** Newest first, already filtered to valid nights. */
    val sessions: List<SleepSession> = emptyList(),
    val profile: SleepProfile? = null,
    val journalEntries: List<JournalEntry> = emptyList(),
    val recentCheckIns: List<MorningCheckIn> = emptyList(),
    val completedWindDownRuns: List<WindDownRun> = emptyList(),
    val score: SleepScoreBreakdown? = null
)

/**
 * The context block attached to a coach message.
 *
 * `missing` is not decoration: it is the list of things Vela must say are
 * absent instead of inventing them. Notes text from the journal is never
 * included — only derived counts — so free-text stays on the device.
 */
data class CoachContext(
    val facts: List<String>,
    val missing: List<String>
) {
    val hasSleepData: Boolean get() = facts.any { it.startsWith(NIGHTS_PREFIX) }

    /**
     * Prompt actually sent to the backend: facts, gaps, then the user's words.
     * Without a single logged night there is no sleep context to send — only the
     * list of what is missing, which is what Vela must talk about.
     */
    fun envelope(prompt: String): String = buildString {
        if (hasSleepData && facts.isNotEmpty()) {
            append("[Sleep Context: ")
            append(facts.joinToString("; "))
            append("]\n")
        }
        if (missing.isNotEmpty()) {
            append("[Missing Data: ")
            append(missing.joinToString("; "))
            append(" — say so plainly instead of estimating]\n")
        }
        append(prompt)
    }

    companion object {
        const val NIGHTS_PREFIX = "Nights logged"
    }
}

/**
 * Builds Vela's context from the user's own data only. Deterministic and
 * dependency-free so it can be unit-tested for both "rich history" and
 * "nothing logged yet" cases.
 */
class BuildCoachContext @Inject constructor() {

    operator fun invoke(input: CoachContextInput): CoachContext {
        val nights = input.sessions
            .filter { it.hasUsableWindow }
            .sortedByDescending { it.date }
            .take(MAX_NIGHTS)

        val facts = mutableListOf<String>()
        val missing = mutableListOf<String>()

        if (nights.isEmpty()) {
            missing += "No sleep nights logged yet"
        } else {
            facts += "${CoachContext.NIGHTS_PREFIX}: ${nights.size} (last ${nights.size} valid nights)"
            facts += "Average asleep: ${formatMinutes(nights.map { it.durationMinutes }.average().toLong())}"
            nights.lastOrNull()?.let { latest ->
                facts += "Most recent night: ${latest.date}, " +
                    "${formatMinutes(latest.durationMinutes)} asleep, source ${latest.source.name.lowercase()}"
            }

            val quality = nights.mapNotNull { it.sleepQuality.takeIf { q -> q in 1..5 } }
            if (quality.isNotEmpty()) {
                facts += "Self-reported quality: ${"%.1f".format(quality.average())}/5"
            } else {
                missing += "No self-reported quality ratings"
            }

            val bedtimes = nights.map { it.bedtime.hour * 60.0 + it.bedtime.minute }
            val deviation = CircularClockStats.meanAbsoluteDeviation(bedtimes)
            facts += "Bedtime regularity: ±${deviation.toInt()} min from the average clock time"

            if (nights.size < MIN_NIGHTS_FOR_PATTERNS) {
                missing += "Fewer than $MIN_NIGHTS_FOR_PATTERNS nights logged, so no trend can be claimed"
            }
        }

        val profile = input.profile
        if (profile != null) {
            facts += "Age ${profile.age}; target ${profile.recommendedSleepLabel}"
        } else {
            missing += "Age not set, so age-based targets are unavailable"
        }

        input.score?.takeIf { it.hasData }?.let { score ->
            facts += "Sleep score ${score.overall} (${score.label}) over ${score.trackedNights} nights"
            facts += "Weakest factor: ${weakestFactor(score)}"
        }

        val answeredJournal = input.journalEntries.filter { it.hasAnyAnswer }
        if (answeredJournal.isEmpty()) {
            missing += "No journal entries"
        } else {
            facts += "Journal days answered: ${answeredJournal.size}"
            lateCaffeineCount(answeredJournal)?.let {
                facts += "Late caffeine (after 14:00) on $it of ${answeredJournal.size} journal days"
            }
            highStressCount(answeredJournal)?.let {
                facts += "High stress (4-5/5) on $it of ${answeredJournal.size} journal days"
            }
            screenUseCount(answeredJournal)?.let {
                facts += "Screens before bed on $it of ${answeredJournal.size} journal days"
            }
            napCount(answeredJournal)?.let {
                facts += "Naps over 20 min on $it of ${answeredJournal.size} journal days"
            }
            routineCount(answeredJournal)?.let {
                facts += "Wind-down completed on $it of ${answeredJournal.size} journal days"
            }
        }

        if (input.recentCheckIns.isNotEmpty()) {
            facts += "Morning check-ins available: ${input.recentCheckIns.size} (awakenings and awake time are self-reported)"
        }

        if (input.completedWindDownRuns.isEmpty()) {
            missing += "No completed wind-down routine runs"
        } else {
            facts += "Completed wind-down runs: ${input.completedWindDownRuns.size}"
        }

        facts += "Data is self-reported or start/stop estimated — no sleep stages are measured"

        return CoachContext(facts = facts, missing = missing)
    }

    private fun weakestFactor(score: SleepScoreBreakdown): String =
        listOf(
            "duration" to score.durationScore,
            "bedtime consistency" to score.consistencyScore,
            "schedule timing" to score.timingScore,
            "self-reported quality" to score.qualityScore,
            "awakenings" to score.routineScore
        ).minByOrNull { it.second }?.first ?: "unknown"

    private fun lateCaffeineCount(entries: List<JournalEntry>): Int? {
        val answered = entries.filter {
            (it.caffeineDrinks ?: 0) > 0 && it.lastCaffeineTime != null
        }
        if (answered.isEmpty()) return null
        return answered.count {
            !it.lastCaffeineTime!!.isBefore(JournalEntry.LATE_CAFFEINE_CUTOFF)
        }
    }

    private fun highStressCount(entries: List<JournalEntry>): Int? {
        val answered = entries.filter { it.stressLevel != null }
        if (answered.isEmpty()) return null
        return answered.count { (it.stressLevel ?: 0) >= HIGH_STRESS_LEVEL }
    }

    private fun screenUseCount(entries: List<JournalEntry>): Int? {
        val answered = entries.filter { it.screenUseBeforeBed != null }
        if (answered.isEmpty()) return null
        return answered.count { it.screenUseBeforeBed == true }
    }

    private fun napCount(entries: List<JournalEntry>): Int? {
        val answered = entries.filter { it.napMinutes != null }
        if (answered.isEmpty()) return null
        return answered.count { (it.napMinutes ?: 0) >= MIN_NAP_MINUTES }
    }

    private fun routineCount(entries: List<JournalEntry>): Int? {
        val answered = entries.filter { it.routineCompleted != null }
        if (answered.isEmpty()) return null
        return answered.count { it.routineCompleted == true }
    }

    private fun formatMinutes(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0L) "${h}h" else "${h}h ${m}m"
    }

    private companion object {
        const val MAX_NIGHTS = 30
        const val MIN_NIGHTS_FOR_PATTERNS = 3
        const val HIGH_STRESS_LEVEL = 4
        const val MIN_NAP_MINUTES = 20
    }
}
