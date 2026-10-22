package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.domain.profile.SleepProfile
import com.nocta.app.testing.testRun
import com.nocta.app.testing.testSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class BuildCoachContextTest {

    private val builder = BuildCoachContext()
    private val today = LocalDate.of(2026, 10, 20)

    @Test
    fun `with no records the context says so instead of inventing numbers`() {
        val context = builder(CoachContextInput(prompt = "How did I sleep?"))

        assertFalse(context.hasSleepData)
        assertTrue(context.facts.none { it.startsWith(CoachContext.NIGHTS_PREFIX) })
        assertTrue(context.missing.any { it.contains("No sleep nights logged") })
        assertTrue(context.missing.any { it.contains("Age not set") })
        assertTrue(context.missing.any { it.contains("No journal entries") })

        val envelope = context.envelope("How did I sleep?")
        assertFalse(envelope.contains("Sleep Context"))
        assertTrue(envelope.contains("Missing Data"))
        assertTrue(envelope.endsWith("How did I sleep?"))
    }

    @Test
    fun `facts are computed from the stored nights`() {
        val sessions = listOf(
            testSession(today.minusDays(1), 420),
            testSession(today.minusDays(2), 480),
            testSession(today.minusDays(3), 450)
        )
        val context = builder(
            CoachContextInput(
                prompt = "Review my week",
                sessions = sessions,
                profile = SleepProfile(age = 30)
            )
        )

        assertTrue(context.hasSleepData)
        val text = context.facts.joinToString("; ")
        assertTrue(text, text.contains("Nights logged: 3"))
        assertTrue(text, text.contains("Average asleep: 7h 30m"))
        assertTrue(text, text.contains("Age 30"))
        // Three nights is exactly the threshold, so no "too thin" warning.
        assertTrue(context.missing.none { it.contains("Fewer than 3") })
    }

    @Test
    fun `thin history is flagged rather than turned into a trend`() {
        val context = builder(
            CoachContextInput(
                prompt = "Any trends?",
                sessions = listOf(testSession(today, 400))
            )
        )
        assertTrue(context.missing.any { it.contains("Fewer than 3 nights") })
    }

    @Test
    fun `journal notes never leave the device but derived counts do`() {
        val date = today.minusDays(1)
        val context = builder(
            CoachContextInput(
                prompt = "Patterns?",
                sessions = listOf(testSession(date, 400)),
                profile = SleepProfile(age = 25),
                journalEntries = listOf(
                    JournalEntry(
                        date = date,
                        stressLevel = 5,
                        screenUseBeforeBed = true,
                        notes = "my secret private note about work"
                    )
                ),
                completedWindDownRuns = listOf(testRun(date))
            )
        )

        val envelope = context.envelope("Patterns?")
        assertFalse("notes must not be sent", envelope.contains("secret"))
        assertFalse(envelope.contains("private note"))
        assertTrue(envelope.contains("Journal days answered: 1"))
        assertTrue(envelope.contains("High stress (4-5/5) on 1 of 1 journal days"))
        assertTrue(envelope.contains("Completed wind-down runs: 1"))
        assertTrue(envelope.contains("no sleep stages are measured"))
    }

    @Test
    fun `weakest factor comes from the real score breakdown`() {
        val context = builder(
            CoachContextInput(
                prompt = "Explain",
                sessions = listOf(testSession(today, 400)),
                profile = SleepProfile(age = 30),
                score = com.nocta.app.domain.model.SleepScoreBreakdown(
                    durationScore = 40,
                    consistencyScore = 80,
                    timingScore = 70,
                    qualityScore = 60,
                    routineScore = 90,
                    trackedNights = 4
                )
            )
        )
        assertTrue(context.facts.any { it.contains("Weakest factor: duration") })
        assertTrue(context.facts.any { it.contains("Sleep score") })
    }

    @Test
    fun `late caffeine count only includes answered days`() {
        val d1 = today.minusDays(1)
        val d2 = today.minusDays(2)
        val context = builder(
            CoachContextInput(
                prompt = "x",
                sessions = listOf(testSession(d1, 400), testSession(d2, 400)),
                profile = SleepProfile(age = 25),
                journalEntries = listOf(
                    JournalEntry(date = d1, caffeineDrinks = 1, lastCaffeineTime = LocalTime.of(16, 0)),
                    JournalEntry(date = d2, caffeineDrinks = 1, lastCaffeineTime = LocalTime.of(8, 0))
                )
            )
        )
        assertTrue(
            context.facts.any { it.contains("Late caffeine (after 14:00) on 1 of 2 journal days") }
        )
        assertEquals(1, context.facts.count { it.startsWith("Journal days") })
        assertEquals(1, context.facts.count { it.startsWith("Late caffeine") })
    }
}
