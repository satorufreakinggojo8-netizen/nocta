package com.nocta.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted sleep tracking session. Additive table introduced in Room v2.
 *
 * `endedAtEpochMillis` is nullable — a null value IS the "currently tracking"
 * marker, which is what allows an active session to survive process death
 * and app restart without any in-memory state.
 */
@Entity(
    tableName = "sleep_tracking_sessions",
    indices = [Index(value = ["startedAtEpochMillis"])]
)
data class SleepTrackingSessionEntity(
    @PrimaryKey val id: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val source: String,          // DataSource.name
    val synced: Boolean = false
)
