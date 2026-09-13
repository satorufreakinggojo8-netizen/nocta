package com.nocta.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepTrackingDao {

    /** The single in-progress session, if any. Matches SleepDao.observeLastNight() style. */
    @Query(
        "SELECT * FROM sleep_tracking_sessions " +
            "WHERE endedAtEpochMillis IS NULL " +
            "ORDER BY startedAtEpochMillis DESC LIMIT 1"
    )
    fun observeActive(): Flow<SleepTrackingSessionEntity?>

    /** Latest completed session, most recent first. */
    @Query(
        "SELECT * FROM sleep_tracking_sessions " +
            "WHERE endedAtEpochMillis IS NOT NULL " +
            "ORDER BY endedAtEpochMillis DESC LIMIT 1"
    )
    fun observeLatestCompleted(): Flow<SleepTrackingSessionEntity?>

    @Query(
        "SELECT * FROM sleep_tracking_sessions " +
            "WHERE endedAtEpochMillis IS NULL " +
            "ORDER BY startedAtEpochMillis DESC LIMIT 1"
    )
    suspend fun getActive(): SleepTrackingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SleepTrackingSessionEntity)

    @Query("UPDATE sleep_tracking_sessions SET endedAtEpochMillis = :endedAtEpochMillis WHERE id = :id")
    suspend fun endSession(id: String, endedAtEpochMillis: Long)

    @Query("DELETE FROM sleep_tracking_sessions WHERE id = :id")
    suspend fun delete(id: String)
}
