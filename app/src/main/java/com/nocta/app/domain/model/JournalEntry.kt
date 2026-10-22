package com.nocta.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * One day's optional sleep-hygiene journal. Every field is optional: a user may
 * answer only caffeine, or nothing at all, and that must never look like a
 * zero. The entry is keyed by the *sleep date* (the night's `SleepSession.date`),
 * so a journal row and its night line up for correlation without a join table.
 *
 * Free-text notes are stored locally only. They are never sent to the coach
 * context — see BuildCoachContext.
 */
data class JournalEntry(
    val date: LocalDate,
    val restfulness: Int? = null,        // 1 (wrecked) .. 5 (rested)
    val stressLevel: Int? = null,        // 1 (calm) .. 5 (very stressed)
    val napMinutes: Int? = null,
    val caffeineDrinks: Int? = null,
    val lastCaffeineTime: LocalTime? = null,
    val exercisedMinutes: Int? = null,
    val lastExerciseTime: LocalTime? = null,
    val screenUseBeforeBed: Boolean? = null,
    val routineCompleted: Boolean? = null,
    val notes: String? = null
) {
    /** True when the user actually answered something; an empty row is not saved. */
    val hasAnyAnswer: Boolean
        get() = restfulness != null || stressLevel != null || napMinutes != null ||
            caffeineDrinks != null || lastCaffeineTime != null ||
            exercisedMinutes != null || lastExerciseTime != null ||
            screenUseBeforeBed != null || routineCompleted != null ||
            !notes.isNullOrBlank()

    /**
     * Clamps out-of-range values instead of trusting the UI, trims notes and
     * turns blank notes into null so "empty" is representable.
     */
    fun normalized(): JournalEntry = copy(
        restfulness = restfulness?.coerceIn(1, 5),
        stressLevel = stressLevel?.coerceIn(1, 5),
        napMinutes = napMinutes?.coerceIn(0, MAX_NAP_MINUTES),
        caffeineDrinks = caffeineDrinks?.coerceIn(0, MAX_CAFFEINE_DRINKS),
        exercisedMinutes = exercisedMinutes?.coerceIn(0, MAX_EXERCISE_MINUTES),
        notes = notes
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.take(MAX_NOTES_LENGTH)
    )

    companion object {
        const val MAX_NAP_MINUTES = 12 * 60
        const val MAX_CAFFEINE_DRINKS = 12
        const val MAX_EXERCISE_MINUTES = 6 * 60
        const val MAX_NOTES_LENGTH = 2000

        /** Caffeine after this local time is treated as "late" for pattern hints. */
        val LATE_CAFFEINE_CUTOFF: LocalTime = LocalTime.of(14, 0)

        /** Exercise within this many minutes of bedtime is treated as "late". */
        const val LATE_EXERCISE_WINDOW_MINUTES = 180
    }
}
