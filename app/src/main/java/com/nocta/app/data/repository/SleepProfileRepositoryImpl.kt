package com.nocta.app.data.repository

import com.nocta.app.data.local.UserProfileDao
import com.nocta.app.data.local.UserProfileEntity
import com.nocta.app.domain.profile.SleepProfile
import com.nocta.app.domain.repository.SleepProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SleepProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : SleepProfileRepository {

    override fun observeProfile(): Flow<SleepProfile?> =
        userProfileDao.observeProfile().map { entity ->
            entity?.let { SleepProfile(age = it.age) }
        }

    override suspend fun saveAge(age: Int) {
        userProfileDao.saveProfile(
            UserProfileEntity(
                id = 1,
                age = age
            )
        )
    }
}
