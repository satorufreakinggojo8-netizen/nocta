package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.SleepDebt
import com.nocta.app.domain.model.SleepDebtStatus
import com.nocta.app.domain.model.SleepSession
import javax.inject.Inject

/**
 * Estimates accumulated sleep debt from recorded nights only.
 *
 * Null when there is nothing recorded: "you're well rested" would be a claim
 * the data cannot support. Missing nights are not counted as zero sleep.
 */
class EstimateSleepDebt @Inject constructor() {

    operator fun invoke(
        recentSessions: List<SleepSession>,
        targetMinutes: Int = DEFAULT_TARGET_MINUTES
    ): SleepDebt? {
        if (recentSessions.isEmpty()) return null

        val averageMinutes = recentSessions.map { it.durationMinutes }.average()
        val deficitPerNight = (targetMinutes - averageMinutes).coerceAtLeast(0.0)
        val approximateDeficit = (deficitPerNight * recentSessions.size).toInt()

        val status = when {
            approximateDeficit < SLIGHTLY_BEHIND_THRESHOLD -> SleepDebtStatus.WELL_RESTED
            approximateDeficit < CATCHING_UP_THRESHOLD -> SleepDebtStatus.SLIGHTLY_BEHIND
            else -> SleepDebtStatus.CATCHING_UP
        }

        return SleepDebt(status = status, approximateDeficitMinutes = approximateDeficit)
    }

    private companion object {
        const val DEFAULT_TARGET_MINUTES = 8 * 60
        const val SLIGHTLY_BEHIND_THRESHOLD = 60
        const val CATCHING_UP_THRESHOLD = 300
    }
}
