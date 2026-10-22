package com.nocta.app.domain.model

import com.nocta.app.domain.profile.SleepProfile
import com.nocta.app.domain.usecase.CalculateSleepScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Regression tests for the dashboard screenshot that showed a Sleep Score of
 * 0 with a "0h 0m" night: a stored record whose wake time equals its sleep
 * time is not a night of sleep, must not be presented as one, and must not
 * produce a numeric score.
 */
class SleepScoreAvailabilityTest {

    private val calculateScore = CalculateSleepScore()

    private fun session(
        id: String,
        sleepStart: LocalDateTime,
        wake: LocalDateTime,
        awakeMinutes: Int = 0
    ) = SleepSession(
        id = id,
        date = sleepStart.toLocalDate(),
        bedtime = sleepStart,
        sleepAttemptTime = sleepStart,
        estimatedSleepTime = sleepStart,
        wakeTime = wake,
        nightAwakenings = 0,
        sleepQuality = 3,
        morningEnergy = 3,
        source = DataSource.SELF_REPORTED,
        awakeMinutes = awakeMinutes
    )

    @Test
    fun `zero length session has no usable window`() {
        val midnight = LocalDateTime.of(2026, 10, 13, 7, 46)
        val broken = session("broken", midnight, midnight)

        assertFalse(broken.hasUsableWindow)
        assertEquals(0L, broken.durationMinutes)
    }

    @Test
    fun `awake time longer than the night leaves no usable window`() {
        val start = LocalDateTime.of(2026, 10, 12, 23, 0)
        val broken = session("broken", start, start.plusHours(1), awakeMinutes = 90)

        assertFalse(broken.hasUsableWindow)
    }

    @Test
    fun `real night has a usable window`() {
        val start = LocalDateTime.of(2026, 10, 12, 23, 0)
        val real = session("real", start, start.plusHours(7).plusMinutes(30))

        assertTrue(real.hasUsableWindow)
        assertEquals(450L, real.durationMinutes)
    }

    @Test
    fun `score reports no data when every stored night is unusable`() {
        val midnight = LocalDateTime.of(2026, 10, 13, 7, 46)
        val breakdown = calculateScore(
            listOf(session("broken", midnight, midnight)),
            SleepProfile(age = 30)
        )

        assertFalse(breakdown.hasData)
        assertEquals(0, breakdown.trackedNights)
        assertEquals(0, breakdown.overall)
    }

    @Test
    fun `score reports data and counts only valid nights`() {
        val start = LocalDateTime.of(2026, 10, 5, 23, 0)
        val valid = (0..4).map { day ->
            val evening = start.plusDays(day.toLong())
            session("valid-$day", evening, evening.plusHours(8))
        }
        val midnight = LocalDateTime.of(2026, 10, 13, 7, 46)
        val broken = session("broken", midnight, midnight)

        val breakdown = calculateScore(valid + broken, SleepProfile(age = 30))

        assertTrue(breakdown.hasData)
        assertEquals(5, breakdown.trackedNights)
        assertTrue(breakdown.overall in 1..100)
    }
}
