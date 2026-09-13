package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.SleepTrackingError
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant

class SleepTrackingRulesTest {

    private val now = Instant.parse("2026-03-01T22:00:00Z")

    @Test
    fun `start now is valid`() {
        SleepTrackingRules.validateStart(now, now)
    }

    @Test
    fun `start in the future throws`() {
        try {
            SleepTrackingRules.validateStart(now.plusSeconds(60 * 10), now)
            fail("expected InvalidTimestamps")
        } catch (e: SleepTrackingError.InvalidTimestamps) {
            assertTrue(e.message!!.contains("future"))
        }
    }

    @Test
    fun `start within clock-skew tolerance is accepted`() {
        SleepTrackingRules.validateStart(now.plusSeconds(30), now)
    }

    @Test
    fun `end before start throws`() {
        try {
            SleepTrackingRules.validateEnd(now, now.minusSeconds(60), now)
            fail("expected InvalidTimestamps")
        } catch (e: SleepTrackingError.InvalidTimestamps) {
            assertTrue(e.message!!.contains("before start"))
        }
    }

    @Test
    fun `end in the future throws`() {
        try {
            SleepTrackingRules.validateEnd(
                now.minusSeconds(3600),
                now.plusSeconds(3600),
                now
            )
            fail("expected InvalidTimestamps")
        } catch (e: SleepTrackingError.InvalidTimestamps) {
            assertTrue(e.message!!.contains("future"))
        }
    }

    @Test
    fun `normal eight-hour session is valid`() {
        SleepTrackingRules.validateEnd(now.minusSeconds(8 * 3600), now, now)
    }

    @Test
    fun `session over twenty hours throws UnreasonableDuration`() {
        val start = now.minusSeconds(21 * 3600)
        try {
            SleepTrackingRules.validateEnd(start, now, now)
            fail("expected UnreasonableDuration")
        } catch (e: SleepTrackingError.UnreasonableDuration) {
            assertTrue(e.actualMinutes >= 20 * 60)
        }
    }

    @Test
    fun `exactly twenty hours is valid`() {
        SleepTrackingRules.validateEnd(now.minusSeconds(20 * 3600), now, now)
    }

    @Test
    fun `stale detection is false for a fresh session`() {
        assertFalse(SleepTrackingRules.isStale(now.minusSeconds(60), now))
    }

    @Test
    fun `stale detection is true past the ceiling`() {
        assertTrue(SleepTrackingRules.isStale(now.minusSeconds(21 * 3600), now))
    }
}
