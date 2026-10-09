package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.profile.SleepProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

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
            session(bedtimeHour = 21, sleepHours = 8),
            session(bedtimeHour = 1, sleepHours = 8) // wildly different bedtime hour
        )
        val result = calculate(sessions)
        assertTrue(result.consistencyScore < 60)
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

        // 7 hrs (420 min) is >= 420 min min for adult (100%), but < 540 min for child (~77%)
        assertEquals(100, adultResult.durationScore)
        assertTrue("Child score should be penalized for 7 hrs, got ${childResult.durationScore}", childResult.durationScore < 100)
    }
}
