package com.nocta.app.domain.repository

import com.nocta.app.domain.model.SleepScoreCardMode
import kotlinx.coroutines.flow.Flow

/**
 * Lightweight user-interface preferences (display choices rather than sleep
 * data). Backed by SharedPreferences on the data layer; see
 * docs/01-ARCHITECTURE.md — sleep data itself stays in Room.
 */
interface SettingsRepository {
    fun observeSleepScoreCardMode(): Flow<SleepScoreCardMode>
    suspend fun setSleepScoreCardMode(mode: SleepScoreCardMode)
}
