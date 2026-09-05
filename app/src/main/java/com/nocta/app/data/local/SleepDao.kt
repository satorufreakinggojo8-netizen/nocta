package com.nocta.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {

    @Query("SELECT * FROM sleep_sessions ORDER BY dateEpochDay DESC LIMIT 1")
    fun observeLastNight(): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC")
    fun observeSince(sinceEpochDay: Long): Flow<List<SleepSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SleepSessionEntity)

    @Query("SELECT * FROM sleep_sessions WHERE synced = 0")
    suspend fun getUnsynced(): List<SleepSessionEntity>

    @Query("UPDATE sleep_sessions SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface AiMessageDao {

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAtEpochMillis ASC")
    fun observeConversation(conversationId: String): Flow<List<AiMessageEntity>>

    @Insert
    suspend fun insert(message: AiMessageEntity)
}
