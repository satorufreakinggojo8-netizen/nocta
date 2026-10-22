package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.domain.model.SleepSession
import javax.inject.Inject

/**
 * One factor that appears alongside better or worse sleep in the user's own
 * journal. Deliberately framed as an association: with a handful of nights and
 * no controlled conditions, nothing here can establish cause.
 */
data class JournalPattern(
    val id: String,
    val factorLabel: String,
    val nightsWithFactor: Int,
    val nightsWithoutFactor: Int,
    val averageMinutesWithFactor: Long,
    val averageMinutesWithoutFactor: Long,
    val differenceMinutes: Long
) {
    val hasMeaningfulDifference: Boolean
        get() = kotlin.math.abs(differenceMinutes) >= MEANINGFUL_MINUTES

    /** Plain-language sentence the UI shows verbatim under the pattern. */
    val description: String
        get() {
            val direction = if (differenceMinutes >= 0) "more" else "less"
            return "Nights with $factorLabel averaged ${kotlin.math.abs(differenceMinutes)} min " +
                "$direction sleep than nights without it " +
                "(${nightsWithFactor} vs ${nightsWithoutFactor} nights)."
        }

    companion object {
        const val MEANINGFUL_MINUTES = 15L
    }
}

/**
 * Finds possible associations between journal answers and real sleep duration.
 *
 * Guards against noise on purpose:
 * - a factor only appears once at least [MIN_GROUP_SIZE] nights exist on *both*
 *   sides (with and without the factor), so a single unusual night can't
 *   produce a "pattern";
 * - only nights that have both a valid sleep session and a journal answer are
 *   counted — nothing is imputed;
 * - differences under [JournalPattern.MEANINGFUL_MINUTES] are dropped.
 */
class JournalInsights @Inject constructor() {

    operator fun invoke(
        entries: List<JournalEntry>,
        sessions: List<SleepSession>
    ): List<JournalPattern> {
        val durationByDate = sessions
            .filter { it.hasUsableWindow }
            .groupBy { it.date }
            .mapValues { (_, list) -> list.last().durationMinutes }

        val usableEntries = entries.filter { it.hasAnyAnswer && it.date in durationByDate }
        if (usableEntries.isEmpty()) return emptyList()

        return buildList {
            addAll(pattern(usableEntries, durationByDate, "caffeine_late", "late caffeine") { entry ->
                val drinks = entry.caffeineDrinks ?: 0
                val time = entry.lastCaffeineTime ?: return@pattern null
                drinks > 0 && !time.isBefore(JournalEntry.LATE_CAFFEINE_CUTOFF)
            })
            addAll(pattern(usableEntries, durationByDate, "screens", "screens before bed") {
                it.screenUseBeforeBed
            })
            addAll(pattern(usableEntries, durationByDate, "stress", "high stress") {
                it.stressLevel?.let { level -> level >= HIGH_STRESS_LEVEL }
            })
            addAll(pattern(usableEntries, durationByDate, "nap", "naps over 20 min") {
                it.napMinutes?.let { minutes -> minutes >= MIN_NAP_MINUTES }
            })
            addAll(pattern(usableEntries, durationByDate, "routine", "a completed wind-down") {
                it.routineCompleted
            })
            addAll(pattern(usableEntries, durationByDate, "exercise_late", "late exercise") {
                entry ->
                val minutes = entry.exercisedMinutes ?: 0
                val time = entry.lastExerciseTime ?: return@pattern null
                minutes > 0 && time.hour >= LATE_EXERCISE_EARLIEST_HOUR
            })
        }
            .sortedByDescending { kotlin.math.abs(it.differenceMinutes) }
            .take(MAX_PATTERNS)
    }

    private fun pattern(
        entries: List<JournalEntry>,
        durationByDate: Map<java.time.LocalDate, Long>,
        id: String,
        label: String,
        predicate: (JournalEntry) -> Boolean?
    ): List<JournalPattern> {
        val withFactor = mutableListOf<Long>()
        val withoutFactor = mutableListOf<Long>()
        entries.forEach { entry ->
            val duration = durationByDate[entry.date] ?: return@forEach
            when (predicate(entry)) {
                true -> withFactor += duration
                false -> withoutFactor += duration
                null -> Unit // not answered — excluded, never guessed
            }
        }
        if (withFactor.size < MIN_GROUP_SIZE || withoutFactor.size < MIN_GROUP_SIZE) {
            return emptyList()
        }
        val avgWith = withFactor.average().toLong()
        val avgWithout = withoutFactor.average().toLong()
        val difference = avgWith - avgWithout
        if (kotlin.math.abs(difference) < JournalPattern.MEANINGFUL_MINUTES) return emptyList()
        return listOf(
            JournalPattern(
                id = id,
                factorLabel = label,
                nightsWithFactor = withFactor.size,
                nightsWithoutFactor = withoutFactor.size,
                averageMinutesWithFactor = avgWith,
                averageMinutesWithoutFactor = avgWithout,
                differenceMinutes = difference
            )
        )
    }

    private companion object {
        const val MIN_GROUP_SIZE = 3
        const val MAX_PATTERNS = 3
        const val HIGH_STRESS_LEVEL = 4
        const val MIN_NAP_MINUTES = 20
        const val LATE_EXERCISE_EARLIEST_HOUR = 20
    }
}
