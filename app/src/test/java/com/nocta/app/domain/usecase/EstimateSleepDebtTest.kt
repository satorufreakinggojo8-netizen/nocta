package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.SleepDebtStatus
import com.nocta.app.domain.model.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EstimateSleepDebtTest {

    private val useCase = EstimateSleepDebt()

    private fun night(index: Int, minutes: Long): SleepSession {
        val base = LocalDate.of(2026, 10, 1).minusDays(index.toLong()).atTime(22, 30)
        return SleepSession(
            id = "night-$index",
            date = base.toLocalDate(),
            bedtime = base,
            sleepAttemptTime = base,
            estimatedSleepTime = base,
            wakeTime = base.plusMinutes(minutes),
            nightAwakenings = 0,
            sleepQuality = 4,
            morningEnergy = 3,
            source = DataSource.SELF_REPORTED
        )
    }

    @Test
    fun `no records means no estimate`() {
        assertNull(useCase(emptyList()))
    }

    @Test
    fun `sleeping past the target is never negative debt`() {
        val debt = useCase(listOf(night(0, 540), night(1, 520), night(2, 600)))!!
        assertEquals(SleepDebtStatus.WELL_RESTED, debt.status)
        assertEquals(0, debt.approximateDeficitMinutes)
    }

    @Test
    fun `a small shortfall reads as well rested`() {
        // 7h35m against an 8h target: 25 min × 2 nights = 50 min.
        val debt = useCase(listOf(night(0, 455), night(1, 455)))!!
        assertEquals(SleepDebtStatus.WELL_RESTED, debt.status)
        assertEquals(50, debt.approximateDeficitMinutes)
    }

    @Test
    fun `a moderate shortfall is slightly behind`() {
        // 7h30m: 30 min × 7 nights = 210 min.
        val debt = useCase((0..6).map { night(it, 450) })!!
        assertEquals(SleepDebtStatus.SLIGHTLY_BEHIND, debt.status)
        assertEquals(210, debt.approximateDeficitMinutes)
    }

    @Test
    fun `a large shortfall is catching up`() {
        // 6h: 120 min × 6 nights = 720 min.
        val debt = useCase((0..5).map { night(it, 360) })!!
        assertEquals(SleepDebtStatus.CATCHING_UP, debt.status)
        assertEquals(720, debt.approximateDeficitMinutes)
    }

    @Test
    fun `missing nights are not counted as zero`() {
        val oneNight = useCase(listOf(night(0, 480)))!!
        assertEquals(SleepDebtStatus.WELL_RESTED, oneNight.status)
        assertEquals(0, oneNight.approximateDeficitMinutes)

        // Fewer records means a smaller total, never a padded one.
        val twoNights = useCase(listOf(night(0, 420), night(1, 420)))!!
        assertEquals(120, twoNights.approximateDeficitMinutes)
    }
}
