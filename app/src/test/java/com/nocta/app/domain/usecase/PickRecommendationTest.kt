package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PickRecommendationTest {

    private val useCase = PickRecommendation()

    private fun breakdown(
        consistency: Int = 85,
        trackedNights: Int = 14
    ) = SleepScoreBreakdown(
        durationScore = 80,
        consistencyScore = consistency,
        timingScore = 80,
        qualityScore = 80,
        routineScore = 80,
        trackedNights = trackedNights
    )

    private fun lastNight(awakenings: Int): SleepSession {
        val base = LocalDate.of(2026, 10, 20).atTime(23, 0)
        return SleepSession(
            id = "last",
            date = base.toLocalDate(),
            bedtime = base,
            sleepAttemptTime = base,
            estimatedSleepTime = base,
            wakeTime = base.plusMinutes(450),
            nightAwakenings = awakenings,
            sleepQuality = 3,
            morningEnergy = 3,
            source = DataSource.SELF_REPORTED
        )
    }

    @Test
    fun `no scored history means no personalised suggestion`() {
        assertNull(useCase(null, null))
        assertNull(useCase(lastNight(0), breakdown(trackedNights = 0)))
    }

    @Test
    fun `low bedtime consistency comes first`() {
        val recommendation = useCase(lastNight(3), breakdown(consistency = 55))!!
        assertEquals("rec_consistency", recommendation.id)
    }

    @Test
    fun `multiple awakenings point at the sleep environment`() {
        val recommendation = useCase(lastNight(2), breakdown())!!
        assertEquals("rec_environment", recommendation.id)
    }

    @Test
    fun `a stable night falls back to the wind-down suggestion`() {
        val recommendation = useCase(lastNight(0), breakdown())!!
        assertEquals("rec_screens", recommendation.id)
    }

    @Test
    fun `the same inputs always produce the same suggestion`() {
        assertEquals(
            useCase(lastNight(1), breakdown(trackedNights = 3)),
            useCase(lastNight(1), breakdown(trackedNights = 3))
        )
    }
}
