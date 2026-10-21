package com.nocta.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SleepCalendarTest {

    private val monday = LocalDate.of(2026, 1, 5)

    @Test
    fun `start of week maps every weekday back to monday`() {
        for (offset in 0L..6L) {
            val date = monday.plusDays(offset)
            assertEquals("failed for $date", monday, SleepCalendar.startOfWeek(date))
        }
    }

    @Test
    fun `week days are seven consecutive dates starting monday`() {
        val days = SleepCalendar.weekDays(LocalDate.of(2026, 1, 8)) // Thursday
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 1, 5), days.first())
        assertEquals(LocalDate.of(2026, 1, 11), days.last())
        assertTrue(days.zipWithNext().all { (a, b) -> b == a.plusDays(1) })
    }

    @Test
    fun `week starting sunday belongs to the previous iso week`() {
        // 2026-01-04 is a Sunday; its ISO week starts Monday 2025-12-29.
        assertEquals(
            LocalDate.of(2025, 12, 29),
            SleepCalendar.startOfWeek(LocalDate.of(2026, 1, 4))
        )
    }

    @Test
    fun `month grid contains every day of the month in order`() {
        val anchor = LocalDate.of(2026, 1, 15)
        val days = SleepCalendar.monthGrid(anchor).filterNotNull()
        assertEquals(31, days.size)
        assertEquals(LocalDate.of(2026, 1, 1), days.first())
        assertEquals(LocalDate.of(2026, 1, 31), days.last())
        assertTrue(days.zipWithNext().all { (a, b) -> b == a.plusDays(1) })
    }

    @Test
    fun `month grid is always whole monday-first weeks`() {
        val anchors = listOf(
            LocalDate.of(2026, 1, 15), // 31-day month starting Thursday
            LocalDate.of(2024, 2, 10), // leap February
            LocalDate.of(2025, 2, 10), // non-leap February
            LocalDate.of(2025, 11, 20) // 30-day month starting Saturday
        )
        for (anchor in anchors) {
            val grid = SleepCalendar.monthGrid(anchor)
            assertEquals("grid not whole weeks for $anchor", 0, grid.size % 7)
            grid.forEachIndexed { index, date ->
                if (date != null) {
                    assertEquals(
                        "cell $index of $anchor is not Monday-first aligned",
                        (index % 7) + 1,
                        date.dayOfWeek.value
                    )
                }
            }
        }
    }

    @Test
    fun `february leap year includes the 29th`() {
        val days = SleepCalendar.monthGrid(LocalDate.of(2024, 2, 10)).filterNotNull()
        assertEquals(29, days.size)
        assertEquals(LocalDate.of(2024, 2, 29), days.last())
    }

    @Test
    fun `year months are january through december of the anchor year`() {
        val months = SleepCalendar.yearMonths(LocalDate.of(2026, 6, 15))
        assertEquals(12, months.size)
        assertEquals(1, months.first().monthValue)
        assertEquals(12, months.last().monthValue)
        assertTrue(months.all { it.year == 2026 })
    }

    @Test
    fun `previous period steps back within and across year boundaries`() {
        assertEquals(
            LocalDate.of(2026, 2, 15),
            SleepCalendar.previousPeriod(CalendarPeriod.MONTH, LocalDate.of(2026, 3, 15))
        )
        assertEquals(
            LocalDate.of(2025, 12, 10),
            SleepCalendar.previousPeriod(CalendarPeriod.MONTH, LocalDate.of(2026, 1, 10))
        )
        assertEquals(
            LocalDate.of(2025, 1, 10),
            SleepCalendar.previousPeriod(CalendarPeriod.YEAR, LocalDate.of(2026, 1, 10))
        )
        assertEquals(
            monday.minusWeeks(1),
            SleepCalendar.previousPeriod(CalendarPeriod.WEEK, monday)
        )
    }

    @Test
    fun `next period moves forward inside the current period`() {
        val today = LocalDate.of(2026, 3, 20)

        assertEquals(
            LocalDate.of(2026, 3, 15),
            SleepCalendar.nextPeriod(CalendarPeriod.MONTH, LocalDate.of(2026, 2, 15), today)
        )
        assertEquals(
            monday.plusWeeks(1),
            SleepCalendar.nextPeriod(CalendarPeriod.WEEK, monday, today)
        )
        assertEquals(
            LocalDate.of(2026, 3, 10),
            SleepCalendar.nextPeriod(CalendarPeriod.YEAR, LocalDate.of(2025, 3, 10), today)
        )
    }

    @Test
    fun `next period is clamped so the calendar cannot point to the future`() {
        val today = LocalDate.of(2026, 3, 20)

        // Already in the current month / week / year: no forward movement,
        // the anchor itself is returned unchanged.
        assertEquals(
            today,
            SleepCalendar.nextPeriod(CalendarPeriod.MONTH, today, today)
        )
        assertEquals(
            SleepCalendar.startOfWeek(today).plusDays(3),
            SleepCalendar.nextPeriod(
                CalendarPeriod.WEEK,
                SleepCalendar.startOfWeek(today).plusDays(3),
                today
            )
        )
        assertEquals(
            today,
            SleepCalendar.nextPeriod(CalendarPeriod.YEAR, today, today)
        )
        assertFalse(
            SleepCalendar.canGoNext(CalendarPeriod.MONTH, today, today)
        )
    }

    @Test
    fun `can go next is true one period back`() {
        val today = LocalDate.of(2026, 3, 20)
        assertTrue(
            SleepCalendar.canGoNext(CalendarPeriod.MONTH, LocalDate.of(2026, 2, 15), today)
        )
    }

    @Test
    fun `month title contains the full month name and year`() {
        val title = SleepCalendar.title(
            CalendarPeriod.MONTH,
            LocalDate.of(2026, 3, 20),
            locale = java.util.Locale.ENGLISH
        )
        assertEquals("March 2026", title)
    }

    @Test
    fun `year title is the anchor year`() {
        assertEquals(
            "2026",
            SleepCalendar.title(CalendarPeriod.YEAR, LocalDate.of(2026, 3, 20))
        )
    }

    @Test
    fun `weekday labels are monday first and seven long`() {
        val labels = SleepCalendar.weekdayLabels(java.util.Locale.ENGLISH)
        assertEquals(7, labels.size)
        assertEquals("Mon", labels.first())
        assertEquals("Sun", labels.last())
    }
}
