package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.testing.FakeCheckInRepository
import com.nocta.app.testing.FakeJournalRepository
import com.nocta.app.testing.FakeSettingsRepository
import com.nocta.app.testing.FakeSleepRepository
import com.nocta.app.testing.FakeWindDownRepository
import com.nocta.app.testing.testRun
import com.nocta.app.testing.testSession
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * org.json is a real implementation only under Robolectric; a plain JVM test
 * would hit the stubbed android.jar and fail for the wrong reason.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExportSleepDataTest {

    private val date = LocalDate.of(2026, 10, 19)

    private fun export(
        sleep: FakeSleepRepository = FakeSleepRepository(listOf(testSession(date, 435))),
        journal: FakeJournalRepository = FakeJournalRepository(
            listOf(
                JournalEntry(
                    date = date,
                    restfulness = 4,
                    stressLevel = 3,
                    napMinutes = 20,
                    caffeineDrinks = 2,
                    lastCaffeineTime = LocalTime.of(15, 30),
                    screenUseBeforeBed = true,
                    routineCompleted = true,
                    notes = "kept it calm"
                )
            )
        ),
        windDown: FakeWindDownRepository = FakeWindDownRepository(
            listOf(testRun(date, plannedMinutes = 45))
        )
    ) = runBlocking {
        ExportSleepData(
            sleep,
            FakeCheckInRepository(),
            journal,
            windDown,
            FakeSettingsRepository()
        )()
    }

    @Test
    fun `export includes nights journal routines and settings`() {
        val root = JSONObject(export())

        assertEquals("Nocta", root.getString("app"))
        assertEquals(1, root.getJSONArray("nights").length())
        assertEquals(435, root.getJSONArray("nights").getJSONObject(0).getInt("timeAsleepMinutes"))

        val journal = root.getJSONArray("journal")
        assertEquals(1, journal.length())
        assertEquals("kept it calm", journal.getJSONObject(0).getString("notes"))
        assertEquals("15:30", journal.getJSONObject(0).getString("lastCaffeineTime"))
        assertTrue(journal.getJSONObject(0).getBoolean("routineCompleted"))

        val runs = root.getJSONArray("windDownRuns")
        assertEquals(1, runs.length())
        assertEquals("COMPLETED", runs.getJSONObject(0).getString("outcome"))
        assertEquals(45, runs.getJSONObject(0).getInt("plannedMinutes"))
        assertEquals(45, runs.getJSONObject(0).getInt("elapsedMinutes"))

        val settings = root.getJSONObject("settings")
        assertEquals("07:00", settings.getString("wakeTime"))
        assertEquals(45, settings.getInt("windDownMinutes"))
        assertEquals(1, settings.getJSONArray("windDownSteps").length())
    }

    @Test
    fun `empty answers export as null rather than zero`() {
        val journal = FakeJournalRepository(listOf(JournalEntry(date = date, restfulness = 2)))
        val root = JSONObject(export(journal = journal))
        val entry = root.getJSONArray("journal").getJSONObject(0)

        assertEquals(2, entry.getInt("restfulness"))
        assertTrue(entry.isNull("stressLevel"))
        assertTrue(entry.isNull("caffeineDrinks"))
        assertTrue(entry.isNull("notes"))
    }

    @Test
    fun `dismissed routines are not exported as completed history`() {
        val windDown = FakeWindDownRepository(
            listOf(testRun(date, completed = false), testRun(date.minusDays(1)))
        )
        val root = JSONObject(export(windDown = windDown))
        assertEquals(1, root.getJSONArray("windDownRuns").length())
    }
}
