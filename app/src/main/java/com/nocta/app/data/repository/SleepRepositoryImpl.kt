package com.nocta.app.data.repository

import com.nocta.app.data.local.SleepDao
import com.nocta.app.data.local.SleepSessionEntity
import com.nocta.app.data.remote.NoctaApi
import com.nocta.app.data.remote.SleepSessionDto
import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.usecase.CalculateSleepScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class SleepRepositoryImpl @Inject constructor(
    private val sleepDao: SleepDao,
    private val api: NoctaApi,
    private val calculateSleepScore: CalculateSleepScore
) : SleepRepository {

    override fun observeLastNight(): Flow<SleepSession?> =
        sleepDao.observeLastNight().map { it?.toDomain() }

    override fun observeRecentSessions(days: Int): Flow<List<SleepSession>> {
        val since = LocalDate.now().minusDays(days.toLong()).toEpochDay()
        return sleepDao.observeSince(since).map { list -> list.map { it.toDomain() } }
    }

    override fun computeScore(sessions: List<SleepSession>): SleepScoreBreakdown =
        calculateSleepScore(sessions)

    override suspend fun logSession(session: SleepSession) {
        // Write locally first (offline-first) — sync happens in syncPending().
        sleepDao.upsert(session.toEntity())
    }

    override suspend fun syncPending() {
        val unsynced = sleepDao.getUnsynced()
        unsynced.forEach { entity ->
            runCatching {
                val response = api.upsertSession(entity.toDto())
                if (response.isSuccessful) sleepDao.markSynced(entity.id)
            }
            // Failures are silently retried on the next syncPending() call —
            // no user-facing error for a background sync, per offline-handling requirement.
        }
    }
}

private fun SleepSessionEntity.toDomain() = SleepSession(
    id = id,
    date = LocalDate.ofEpochDay(dateEpochDay),
    bedtime = epochMinuteToDateTime(bedtimeEpochMinute),
    sleepAttemptTime = epochMinuteToDateTime(sleepAttemptEpochMinute),
    estimatedSleepTime = epochMinuteToDateTime(estimatedSleepEpochMinute),
    wakeTime = epochMinuteToDateTime(wakeEpochMinute),
    nightAwakenings = nightAwakenings,
    sleepQuality = sleepQuality,
    morningEnergy = morningEnergy,
    source = DataSource.valueOf(source)
)

private fun SleepSession.toEntity() = SleepSessionEntity(
    id = id,
    dateEpochDay = date.toEpochDay(),
    bedtimeEpochMinute = dateTimeToEpochMinute(bedtime),
    sleepAttemptEpochMinute = dateTimeToEpochMinute(sleepAttemptTime),
    estimatedSleepEpochMinute = dateTimeToEpochMinute(estimatedSleepTime),
    wakeEpochMinute = dateTimeToEpochMinute(wakeTime),
    nightAwakenings = nightAwakenings,
    sleepQuality = sleepQuality,
    morningEnergy = morningEnergy,
    source = source.name,
    synced = false
)

private fun SleepSessionEntity.toDto() = SleepSessionDto(
    id = id,
    date = LocalDate.ofEpochDay(dateEpochDay).toString(),
    bedtime = epochMinuteToDateTime(bedtimeEpochMinute).toString(),
    sleepAttemptTime = epochMinuteToDateTime(sleepAttemptEpochMinute).toString(),
    estimatedSleepTime = epochMinuteToDateTime(estimatedSleepEpochMinute).toString(),
    wakeTime = epochMinuteToDateTime(wakeEpochMinute).toString(),
    nightAwakenings = nightAwakenings,
    sleepQuality = sleepQuality,
    morningEnergy = morningEnergy,
    source = source
)

private fun epochMinuteToDateTime(epochMinute: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochSecond(epochMinute * 60), ZoneOffset.UTC)

private fun dateTimeToEpochMinute(dateTime: LocalDateTime): Long =
    dateTime.toEpochSecond(ZoneOffset.UTC) / 60
