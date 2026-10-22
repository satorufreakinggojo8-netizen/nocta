package com.nocta.app.domain.usecase

import com.nocta.app.domain.repository.MorningCheckInRepository
import com.nocta.app.domain.repository.SleepJournalRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.repository.SleepTrackingRepository
import com.nocta.app.domain.repository.WindDownRepository
import javax.inject.Inject

/**
 * Deletes every local sleep-related record in one place, so "delete all my
 * data" cannot leave orphaned rows in one table while clearing another.
 *
 * Coach messages are intentionally excluded: they have their own confirmation
 * (ProfileScreen) so a user can keep their conversation while clearing logs.
 * Settings are not personal sleep records and are left intact.
 */
class DeleteAllSleepData @Inject constructor(
    private val sleepRepository: SleepRepository,
    private val sleepTrackingRepository: SleepTrackingRepository,
    private val morningCheckInRepository: MorningCheckInRepository,
    private val journalRepository: SleepJournalRepository,
    private val windDownRepository: WindDownRepository
) {
    suspend operator fun invoke() {
        // Order matters only for clarity: each repository owns its own table.
        sleepRepository.deleteAllSleepData()
        sleepTrackingRepository.deleteAllTrackingData()
        morningCheckInRepository.deleteAllCheckIns()
        journalRepository.deleteAllJournal()
        windDownRepository.deleteAllRuns()
    }
}
