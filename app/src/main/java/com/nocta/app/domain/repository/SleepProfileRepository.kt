package com.nocta.app.domain.repository

import com.nocta.app.domain.profile.SleepProfile
import kotlinx.coroutines.flow.Flow

interface SleepProfileRepository {
    fun observeProfile(): Flow<SleepProfile?>
    suspend fun saveAge(age: Int)
}
