package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.profile.SleepProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalDate

class CalculateSleepScoreTest {

    private val calculate = CalculateSleepScore(sleepProfile = SleepProfile(age = 18))

    private fun session(
        bedtimeHour: Int,
        bedtimeMinute: Int = 0,
        sleepHours: Long,
        quality: Int = 4,
        awakenings: Int = 0
    ): SleepSession {
        val bedtime = LocalDateTime.of(2026, 1, 1, bedtimeHour, bedtimeMinute)
        val asleep = bedtime.plusMinutes(10)
        val wake = asleep.plusHours(sleepHours)
        return SleepSession(
            id = "s1",
            date = bedtime.toLocalDate(),
            bedtime = bedtime,
            sleepAttemptTime = bedtime,
            estimatedSleepTime = asleep,
            wakeTime = wake,
            nightAwakenings = awakenings,
            sleepQuality = quality,
            morningEnergy = 4,
            source = DataSource.SELF_REPORTED
        )
    }

    @Test
    fun `empty history returns all-zero breakdown`() {
        val result = calculate(emptyList())
        assertEquals(0, result.overall)
    }

    @Test
    fun `full target duration with high quality scores well`() {
        val sessions = listOf(
            session(bedtimeHour = 22, sleepHours = 8, quality = 5),
            session(bedtimeHour = 22, sleepHours = 8, quality = 5)
        )
        val result = calculate(sessions)
        assertTrue("expected a strong score, got ${result.overall}", result.overall >= 80)
    }

    @Test
    fun `short sleep duration lowers the duration factor`() {
        val sessions = listOf(session(bedtimeHour = 22, sleepHours = 3, quality = 3))
        val result = calculate(sessions)
        assertTrue(result.durationScore <= 55)
    }

    @Test
    fun `inconsistent bedtimes lower the consistency factor`() {
        val sessions = listOf(
            sessionAt(LocalDate.of(2026, 1, 1), 21, 0),
            sessionAt(LocalDate.of(2026, 1, 2), 1, 0),
            sessionAt(LocalDate.of(2026, 1, 3), 21, 0)
        )
        val result = calculate(sessions)
        assertTrue(result.consistencyScore < 60)
    }

    @Test
    fun `bedtimes across midnight are treated as close together`() {
        val sessions = listOf(
            sessionAt(LocalDate.of(2026, 1, 1), 23, 50),
            sessionAt(LocalDate.of(2026, 1, 2), 0, 10),
            sessionAt(LocalDate.of(2026, 1, 3), 23, 55)
        )

        assertTrue(calculate(sessions).consistencyScore >= 90)
    }

    @Test
    fun `very long sleep no longer receives the same duration score as target sleep`() {
        val target = session(bedtimeHour = 22, sleepHours = 8)
        val veryLong = session(bedtimeHour = 22, sleepHours = 14)

        assertTrue(calculate(listOf(veryLong)).durationScore < calculate(listOf(target)).durationScore)
    }

    @Test
    fun `one night does not claim a proven schedule pattern`() {
        val result = calculate(listOf(session(bedtimeHour = 22, sleepHours = 8)))

        assertEquals(70, result.consistencyScore)
        assertEquals(70, result.timingScore)
    }

    @Test
    fun `frequent awakenings lower the routine proxy factor`() {
        val sessions = listOf(session(bedtimeHour = 22, sleepHours = 8, awakenings = 4))
        val result = calculate(sessions)
        assertTrue(result.routineScore < 40)
    }

    @Test
    fun `child profile requires higher minimum duration than adult profile`() {
        val childCalculate = CalculateSleepScore(sleepProfile = SleepProfile(age = 10)) // 9-12 hours target (540 mins min)
        val adultCalculate = CalculateSleepScore(sleepProfile = SleepProfile(age = 25)) // 7-10 hours target (420 mins min)

        val sessions = listOf(session(bedtimeHour = 22, sleepHours = 7)) // 420 minutes

        val childResult = childCalculate(sessions)
        val adultResult = adultCalculate(sessions)

        // Seven hours meets the adult minimum but falls below the child's range.
        assertTrue("Adult minimum should still score reasonably, got ${adultResult.durationScore}", adultResult.durationScore >= 80)
        assertTrue("Child score should be below adult score, got ${childResult.durationScore}", childResult.durationScore < adultResult.durationScore)
    }

    private fun sessionAt(date: LocalDate, hour: Int, minute: Int): SleepSession {
        val bedtime = date.atTime(hour, minute)
        val asleep = bedtime.plusMinutes(10)
        return SleepSession(
            id = date.toString(),
            date = date,
            bedtime = bedtime,
            sleepAttemptTime = bedtime,
            estimatedSleepTime = asleep,
            wakeTime = asleep.plusHours(8),
            nightAwakenings = 0,
            sleepQuality = 4,
            morningEnergy = 4,
            source = DataSource.SELF_REPORTED
        )
    }
}
