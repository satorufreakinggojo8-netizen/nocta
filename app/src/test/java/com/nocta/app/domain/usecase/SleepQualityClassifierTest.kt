package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.SleepDayQuality
import com.nocta.app.domain.model.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class SleepQualityClassifierTest {

    private val classifier = SleepQualityClassifier()

    /**
     * durationMinutes of sleep plus any self-reported awake time; the model's
     * durationMinutes subtracts awakeMinutes again, so wake time is padded to
     * keep the two consistent.
     */
    private fun session(
        durationMinutes: Long,
        quality: Int = 4,
        awakenings: Int = 0,
        awakeMinutes: Int = 0
    ): SleepSession {
        val start = LocalDateTime.of(2026, 1, 1, 22, 0)
        return SleepSession(
            id = "s-$durationMinutes-$quality-$awakenings-$awakeMinutes",
            date = start.toLocalDate(),
            bedtime = start,
            sleepAttemptTime = start,
            estimatedSleepTime = start,
            wakeTime = start.plusMinutes(durationMinutes + awakeMinutes),
            nightAwakenings = awakenings,
            sleepQuality = quality,
            morningEnergy = 3,
            source = DataSource.SELF_REPORTED,
            awakeMinutes = awakeMinutes
        )
    }

    @Test
    fun `missing session is the neutral no-record state`() {
        assertEquals(SleepDayQuality.NO_RECORD, classifier.classify(null))
    }

    @Test
    fun `target duration with excellent self-report is excellent`() {
        assertEquals(
            SleepDayQuality.EXCELLENT,
            classifier.classify(session(durationMinutes = 480, quality = 5))
        )
    }

    @Test
    fun `target duration with fair self-report is good`() {
        assertEquals(
            SleepDayQuality.GOOD,
            classifier.classify(session(durationMinutes = 480, quality = 3))
        )
    }

    @Test
    fun `target duration with poor self-report is fair`() {
        assertEquals(
            SleepDayQuality.FAIR,
            classifier.classify(session(durationMinutes = 480, quality = 2))
        )
    }

    @Test
    fun `four hours with poor self-report is poor`() {
        assertEquals(
            SleepDayQuality.POOR,
            classifier.classify(session(durationMinutes = 240, quality = 2))
        )
    }

    @Test
    fun `three hours with very poor self-report is very poor`() {
        assertEquals(
            SleepDayQuality.VERY_POOR,
            classifier.classify(session(durationMinutes = 180, quality = 1))
        )
    }

    @Test
    fun `implausible short night is never a good night`() {
        assertEquals(
            SleepDayQuality.VERY_POOR,
            classifier.classify(session(durationMinutes = 60, quality = 5))
        )
    }

    @Test
    fun `implausible long night is never a good night`() {
        assertEquals(
            SleepDayQuality.VERY_POOR,
            classifier.classify(session(durationMinutes = 21 * 60, quality = 5))
        )
    }

    @Test
    fun `oversleeping scores below target sleep`() {
        val target = classifier.scoreNight(session(durationMinutes = 480, quality = 5))
        val twelveHours = classifier.scoreNight(session(durationMinutes = 12 * 60, quality = 5))

        assertTrue(twelveHours < target)
        assertEquals(SleepDayQuality.EXCELLENT, classifier.levelForScore(twelveHours))
    }

    @Test
    fun `self-report values outside one to five are coerced`() {
        val tooHigh = classifier.classify(session(durationMinutes = 480, quality = 99))
        val cappedHigh = classifier.classify(session(durationMinutes = 480, quality = 5))
        assertEquals(cappedHigh, tooHigh)

        val tooLow = classifier.classify(session(durationMinutes = 480, quality = -3))
        val cappedLow = classifier.classify(session(durationMinutes = 480, quality = 1))
        assertEquals(cappedLow, tooLow)
    }

    @Test
    fun `night awakenings lower the level but the penalty is capped`() {
        val clean = classifier.scoreNight(session(durationMinutes = 480, quality = 5))
        val manyAwakenings =
            classifier.scoreNight(session(durationMinutes = 480, quality = 5, awakenings = 3))
        val extremeAwakenings =
            classifier.scoreNight(session(durationMinutes = 480, quality = 5, awakenings = 10))

        assertTrue(manyAwakenings < clean)
        assertEquals(clean - 15.0, extremeAwakenings, 0.001)
    }

    @Test
    fun `classification is deterministic for identical inputs`() {
        val night = session(durationMinutes = 390, quality = 3, awakenings = 2)
        assertEquals(classifier.classify(night), classifier.classify(night))
        assertEquals(classifier.scoreNight(night), classifier.scoreNight(night), 0.0)
    }

    @Test
    fun `level thresholds are inclusive at the band minimums`() {
        assertEquals(SleepDayQuality.EXCELLENT, classifier.levelForScore(100.0))
        assertEquals(SleepDayQuality.EXCELLENT, classifier.levelForScore(85.0))
        assertEquals(SleepDayQuality.GOOD, classifier.levelForScore(84.99))
        assertEquals(SleepDayQuality.GOOD, classifier.levelForScore(70.0))
        assertEquals(SleepDayQuality.FAIR, classifier.levelForScore(69.99))
        assertEquals(SleepDayQuality.FAIR, classifier.levelForScore(50.0))
        assertEquals(SleepDayQuality.POOR, classifier.levelForScore(49.99))
        assertEquals(SleepDayQuality.POOR, classifier.levelForScore(30.0))
        assertEquals(SleepDayQuality.VERY_POOR, classifier.levelForScore(29.99))
        assertEquals(SleepDayQuality.VERY_POOR, classifier.levelForScore(0.0))
    }

    @Test
    fun `awake minutes reduce the effective duration`() {
        // The same 420 minutes in bed, but 120 of them spent awake, is a
        // 300-minute night — and scores worse than 420 minutes of sleep.
        val restless = session(durationMinutes = 300, quality = 4, awakeMinutes = 120)
        val soundSleep = session(durationMinutes = 420, quality = 4)
        assertEquals(300L, restless.durationMinutes)

        assertTrue(
            classifier.scoreNight(restless) < classifier.scoreNight(soundSleep)
        )
    }
}
