package com.nocta.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SleepQualityCalendarTest {

    @Test
    fun `score thresholds correctly map to 5 sleep quality categories`() {
        assertEquals(SleepQualityCategory.EXCELLENT, SleepQualityCategory.fromScore(95))
        assertEquals(SleepQualityCategory.EXCELLENT, SleepQualityCategory.fromScore(85))
        assertEquals(SleepQualityCategory.GOOD, SleepQualityCategory.fromScore(84))
        assertEquals(SleepQualityCategory.GOOD, SleepQualityCategory.fromScore(70))
        assertEquals(SleepQualityCategory.FAIRLY_GOOD, SleepQualityCategory.fromScore(69))
        assertEquals(SleepQualityCategory.FAIRLY_GOOD, SleepQualityCategory.fromScore(55))
        assertEquals(SleepQualityCategory.POOR, SleepQualityCategory.fromScore(54))
        assertEquals(SleepQualityCategory.POOR, SleepQualityCategory.fromScore(40))
        assertEquals(SleepQualityCategory.VERY_POOR, SleepQualityCategory.fromScore(39))
        assertEquals(SleepQualityCategory.VERY_POOR, SleepQualityCategory.fromScore(0))
    }

    @Test
    fun `unrecorded date returns neutral unrecorded category`() {
        val record = DailySleepRecord(
            date = LocalDate.of(2026, 10, 1),
            session = null,
            score = null,
            category = SleepQualityCategory.UNRECORDED
        )

        assertEquals(SleepQualityCategory.UNRECORDED, record.category)
        assertEquals(null, record.score)
        assertEquals(null, record.session)
    }
}
