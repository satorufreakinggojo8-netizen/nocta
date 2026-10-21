package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.profile.SleepProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PlanBedtimeTest {

    private val useCase = PlanBedtime()
    private val adult = SleepProfile(age = 30)   // 7–10 h range
    private val today = LocalDate.of(2026, 10, 21)
    private val wake = LocalTime.of(7, 0)

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
            sleepQuality = 3,
            morningEnergy = 3,
            source = DataSource.SELF_REPORTED
        )
    }

    @Test
    fun `without history the plan uses the age midpoint`() {
        val plan = useCase(today, wake, emptyList(), adult)
        // 7–10h midpoint = 8h30m + 20m onset = 8h50m before 07:00 = 22:10
        assertEquals(22 * 60 + 10, plan.recommendedBedtime.hour * 60 + plan.recommendedBedtime.minute)
        assertEquals(0, plan.nightsOfHistoryUsed)
        assertEquals(530L, plan.sleepOpportunityMinutes)
    }

    @Test
    fun `wind-down starts the configured minutes before bedtime`() {
        val plan = useCase(today, wake, emptyList(), adult, windDownMinutes = 45)
        val diff = java.time.Duration.between(plan.windDownStart, plan.recommendedBedtime).toMinutes()
        assertEquals(45L, diff)
    }

    @Test
    fun `history anchors the target inside the age range`() {
        // 5 nights of 6h — below the 7h minimum, so target clamps up to 7h.
        val shortNights = (0..4).map { session(today.minusDays(it.toLong()), 1, 360) }
        val plan = useCase(today, wake, shortNights, adult)
        assertEquals(5, plan.nightsOfHistoryUsed)
        assertEquals((7 * 60 + 20).toLong(), plan.sleepOpportunityMinutes)
    }

    @Test
    fun `long shifts are phased to twenty minutes`() {
        // Habit of 02:00 bedtime; ideal for 7h20m target is ~23:20 → big gap.
        val lateNights = (0..4).map { session(today.minusDays(it.toLong()), 2, 380) }
        val plan = useCase(today, wake, lateNights, adult)
        assertEquals(20, kotlin.math.abs(plan.suggestedShiftTonightMinutes))
        // Habit (02:00) is later than target, so the signed shift is negative.
        assertTrue(plan.shiftFromRecentAverageMinutes < -20)
    }

    @Test
    fun `small shifts are taken as-is`() {
        // Recent bedtime 23:40, ideal ~23:20 → shift -20, within the cap.
        val nights = (0..3).map { session(today.minusDays(it.toLong()), 23, 440) }
        val plan = useCase(today, wake, nights, adult)
        assertEquals(plan.shiftFromRecentAverageMinutes, plan.suggestedShiftTonightMinutes)
        assertTrue(kotlin.math.abs(plan.suggestedShiftTonightMinutes) <= 20)
    }

    @Test
    fun `recommended bedtime lands on the stepped time from the recent habit`() {
        val lateNights = (0..4).map { session(today.minusDays(it.toLong()), 2, 380) }
        val plan = useCase(today, wake, lateNights, adult)
        // Stepped from the recent average (02:00) toward the ideal by 20 min.
        assertEquals(1 * 60 + 40, plan.recommendedBedtime.hour * 60 + plan.recommendedBedtime.minute)
    }

    @Test
    fun `history outside the 14-day window is ignored`() {
        val old = (0..9).map { session(today.minusDays(20 + it.toLong()), 2, 380) }
        val plan = useCase(today, wake, old, adult)
        assertEquals(0, plan.nightsOfHistoryUsed)
    }

    @Test
    fun `future dates are ignored`() {
        val future = session(today.plusDays(3), 23, 420)
        val plan = useCase(today, wake, listOf(future), adult)
        assertEquals(0, plan.nightsOfHistoryUsed)
    }

    @Test
    fun `child profile gets a longer opportunity than senior profile`() {
        val child = SleepProfile(age = 10)   // 9–12h
        val senior = SleepProfile(age = 70)  // 7–8h
        val childPlan = useCase(today, wake, emptyList(), child)
        val seniorPlan = useCase(today, wake, emptyList(), senior)
        assertTrue(childPlan.sleepOpportunityMinutes > seniorPlan.sleepOpportunityMinutes)
    }

    @Test
    fun `explanation always names the age range and wake time`() {
        val plan = useCase(today, wake, emptyList(), adult)
        assertTrue(plan.explanation.isNotEmpty())
        assertTrue(plan.explanation[0].contains("7–10 h"))
        assertTrue(plan.explanation[1].contains("7:00 AM"))
    }

    @Test
    fun `plan is deterministic`() {
        val nights = (0..6).map { session(today.minusDays(it.toLong()), 22 + (it % 2), 420) }
        val a = useCase(today, wake, nights, adult)
        val b = useCase(today, wake, nights, adult)
        assertEquals(a, b)
    }
}
