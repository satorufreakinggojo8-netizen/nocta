package com.nocta.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of sleep sessions (offline-first: writes land here immediately,
 * then sync to the backend; reads never block on network).
 */
@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey val id: String,
    val dateEpochDay: Long,
    val bedtimeEpochMinute: Long,
    val sleepAttemptEpochMinute: Long,
    val estimatedSleepEpochMinute: Long,
    val wakeEpochMinute: Long,
    val nightAwakenings: Int,
    val sleepQuality: Int,
    val morningEnergy: Int,
    val source: String,          // DataSource.name
    val synced: Boolean = false  // true once backend has confirmed persistence
)

@Entity(tableName = "ai_messages")
data class AiMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,            // MessageRole.name
    val content: String,
    val createdAtEpochMillis: Long
)
