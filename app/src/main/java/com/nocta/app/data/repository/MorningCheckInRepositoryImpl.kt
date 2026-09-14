package com.nocta.app.data.repository

import com.nocta.app.data.local.MorningCheckInDao
import com.nocta.app.data.local.MorningCheckInEntity
import com.nocta.app.domain.model.MorningCheckIn
import com.nocta.app.domain.model.SleepDisturbance
import com.nocta.app.domain.repository.MorningCheckInRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MorningCheckInRepositoryImpl @Inject constructor(
    private val dao: MorningCheckInDao
) : MorningCheckInRepository {

    override fun observeForSession(
        trackingSessionId: String
    ): Flow<MorningCheckIn?> =
        dao.observeForSession(trackingSessionId)
            .map { it?.toDomain() }

    override suspend fun getForSession(
        trackingSessionId: String
    ): MorningCheckIn? =
        dao.getForSession(trackingSessionId)?.toDomain()

    override suspend fun save(
        trackingSessionId: String,
        checkIn: MorningCheckIn
    ) {
        dao.upsert(
            MorningCheckInEntity(
                trackingSessionId = trackingSessionId,
                sleepQuality = checkIn.sleepQuality,
                nightAwakenings = checkIn.nightAwakenings,
                awakeMinutes = checkIn.awakeMinutes,
                morningEnergy = checkIn.morningEnergy,
                disturbance = checkIn.disturbance.name,
                unusualEvent = checkIn.unusualEvent
            )
        )
    }
}

private fun MorningCheckInEntity.toDomain() =
    MorningCheckIn(
        sleepQuality = sleepQuality,
        nightAwakenings = nightAwakenings,
        awakeMinutes = awakeMinutes,
        morningEnergy = morningEnergy,
        disturbance = runCatching {
            SleepDisturbance.valueOf(disturbance)
        }.getOrDefault(SleepDisturbance.OTHER),
        unusualEvent = unusualEvent
    )
