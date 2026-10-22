package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.PlannerLimits
import com.nocta.app.domain.model.SchedulePreset
import com.nocta.app.domain.profile.SleepProfile
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class PlannerLimitsTest {

    @Test
    fun `preset default wake times are distinct and age-appropriate`() {
        assertEquals(LocalTime.of(6, 30), SchedulePreset.SCHOOL_NIGHT.defaultWakeTime)
        assertEquals(LocalTime.of(8, 30), SchedulePreset.WEEKEND.defaultWakeTime)
        assertEquals(LocalTime.of(7, 0), SchedulePreset.CUSTOM.defaultWakeTime)
    }

    @Test
    fun `wind-down length is clamped and rounded to steps`() {
        assertEquals(PlannerLimits.MIN_WIND_DOWN_MINUTES, PlannerLimits.clampWindDownMinutes(0))
        assertEquals(PlannerLimits.MIN_WIND_DOWN_MINUTES, PlannerLimits.clampWindDownMinutes(-30))
        assertEquals(PlannerLimits.MAX_WIND_DOWN_MINUTES, PlannerLimits.clampWindDownMinutes(240))
        assertEquals(45, PlannerLimits.clampWindDownMinutes(47))
        assertEquals(45, PlannerLimits.clampWindDownMinutes(43))
    }

    @Test
    fun `plan is built backwards from the wake time`() {
        val plan = PlanBedtime().invoke(
            today = LocalDate.of(2026, 10, 20),
            wakeTime = LocalTime.of(7, 30),
            recentSessions = emptyList(),
            profile = SleepProfile(age = 30)
        )
        // 8h30 target + 20 min onset allowance = 8h50 opportunity.
        assertEquals(530L, plan.sleepOpportunityMinutes)
        assertEquals(LocalTime.of(22, 40), plan.recommendedBedtime)
        assertEquals(LocalTime.of(21, 55), plan.windDownStart)
        assertEquals(LocalTime.of(7, 30), plan.wakeTime)
        assertEquals(0, plan.nightsOfHistoryUsed)
    }

    @Test
    fun `wind-down that crosses midnight keeps a valid clock time`() {
        val plan = PlanBedtime().invoke(
            today = LocalDate.of(2026, 10, 20),
            wakeTime = LocalTime.of(6, 0),
            recentSessions = emptyList(),
            profile = SleepProfile(age = 16),
            windDownMinutes = 60
        )
        // Teen target 9h, +20 onset = 9h20 before 06:00 → 20:40; wind-down 19:40.
        assertEquals(LocalTime.of(20, 40), plan.recommendedBedtime)
        assertEquals(LocalTime.of(19, 40), plan.windDownStart)
    }

    @Test
    fun `a late wake time can push lights-out past midnight`() {
        val plan = PlanBedtime().invoke(
            today = LocalDate.of(2026, 10, 20),
            wakeTime = LocalTime.of(9, 0),
            recentSessions = emptyList(),
            profile = SleepProfile(age = 30),
            windDownMinutes = 30
        )
        // 09:00 − 8h50 = 00:10, wind-down 23:40 the previous evening.
        assertEquals(LocalTime.of(0, 10), plan.recommendedBedtime)
        assertEquals(LocalTime.of(23, 40), plan.windDownStart)
    }

    @Test
    fun `daylight saving transitions do not shift the wall-clock plan`() {
        val sleepProfile = SleepProfile(age = 30)
        val normalDay = LocalDate.of(2026, 10, 20)
        val dstDay = LocalDate.of(2026, 3, 8) // US spring-forward date

        val normal = PlanBedtime().invoke(normalDay, LocalTime.of(7, 0), emptyList(), sleepProfile)
        val dst = PlanBedtime().invoke(dstDay, LocalTime.of(7, 0), emptyList(), sleepProfile)

        // The plan is wall-clock based, so the recommendation is identical; the
        // actual instant is resolved by the zone when a reminder fires.
        assertEquals(normal.recommendedBedtime, dst.recommendedBedtime)
        assertEquals(normal.windDownStart, dst.windDownStart)

        val zone = ZoneId.of("America/New_York")
        assertEquals(
            LocalTime.of(22, 10),
            ZonedDateTime.of(dstDay, dst.recommendedBedtime, zone).toLocalTime()
        )
    }
}
