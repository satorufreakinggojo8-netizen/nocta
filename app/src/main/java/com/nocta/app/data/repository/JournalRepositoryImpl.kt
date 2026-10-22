package com.nocta.app.data.repository

import com.nocta.app.data.local.JournalDao
import com.nocta.app.data.local.JournalEntryEntity
import com.nocta.app.data.local.WindDownDao
import com.nocta.app.data.local.WindDownRunEntity
import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.domain.model.WindDownOutcome
import com.nocta.app.domain.model.WindDownRun
import com.nocta.app.domain.repository.SleepJournalRepository
import com.nocta.app.domain.repository.WindDownRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

class SleepJournalRepositoryImpl @Inject constructor(
    private val dao: JournalDao
) : SleepJournalRepository {

    override fun observeEntry(date: LocalDate): Flow<JournalEntry?> =
        dao.observeEntry(date.toEpochDay()).map { it?.toDomain() }

    override fun observeRecent(days: Int): Flow<List<JournalEntry>> {
        val since = LocalDate.now().minusDays(days.toLong() - 1).toEpochDay()
        return dao.observeSince(since).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun save(entry: JournalEntry) {
        val normalized = entry.normalized()
        // A row where the user answered nothing is not a record — remove it so
        // the database never claims a journal day exists with no content.
        if (!normalized.hasAnyAnswer) {
            dao.delete(normalized.date.toEpochDay())
            return
        }
        dao.upsert(normalized.toEntity())
    }

    override suspend fun delete(date: LocalDate) {
        dao.delete(date.toEpochDay())
    }

    override suspend fun deleteAllJournal() {
        dao.deleteAll()
    }

    override suspend fun getAll(): List<JournalEntry> =
        dao.getAllForExport().map { it.toDomain() }

    override suspend fun getRecent(days: Int): List<JournalEntry> {
        val since = LocalDate.now().minusDays(days.toLong() - 1).toEpochDay()
        return dao.getSince(since)
            .map { it.toDomain() }
            .filter { it.hasAnyAnswer }
            .sortedByDescending { it.date }
    }
}

class WindDownRepositoryImpl @Inject constructor(
    private val dao: WindDownDao
) : WindDownRepository {

    override fun observeActiveRun(): Flow<WindDownRun?> =
        dao.observeActiveRun().map { it?.toDomain() }

    override fun observeRecentRuns(limit: Int): Flow<List<WindDownRun>> =
        dao.observeRecentRuns(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun activeRun(): WindDownRun? = dao.getActiveRun()?.toDomain()

    override suspend fun startRun(plannedMinutes: Int, startedAtMillis: Long): WindDownRun {
        val run = WindDownRun(
            id = UUID.randomUUID().toString(),
            date = java.time.Instant.ofEpochMilli(startedAtMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate(),
            plannedMinutes = plannedMinutes.coerceIn(MIN_PLANNED_MINUTES, MAX_PLANNED_MINUTES),
            startedAtMillis = startedAtMillis
        )
        dao.upsert(run.toEntity())
        return run
    }

    override suspend fun updateRun(run: WindDownRun) {
        dao.upsert(run.toEntity())
    }

    override suspend fun deleteAllRuns() {
        dao.deleteAll()
    }

    override suspend fun completedRunsForExport(): List<WindDownRun> =
        dao.getAllForExport()
            .map { it.toDomain() }
            .filter { it.outcome == WindDownOutcome.COMPLETED }

    private companion object {
        const val MIN_PLANNED_MINUTES = 5
        const val MAX_PLANNED_MINUTES = 180
    }
}

private fun JournalEntryEntity.toDomain() = JournalEntry(
    date = LocalDate.ofEpochDay(dateEpochDay),
    restfulness = restfulness,
    stressLevel = stressLevel,
    napMinutes = napMinutes,
    caffeineDrinks = caffeineDrinks,
    lastCaffeineTime = lastCaffeineMinutes?.let { minutesOfDay(it) },
    exercisedMinutes = exercisedMinutes,
    lastExerciseTime = lastExerciseMinutes?.let { minutesOfDay(it) },
    screenUseBeforeBed = screenUseBeforeBed,
    routineCompleted = routineCompleted,
    notes = notes
)

private fun JournalEntry.toEntity() = JournalEntryEntity(
    dateEpochDay = date.toEpochDay(),
    restfulness = restfulness,
    stressLevel = stressLevel,
    napMinutes = napMinutes,
    caffeineDrinks = caffeineDrinks,
    lastCaffeineMinutes = lastCaffeineTime?.let { it.hour * 60 + it.minute },
    exercisedMinutes = exercisedMinutes,
    lastExerciseMinutes = lastExerciseTime?.let { it.hour * 60 + it.minute },
    screenUseBeforeBed = screenUseBeforeBed,
    routineCompleted = routineCompleted,
    notes = notes,
    updatedAtEpochMillis = System.currentTimeMillis()
)

private fun minutesOfDay(minutes: Int): LocalTime =
    LocalTime.of((minutes / 60).coerceIn(0, 23), (minutes % 60).coerceIn(0, 59))

private fun WindDownRunEntity.toDomain() = WindDownRun(
    id = id,
    date = LocalDate.ofEpochDay(dateEpochDay),
    plannedMinutes = plannedMinutes,
    startedAtMillis = startedAtEpochMillis,
    accumulatedPauseMillis = accumulatedPauseMillis,
    pausedAtMillis = pausedAtEpochMillis,
    endedAtMillis = endedAtEpochMillis,
    outcome = runCatching { WindDownOutcome.valueOf(outcome) }
        .getOrDefault(WindDownOutcome.DISMISSED),
    completedStepIds = completedStepIds
        .split(STEP_SEPARATOR)
        .filter { it.isNotBlank() }
        .toSet()
)

private fun WindDownRun.toEntity() = WindDownRunEntity(
    id = id,
    dateEpochDay = date.toEpochDay(),
    plannedMinutes = plannedMinutes,
    startedAtEpochMillis = startedAtMillis,
    accumulatedPauseMillis = accumulatedPauseMillis,
    pausedAtEpochMillis = pausedAtMillis,
    endedAtEpochMillis = endedAtMillis,
    outcome = outcome.name,
    completedStepIds = completedStepIds.sorted().joinToString(STEP_SEPARATOR)
)

private const val STEP_SEPARATOR = "|"
