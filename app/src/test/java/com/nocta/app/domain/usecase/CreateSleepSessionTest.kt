package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.MorningCheckIn
import com.nocta.app.domain.model.SleepDisturbance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CreateSleepSessionTest {

    private val createSleepSession = CreateSleepSession()

    private val trackingStart: Instant =
        Instant.parse("2026-10-20T23:30:00Z")
    private val trackingEnd: Instant =
        Instant.parse("2026-10-21T07:00:00Z")

    private fun checkIn(awakeMinutes: Int = 0) = MorningCheckIn(
        sleepQuality = 4,
        nightAwakenings = 1,
        awakeMinutes = awakeMinutes,
        morningEnergy = 4,
        disturbance = SleepDisturbance.NOISE,
        unusualEvent = null
    )

    @Test
    fun `carries awake minutes from the check-in into the session`() {
        val session = createSleepSession(
            trackingSessionId = "t1",
            trackingStart = trackingStart,
            trackingEnd = trackingEnd,
            checkIn = checkIn(awakeMinutes = 45)
        )

        assertEquals(45, session.awakeMinutes)
    }

    @Test
    fun `duration subtracts awake minutes from time in bed`() {
        val session = createSleepSession(
            trackingSessionId = "t1",
            trackingStart = trackingStart,
            trackingEnd = trackingEnd,
            checkIn = checkIn(awakeMinutes = 30)
        )

        val timeInBed = Duration.between(trackingStart, trackingEnd).toMinutes()
        assertEquals(timeInBed - 30, session.durationMinutes)
    }

    @Test
    fun `maps tracking window onto attempt and wake times`() {
        val zone = ZoneId.systemDefault()
        val session = createSleepSession(
            trackingSessionId = "t1",
            trackingStart = trackingStart,
            trackingEnd = trackingEnd,
            checkIn = checkIn()
        )

        assertEquals(
            LocalDateTime.ofInstant(trackingStart, zone),
            session.sleepAttemptTime
        )
        assertEquals(LocalDateTime.ofInstant(trackingStart, zone), session.estimatedSleepTime)
        assertEquals(LocalDateTime.ofInstant(trackingEnd, zone), session.wakeTime)
        assertEquals(LocalDate.ofInstant(trackingStart, zone), session.date)
        assertEquals(DataSource.SELF_REPORTED, session.source)
    }

    @Test
    fun `carries check-in answers through`() {
        val session = createSleepSession(
            trackingSessionId = "t1",
            trackingStart = trackingStart,
            trackingEnd = trackingEnd,
            checkIn = checkIn(awakeMinutes = 10)
        )

        assertEquals(4, session.sleepQuality)
        assertEquals(1, session.nightAwakenings)
        assertEquals(4, session.morningEnergy)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects end before start`() {
        createSleepSession(
            trackingSessionId = "t1",
            trackingStart = trackingEnd,
            trackingEnd = trackingStart,
            checkIn = checkIn()
        )
    }

    @Test
    fun `session id is the tracking session id for dedupe`() {
        val session = createSleepSession(
            trackingSessionId = "abc",
            trackingStart = trackingStart,
            trackingEnd = trackingEnd,
            checkIn = checkIn()
        )
        assertTrue(session.id == "abc")
    }
}
