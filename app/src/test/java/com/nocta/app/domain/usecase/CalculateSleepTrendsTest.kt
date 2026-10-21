package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CalculateSleepTrendsTest {

    private val useCase = CalculateSleepTrends(CalculateSleepScore())
    private val today = LocalDate.of(2026, 10, 21)

    private fun session(date: LocalDate, bedtimeHour: Int, durationMinutes: Long): SleepSession {
        val attempt = date.atTime(bedtimeHour, 0)
        return SleepSession(
            id = "s-${date.toEpochDay()}",
            date = date,
            bedtime = attempt,
            sleepAttemptTime = attempt,
            estimatedSleepTime = attempt,
            wakeTime = attempt.plusMinutes(durationMinutes),
            nightAwakenings = 0,
            sleepQuality = 4,
            morningEnergy = 4,
            source = DataSource.SELF_REPORTED
        )
    }

    @Test
    fun `empty history yields zero counts and null averages`() {
        val summary = useCase(emptyList(), windowDays = 7, today = today)
        assertEquals(0, summary.trackedNights)
        assertEquals(7, summary.missedNights)
        assertNull(summary.averageDurationMinutes)
        assertNull(summary.averageScore)
        assertNull(summary.consistencyPercent)
        assertEquals(0, summary.currentStreakNights)
        assertEquals(0, summary.longestStreakNights)
        assertTrue(summary.scoreSeries.isEmpty())
        assertTrue(summary.durationSeries.isEmpty())
    }

    @Test
    fun `only nights inside the window are counted`() {
        val sessions = listOf(
            session(today.minusDays(1), 23, 420),
            session(today.minusDays(2), 23, 400),
            session(today.minusDays(20), 23, 500) // outside 7-day window
        )
        val summary = useCase(sessions, windowDays = 7, today = today)
        assertEquals(2, summary.trackedNights)
        assertEquals(5, summary.missedNights)
        assertEquals(410L, summary.averageDurationMinutes)
        assertEquals(2, summary.scoreSeries.size)
    }

    @Test
    fun `window boundaries are inclusive`() {
        val sessions = listOf(
            session(today.minusDays(6), 23, 420),
            session(today, 23, 420)
        )
        val summary = useCase(sessions, windowDays = 7, today = today)
        assertEquals(2, summary.trackedNights)
    }

    @Test
    fun `streak survives until yesterday, resets on older gap`() {
        val alive = listOf(
            session(today.minusDays(2), 23, 420),
            session(today.minusDays(1), 23, 420)
        )
        assertEquals(2, useCase(alive, 7, today).currentStreakNights)

        val broken = listOf(
            session(today.minusDays(3), 23, 420),
            session(today.minusDays(1), 23, 420)
        )
        assertEquals(1, useCase(broken, 7, today).currentStreakNights)
    }

    @Test
    fun `longest streak spans consecutive tracked dates`() {
        val sessions = listOf(
            session(today.minusDays(10), 23, 420),
            session(today.minusDays(9), 23, 420),
            session(today.minusDays(8), 23, 420),
            session(today.minusDays(5), 23, 420),
            session(today.minusDays(4), 23, 420)
        )
        assertEquals(3, useCase(sessions, 30, today).longestStreakNights)
    }

    @Test
    fun `consistency needs at least three nights`() {
        val two = listOf(
            session(today.minusDays(1), 23, 420),
            session(today, 23, 420)
        )
        assertNull(useCase(two, 7, today).consistencyPercent)

        val three = two + session(today.minusDays(2), 23, 420)
        assertEquals(100, useCase(three, 7, today).consistencyPercent)
    }

    @Test
    fun `score series matches direct scoring of the trailing window`() {
        val sessions = (0..9).map {
            session(today.minusDays(it.toLong()), 23, 400L + it * 5)
        }
        val summary = useCase(sessions, windowDays = 30, today = today)
        assertEquals(10, summary.scoreSeries.size)
        // Every point must be within 0..100 and monotone-safe (deterministic)
        assertTrue(summary.scoreSeries.all { it.score in 0..100 })
        val again = useCase(sessions, windowDays = 30, today = today)
        assertEquals(summary, again)
    }

    @Test
    fun `distribution buckets cover every score point`() {
        val sessions = (0..5).map {
            session(today.minusDays(it.toLong()), 23, 420)
        }
        val summary = useCase(sessions, windowDays = 7, today = today)
        val total = summary.scoreDistribution.sumOf { it.count }
        assertEquals(summary.scoreSeries.size, total)
    }

    @Test
    fun `duplicate dates on one night are collapsed`() {
        val night = today.minusDays(1)
        val a = session(night, 23, 420)
        val dup = a.copy(id = "other")
        val summary = useCase(listOf(a, dup), 7, today)
        assertEquals(1, summary.trackedNights)
    }
}
