package com.nocta.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Daily journal row, keyed by the sleep date's epoch day so a row and its
 * `SleepSessionEntity` share one key. All answers are nullable: a null means
 * "not answered", which is different from a zero answer.
 */
@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val dateEpochDay: Long,
    val restfulness: Int?,
    val stressLevel: Int?,
    val napMinutes: Int?,
    val caffeineDrinks: Int?,
    /** Minutes since local midnight of the last caffeine; null = not answered. */
    val lastCaffeineMinutes: Int?,
    val exercisedMinutes: Int?,
    /** Minutes since local midnight of the last exercise. */
    val lastExerciseMinutes: Int?,
    val screenUseBeforeBed: Boolean?,
    val routineCompleted: Boolean?,
    val notes: String?,
    val updatedAtEpochMillis: Long
)

/** One wind-down routine run. Durable so an in-progress run survives restart. */
@Entity(
    tableName = "wind_down_runs",
    indices = [Index(value = ["startedAtEpochMillis"])]
)
data class WindDownRunEntity(
    @PrimaryKey val id: String,
    val dateEpochDay: Long,
    val plannedMinutes: Int,
    val startedAtEpochMillis: Long,
    val accumulatedPauseMillis: Long,
    val pausedAtEpochMillis: Long?,
    val endedAtEpochMillis: Long?,
    val outcome: String,
    /** Enabled step ids the user ticked during this run, '|' separated. */
    val completedStepIds: String
)
