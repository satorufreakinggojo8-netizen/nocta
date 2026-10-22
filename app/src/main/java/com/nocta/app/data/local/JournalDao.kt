package com.nocta.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Query("SELECT * FROM journal_entries WHERE dateEpochDay = :epochDay LIMIT 1")
    fun observeEntry(epochDay: Long): Flow<JournalEntryEntity?>

    @Query(
        "SELECT * FROM journal_entries WHERE dateEpochDay >= :sinceEpochDay " +
            "ORDER BY dateEpochDay DESC"
    )
    fun observeSince(sinceEpochDay: Long): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE dateEpochDay = :epochDay")
    suspend fun delete(epochDay: Long)

    @Query("SELECT * FROM journal_entries ORDER BY dateEpochDay ASC")
    suspend fun getAllForExport(): List<JournalEntryEntity>

    @Query(
        "SELECT * FROM journal_entries WHERE dateEpochDay >= :sinceEpochDay " +
            "ORDER BY dateEpochDay DESC"
    )
    suspend fun getSince(sinceEpochDay: Long): List<JournalEntryEntity>

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAll()
}

@Dao
interface WindDownDao {

    @Query(
        "SELECT * FROM wind_down_runs WHERE outcome = 'RUNNING' " +
            "ORDER BY startedAtEpochMillis DESC LIMIT 1"
    )
    fun observeActiveRun(): Flow<WindDownRunEntity?>

    @Query(
        "SELECT * FROM wind_down_runs WHERE outcome != 'RUNNING' " +
            "ORDER BY startedAtEpochMillis DESC LIMIT :limit"
    )
    fun observeRecentRuns(limit: Int): Flow<List<WindDownRunEntity>>

    @Query(
        "SELECT * FROM wind_down_runs WHERE outcome = 'RUNNING' " +
            "ORDER BY startedAtEpochMillis DESC LIMIT 1"
    )
    suspend fun getActiveRun(): WindDownRunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(run: WindDownRunEntity)

    @Query(
        "SELECT * FROM wind_down_runs WHERE outcome = 'COMPLETED' " +
            "ORDER BY startedAtEpochMillis DESC LIMIT :limit"
    )
    suspend fun getCompleted(limit: Int): List<WindDownRunEntity>

    @Query("SELECT * FROM wind_down_runs ORDER BY startedAtEpochMillis ASC")
    suspend fun getAllForExport(): List<WindDownRunEntity>

    @Query("DELETE FROM wind_down_runs")
    suspend fun deleteAll()
}
