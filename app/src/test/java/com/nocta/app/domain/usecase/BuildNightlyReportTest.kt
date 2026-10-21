package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.MorningCheckIn
import com.nocta.app.domain.model.SleepDisturbance
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.profile.SleepProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BuildNightlyReportTest {

    private val useCase = BuildNightlyReport(CalculateSleepScore())
    private val profile = SleepProfile(age = 30)

    private fun session(
        date: LocalDate,
        bedtimeHour: Int,
        durationMinutes: Long,
        awakeMinutes: Int = 0,
        awakenings: Int = 0,
        quality: Int = 3,
        energy: Int = 3,
        source: DataSource = DataSource.SELF_REPORTED
    ): SleepSession {
        val attempt = date.atTime(bedtimeHour, 0)
        val wake = attempt.plusMinutes(durationMinutes + awakeMinutes)
        return SleepSession(
            id = "s-${date.toEpochDay()}",
            date = date,
            bedtime = attempt,
            sleepAttemptTime = attempt,
            estimatedSleepTime = attempt,
            wakeTime = wake,
            nightAwakenings = awakenings,
            sleepQuality = quality,
            morningEnergy = energy,
            source = source,
            awakeMinutes = awakeMinutes
        )
    }

    private fun checkIn(quality: Int = 4) = MorningCheckIn(
        sleepQuality = quality,
        nightAwakenings = 0,
        awakeMinutes = 0,
        morningEnergy = 4,
        disturbance = SleepDisturbance.NONE,
        unusualEvent = null
    )

    private val today = LocalDate.of(2026, 10, 21)

    @Test
    fun `efficiency is computed for self-reported nights`() {
        val night = session(today, 23, durationMinutes = 420, awakeMinutes = 30)
        val report = useCase(night, listOf(night), checkIn(), profile)
        // time in bed = 450, asleep = 420 -> 93%
        assertEquals(450L, report.timeInBedMinutes)
        assertEquals(420L, report.asleepMinutes)
        assertEquals(93, report.efficiencyPercent)
    }

    @Test
    fun `efficiency is withheld for estimated sources`() {
        val night = session(today, 23, durationMinutes = 420, source = DataSource.ESTIMATED)
        val report = useCase(night, listOf(night), checkIn(), profile)
        assertNull(report.efficiencyPercent)
        assertTrue(report.missingInputs.any { it.contains("efficiency is not available") })
    }

    @Test
    fun `baseline excludes the night itself`() {
        val n1 = session(today.minusDays(2), 23, durationMinutes = 400)
        val n2 = session(today.minusDays(1), 23, durationMinutes = 420)
        val target = session(today, 23, durationMinutes = 480)
        val report = useCase(target, listOf(n1, n2, target), checkIn(), profile)
        assertEquals(2, report.baselineNightsCompared)
        assertEquals(410L, report.baselineAvgDurationMinutes)
        assertEquals(70L, report.durationDeltaMinutes)
    }

    @Test
    fun `bedtime delta is circular and signed`() {
        // Baseline at 23:00; target at 00:00 = +60 minutes, not -1380.
        val n1 = session(today.minusDays(1), 23, durationMinutes = 420)
        val target = session(today, 0, durationMinutes = 420)
        val report = useCase(target, listOf(n1, target), checkIn(), profile)
        assertNotNull(report.bedtimeDeltaMinutes)
        assertEquals(60, report.bedtimeDeltaMinutes)
    }

    @Test
    fun `single night with no baseline reports null baseline fields`() {
        val night = session(today, 23, durationMinutes = 420)
        val report = useCase(night, listOf(night), checkIn(), profile)
        assertNull(report.baselineAvgDurationMinutes)
        assertNull(report.durationDeltaMinutes)
        assertNull(report.bedtimeDeltaMinutes)
        assertEquals(0, report.baselineNightsCompared)
    }

    @Test
    fun `missing check-in is reported without inventing data`() {
        val night = session(today, 23, durationMinutes = 420)
        val report = useCase(night, listOf(night), null, profile)
        assertNull(report.checkIn)
        assertTrue(report.missingInputs.any { it.contains("No morning check-in") })
    }

    @Test
    fun `strongest and improvement factors match the breakdown extremes`() {
        val nights = (0..5).map { session(today.minusDays(it.toLong()), 23, durationMinutes = 430) }
        val report = useCase(nights.last(), nights, checkIn(), profile)
        val allScores = listOf(
            report.breakdown.durationScore,
            report.breakdown.consistencyScore,
            report.breakdown.qualityScore,
            report.breakdown.timingScore,
            report.breakdown.routineScore
        )
        assertEquals(allScores.max(), report.strongestFactor.score)
        assertEquals(allScores.min(), report.improvementFactor.score)
        assertTrue(report.recommendation.isNotBlank())
    }

    @Test
    fun `report is deterministic`() {
        val nights = (0..9).map {
            session(today.minusDays(it.toLong()), 22 + (it % 2), durationMinutes = 400L + it * 5)
        }
        val a = useCase(nights.last(), nights, checkIn(), profile)
        val b = useCase(nights.last(), nights, checkIn(), profile)
        assertEquals(a, b)
    }

    @Test
    fun `score window matches CalculateSleepScore over the same window`() {
        // Built oldest-first so the most recent night has a full 14-night window.
        val nights = (20 downTo 0).map {
            session(today.minusDays(it.toLong()), 23, durationMinutes = 420)
        }
        val target = nights.last()
        val report = useCase(target, nights, checkIn(), profile)
        val window = nights.filter { !it.date.isAfter(target.date) }.takeLast(14)
        assertEquals(14, report.nightsInScoreWindow)
        assertEquals(window.size, report.nightsInScoreWindow)
        assertEquals(SleepScoreBreakdownChecker.overallOf(window, profile), report.breakdown.overall)
    }
}

/** Mirrors CalculateSleepScore's windowing so the test asserts real equality. */
private object SleepScoreBreakdownChecker {
    private val score = CalculateSleepScore()
    fun overallOf(window: List<SleepSession>, profile: SleepProfile): Int =
        score(window, profile).overall
}
