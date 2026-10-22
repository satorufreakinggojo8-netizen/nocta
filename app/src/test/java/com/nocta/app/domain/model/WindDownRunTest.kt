package com.nocta.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WindDownRunTest {

    private val start = 1_000_000L
    private val minute = 60_000L

    private fun run(plannedMinutes: Int = 45) = WindDownRun(
        id = "r1",
        date = LocalDate.of(2026, 10, 20),
        plannedMinutes = plannedMinutes,
        startedAtMillis = start
    )

    @Test
    fun `elapsed time grows while running`() {
        assertEquals(0L, run().elapsedMillisAt(start))
        assertEquals(5 * minute, run().elapsedMillisAt(start + 5 * minute))
    }

    @Test
    fun `pause freezes elapsed time and resume excludes the pause`() {
        val paused = run().pause(start + 10 * minute)
        assertTrue(paused.isPaused)
        // Time keeps passing in the world, but not in the run.
        assertEquals(10 * minute, paused.elapsedMillisAt(start + 30 * minute))

        val resumed = paused.resume(start + 30 * minute)
        assertFalse(resumed.isPaused)
        assertEquals(20 * minute, resumed.accumulatedPauseMillis)
        assertEquals(10 * minute, resumed.elapsedMillisAt(start + 30 * minute))
        assertEquals(15 * minute, resumed.elapsedMillisAt(start + 35 * minute))
    }

    @Test
    fun `pausing twice or resuming an unpaused run is a no-op`() {
        val paused = run().pause(start + minute)
        assertEquals(paused, paused.pause(start + 5 * minute))
        assertEquals(run(), run().resume(start + 5 * minute))
    }

    @Test
    fun `remaining time is clamped and progress never exceeds one`() {
        val r = run(plannedMinutes = 10)
        assertEquals(600L, r.remainingSecondsAt(start))
        assertEquals(300L, r.remainingSecondsAt(start + 5 * minute))
        assertEquals(0L, r.remainingSecondsAt(start + 20 * minute))
        assertEquals(1f, r.progressAt(start + 20 * minute))
        assertEquals(0.5f, r.progressAt(start + 5 * minute))
    }

    @Test
    fun `completing freezes the run at the finish time and records steps`() {
        val ticking = run().toggleStep("dim_lights").toggleStep("phone_away")
        val done = ticking.finish(start + 12 * minute, WindDownOutcome.COMPLETED)

        assertEquals(WindDownOutcome.COMPLETED, done.outcome)
        assertFalse(done.isActive)
        assertEquals(start + 12 * minute, done.endedAtMillis)
        // Later reads must not keep counting after the run ended.
        assertEquals(12 * minute, done.elapsedMillisAt(start + 60 * minute))
        assertEquals(setOf("dim_lights", "phone_away"), done.completedStepIds)
    }

    @Test
    fun `finishing while paused ends at the pause moment`() {
        val paused = run().pause(start + 8 * minute)
        val done = paused.finish(start + 40 * minute, WindDownOutcome.DISMISSED)
        assertEquals(start + 8 * minute, done.endedAtMillis)
        assertNull(done.pausedAtMillis)
        assertEquals(8L, done.elapsedMinutesAt(start + 40 * minute))
    }

    @Test
    fun `dismissed runs cannot be completed afterwards`() {
        val dismissed = run().finish(start + 2 * minute, WindDownOutcome.DISMISSED)
        assertEquals(dismissed, dismissed.finish(start + 3 * minute, WindDownOutcome.COMPLETED))
    }

    @Test
    fun `toggling a step twice clears it`() {
        val once = run().toggleStep("read")
        assertTrue("read" in once.completedStepIds)
        assertTrue(once.toggleStep("read").completedStepIds.isEmpty())
    }
}
