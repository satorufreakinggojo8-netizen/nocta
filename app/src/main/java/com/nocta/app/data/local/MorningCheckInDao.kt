package com.nocta.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MorningCheckInDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkIn: MorningCheckInEntity)

    @Query("SELECT * FROM morning_check_ins WHERE trackingSessionId = :trackingSessionId LIMIT 1")
    fun observeForSession(
        trackingSessionId: String
    ): Flow<MorningCheckInEntity?>

    @Query("SELECT * FROM morning_check_ins WHERE trackingSessionId = :trackingSessionId LIMIT 1")
    suspend fun getForSession(
        trackingSessionId: String
    ): MorningCheckInEntity?
}
