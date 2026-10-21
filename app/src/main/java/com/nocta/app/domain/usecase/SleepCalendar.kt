package com.nocta.app.domain.usecase

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Navigation granularity of the sleep-history calendar. */
enum class CalendarPeriod { WEEK, MONTH, YEAR }

/**
 * Pure date math behind the sleep-history calendar. No Android dependencies,
 * so every rule (Monday-first weeks, month grids, clamped forward navigation)
 * is unit-testable on the JVM.
 */
object SleepCalendar {

    /** Monday of the ISO week containing [anchor]. */
    fun startOfWeek(anchor: LocalDate): LocalDate =
        anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())

    /** The seven dates of the week containing [anchor], Monday first. */
    fun weekDays(anchor: LocalDate): List<LocalDate> {
        val start = startOfWeek(anchor)
        return (0L..6L).map { start.plusDays(it) }
    }

    /**
     * Full calendar-month grid for [anchor]'s month: whole weeks, Monday
     * first, with nulls padding the leading and trailing cells. The result
     * length is always a multiple of seven.
     */
    fun monthGrid(anchor: LocalDate): List<LocalDate?> {
        val first = anchor.withDayOfMonth(1)
        val lead = first.dayOfWeek.value - 1
        val daysInMonth = first.lengthOfMonth()
        val trailing = (7 - (lead + daysInMonth) % 7) % 7
        val cells = ArrayList<LocalDate?>(lead + daysInMonth + trailing)
        repeat(lead) { cells.add(null) }
        for (day in 1..daysInMonth) cells.add(first.withDayOfMonth(day))
        repeat(trailing) { cells.add(null) }
        return cells
    }

    /** The twelve months of [anchor]'s year, January first. */
    fun yearMonths(anchor: LocalDate): List<YearMonth> =
        (1..12).map { YearMonth.of(anchor.year, it) }

    /** One period before [anchor]. */
    fun previousPeriod(period: CalendarPeriod, anchor: LocalDate): LocalDate =
        when (period) {
            CalendarPeriod.WEEK -> anchor.minusWeeks(1)
            CalendarPeriod.MONTH -> anchor.minusMonths(1)
            CalendarPeriod.YEAR -> anchor.minusYears(1)
        }

    /**
     * One period after [anchor], clamped so the calendar can never point into
     * the future: if the next period would *start* after the period containing
     * [today], the anchor stays where it is.
     */
    fun nextPeriod(period: CalendarPeriod, anchor: LocalDate, today: LocalDate): LocalDate {
        val candidate = when (period) {
            CalendarPeriod.WEEK -> anchor.plusWeeks(1)
            CalendarPeriod.MONTH -> anchor.plusMonths(1)
            CalendarPeriod.YEAR -> anchor.plusYears(1)
        }
        val candidateStart = startOfPeriod(period, candidate)
        val currentStart = startOfPeriod(period, today)
        return if (candidateStart.isAfter(currentStart)) anchor else candidate
    }

    /** Whether forward navigation is still possible. */
    fun canGoNext(period: CalendarPeriod, anchor: LocalDate, today: LocalDate): Boolean =
        nextPeriod(period, anchor, today) != anchor

    /** First date of the period containing [date]. */
    fun startOfPeriod(period: CalendarPeriod, date: LocalDate): LocalDate =
        when (period) {
            CalendarPeriod.WEEK -> startOfWeek(date)
            CalendarPeriod.MONTH -> date.withDayOfMonth(1)
            CalendarPeriod.YEAR -> date.withDayOfYear(1)
        }

    /** Human-readable header for the anchored period. */
    fun title(period: CalendarPeriod, anchor: LocalDate, locale: Locale = Locale.getDefault()): String {
        val monthShort = { month: java.time.Month -> month.getDisplayName(TextStyle.SHORT, locale) }
        return when (period) {
            CalendarPeriod.WEEK -> {
                val start = startOfWeek(anchor)
                val end = start.plusDays(6)
                when {
                    start.year != end.year ->
                        "${start.dayOfMonth} ${monthShort(start.month)} ${start.year} – " +
                            "${end.dayOfMonth} ${monthShort(end.month)} ${end.year}"
                    start.month != end.month ->
                        "${start.dayOfMonth} ${monthShort(start.month)} – " +
                            "${end.dayOfMonth} ${monthShort(end.month)} ${end.year}"
                    else ->
                        "${start.dayOfMonth}–${end.dayOfMonth} ${monthShort(start.month)} ${start.year}"
                }
            }
            CalendarPeriod.MONTH ->
                "${anchor.month.getDisplayName(TextStyle.FULL, locale)} ${anchor.year}"
            CalendarPeriod.YEAR -> "${anchor.year}"
        }
    }

    /** Short weekday labels, Monday first, localized. */
    fun weekdayLabels(locale: Locale = Locale.getDefault()): List<String> =
        (1..7).map { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, locale) }

    /** Short month labels, January first, localized. */
    fun monthLabels(locale: Locale = Locale.getDefault()): List<String> =
        (1..12).map { java.time.Month.of(it).getDisplayName(TextStyle.SHORT, locale) }
}
