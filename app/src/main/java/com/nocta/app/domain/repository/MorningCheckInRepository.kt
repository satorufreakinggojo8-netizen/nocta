package com.nocta.app.domain.repository

import com.nocta.app.domain.model.MorningCheckIn
import kotlinx.coroutines.flow.Flow

interface MorningCheckInRepository {

    fun observeForSession(
        trackingSessionId: String
    ): Flow<MorningCheckIn?>

    suspend fun getForSession(
        trackingSessionId: String
    ): MorningCheckIn?

    suspend fun save(
        trackingSessionId: String,
        checkIn: MorningCheckIn
    )
}
