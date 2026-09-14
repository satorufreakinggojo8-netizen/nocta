package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.MorningCheckIn
import com.nocta.app.domain.model.SleepSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class CreateSleepSession @Inject constructor() {

    operator fun invoke(
        trackingSessionId: String,
        trackingStart: Instant,
        trackingEnd: Instant,
        checkIn: MorningCheckIn
    ): SleepSession {
        require(!trackingEnd.isBefore(trackingStart))

        val zone = ZoneId.systemDefault()

        val sleepAttemptTime =
            trackingStart.atZone(zone).toLocalDateTime()

        val wakeTime =
            trackingEnd.atZone(zone).toLocalDateTime()

        return SleepSession(
            id = trackingSessionId,
            date = LocalDate.ofInstant(trackingStart, zone),
            bedtime = sleepAttemptTime,
            sleepAttemptTime = sleepAttemptTime,
            estimatedSleepTime = sleepAttemptTime,
            wakeTime = wakeTime,
            nightAwakenings = checkIn.nightAwakenings,
            sleepQuality = checkIn.sleepQuality,
            morningEnergy = checkIn.morningEnergy,
            source = DataSource.SELF_REPORTED
        )
    }
}
