package com.nocta.app.data.repository

import com.nocta.app.data.local.SleepTrackingDao
import com.nocta.app.data.local.SleepTrackingSessionEntity
import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.SleepTrackingError
import com.nocta.app.domain.model.SleepTrackingSession
import com.nocta.app.domain.repository.SleepTrackingRepository
import com.nocta.app.domain.usecase.SleepTrackingRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class SleepTrackingRepositoryImpl @Inject constructor(
    private val dao: SleepTrackingDao
) : SleepTrackingRepository {

    override fun observeActive(): Flow<SleepTrackingSession?> =
        dao.observeActive().map { it?.toDomain() }

    override fun observeLatestCompleted(): Flow<SleepTrackingSession?> =
        dao.observeLatestCompleted().map { it?.toDomain() }

    override suspend fun start(startedAt: Instant): SleepTrackingSession {
        // Guard against concurrent starts. Room has no partial unique index
        // for "at most one null-endedAt row"; the ViewModel serialises calls.
        val existing = dao.getActive()
        if (existing != null) throw SleepTrackingError.AlreadyTracking

        SleepTrackingRules.validateStart(startedAt)

        val entity = SleepTrackingSessionEntity(
            id = UUID.randomUUID().toString(),
            startedAtEpochMillis = startedAt.toEpochMilli(),
            endedAtEpochMillis = null,
            source = DataSource.ESTIMATED.name,
            synced = false
        )
        try {
            dao.upsert(entity)
        } catch (t: Throwable) {
            throw SleepTrackingError.StorageFailure(t)
        }
        return entity.toDomain()
    }

    override suspend fun end(endedAt: Instant): SleepTrackingSession {
        val active = dao.getActive() ?: throw SleepTrackingError.NoActiveSession
        val startedAt = Instant.ofEpochMilli(active.startedAtEpochMillis)

        SleepTrackingRules.validateEnd(startedAt, endedAt)

        try {
            dao.endSession(active.id, endedAt.toEpochMilli())
        } catch (t: Throwable) {
            throw SleepTrackingError.StorageFailure(t)
        }
        return active.copy(endedAtEpochMillis = endedAt.toEpochMilli()).toDomain()
    }

    override suspend fun discardActive() {
        dao.getActive()?.let { dao.delete(it.id) }
    }
}

private fun SleepTrackingSessionEntity.toDomain() = SleepTrackingSession(
    id = id,
    startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
    endedAt = endedAtEpochMillis?.let(Instant::ofEpochMilli),
    source = DataSource.valueOf(source),
    synced = synced
)
