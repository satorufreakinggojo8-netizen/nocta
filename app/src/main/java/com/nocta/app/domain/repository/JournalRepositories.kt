package com.nocta.app.domain.repository

import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.domain.model.WindDownRun
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Optional daily sleep-hygiene journal, one row per sleep date. */
interface SleepJournalRepository {
    fun observeEntry(date: LocalDate): Flow<JournalEntry?>
    fun observeRecent(days: Int): Flow<List<JournalEntry>>

    /** Saves (or replaces) the entry for [JournalEntry.date]. Empty rows are deleted. */
    suspend fun save(entry: JournalEntry)
    suspend fun delete(date: LocalDate)
    suspend fun deleteAllJournal()

    /** Every entry, oldest first — used by export. */
    suspend fun getAll(): List<JournalEntry>

    /** Entries with at least one answer, newest first — used for pattern hints. */
    suspend fun getRecent(days: Int): List<JournalEntry>
}

/** Runs of the evening wind-down routine (real completed history, not a demo). */
interface WindDownRepository {
    /** The run in progress, if any. One at a time. */
    fun observeActiveRun(): Flow<WindDownRun?>

    /** Completed or dismissed runs, newest first. */
    fun observeRecentRuns(limit: Int): Flow<List<WindDownRun>>

    suspend fun activeRun(): WindDownRun?
    suspend fun startRun(plannedMinutes: Int, startedAtMillis: Long): WindDownRun
    suspend fun updateRun(run: WindDownRun)
    suspend fun deleteAllRuns()

    /** Completed runs, oldest first — used by data export. */
    suspend fun completedRunsForExport(): List<WindDownRun>
}
