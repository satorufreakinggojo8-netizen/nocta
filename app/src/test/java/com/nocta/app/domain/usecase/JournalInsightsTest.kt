package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.testing.testSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class JournalInsightsTest {

    private val insights = JournalInsights()
    private val baseDate = LocalDate.of(2026, 10, 1)

    private fun days(count: Int) = (0 until count).map { baseDate.plusDays(it.toLong()) }

    @Test
    fun `late caffeine pattern is reported once both sides have three nights`() {
        val dates = days(6)
        // Three short nights with late caffeine, three long nights without.
        val sessions = dates.mapIndexed { index, date ->
            testSession(date, if (index < 3) 360 else 480)
        }
        val entries = dates.mapIndexed { index, date ->
            if (index < 3) {
                JournalEntry(
                    date = date,
                    caffeineDrinks = 2,
                    lastCaffeineTime = LocalTime.of(17, 30)
                )
            } else {
                JournalEntry(date = date, caffeineDrinks = 1, lastCaffeineTime = LocalTime.of(9, 0))
            }
        }

        val patterns = insights(entries, sessions)
        val caffeine = patterns.firstOrNull { it.id == "caffeine_late" }

        assertTrue("expected a late-caffeine pattern, got $patterns", caffeine != null)
        assertEquals(3, caffeine!!.nightsWithFactor)
        assertEquals(3, caffeine.nightsWithoutFactor)
        assertEquals(-120L, caffeine.differenceMinutes)
        assertTrue(caffeine.description.contains("less sleep"))
    }

    @Test
    fun `no pattern when one side has fewer than three nights`() {
        val dates = days(5)
        val sessions = dates.map { testSession(it, 420) }
        val entries = dates.mapIndexed { index, date ->
            JournalEntry(
                date = date,
                screenUseBeforeBed = index < 2 // only two nights with screens
            )
        }

        assertTrue(insights(entries, sessions).none { it.id == "screens" })
    }

    @Test
    fun `unanswered fields are excluded rather than treated as false`() {
        val dates = days(6)
        val sessions = dates.mapIndexed { index, date -> testSession(date, if (index < 3) 300 else 480) }
        val entries = dates.mapIndexed { index, date ->
            // Three answered "screens = true", the rest never answered.
            JournalEntry(date = date, screenUseBeforeBed = if (index < 3) true else null)
        }

        assertTrue(insights(entries, sessions).none { it.id == "screens" })
    }

    @Test
    fun `small differences are not reported as patterns`() {
        val dates = days(6)
        val sessions = dates.mapIndexed { index, date -> testSession(date, if (index < 3) 420 else 425) }
        val entries = dates.mapIndexed { index, date ->
            JournalEntry(date = date, screenUseBeforeBed = index < 3)
        }

        assertTrue(insights(entries, sessions).isEmpty())
    }

    @Test
    fun `journal days without a sleep record are ignored`() {
        val dates = days(6)
        val sessions = dates.take(3).map { testSession(it, 420) }
        val entries = dates.map { JournalEntry(date = it, screenUseBeforeBed = true) }

        assertTrue(insights(entries, sessions).isEmpty())
    }

    @Test
    fun `caffeine cutoff is midnight-safe and drinks zero never counts as late`() {
        val dates = days(6)
        val sessions = dates.mapIndexed { index, date ->
            testSession(date, if (index < 3) 330 else 480)
        }
        val entries = dates.mapIndexed { index, date ->
            if (index < 3) {
                JournalEntry(date = date, caffeineDrinks = 1, lastCaffeineTime = LocalTime.of(23, 45))
            } else {
                // Coffee early but zero drinks recorded — must not be "late".
                JournalEntry(date = date, caffeineDrinks = 0, lastCaffeineTime = LocalTime.of(18, 0))
            }
        }

        val pattern = insights(entries, sessions).firstOrNull { it.id == "caffeine_late" }
        assertEquals(3, pattern?.nightsWithFactor)
        assertEquals(3, pattern?.nightsWithoutFactor)
    }
}
