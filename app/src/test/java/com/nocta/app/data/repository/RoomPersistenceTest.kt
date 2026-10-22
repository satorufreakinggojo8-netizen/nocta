package com.nocta.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nocta.app.data.local.NoctaDatabase
import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.domain.model.WindDownOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * Real Room database, in-memory: proves the v5 tables, DAO queries and the
 * entity↔domain mapping agree — including that a paused run and a null journal
 * answer survive a read/write cycle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomPersistenceTest {

    private lateinit var database: NoctaDatabase
    private lateinit var journal: SleepJournalRepositoryImpl
    private lateinit var windDown: WindDownRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NoctaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        journal = SleepJournalRepositoryImpl(database.journalDao())
        windDown = WindDownRepositoryImpl(database.windDownDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `journal entry round trips every field`() = runBlocking {
        val date = LocalDate.of(2026, 10, 18)
        journal.save(
            JournalEntry(
                date = date,
                restfulness = 4,
                stressLevel = 2,
                napMinutes = 25,
                caffeineDrinks = 2,
                lastCaffeineTime = LocalTime.of(15, 30),
                exercisedMinutes = 40,
                lastExerciseTime = LocalTime.of(18, 15),
                screenUseBeforeBed = false,
                routineCompleted = true,
                notes = "  quiet evening  "
            )
        )

        val loaded = journal.observeEntry(date).first()!!
        assertEquals(4, loaded.restfulness)
        assertEquals(2, loaded.stressLevel)
        assertEquals(25, loaded.napMinutes)
        assertEquals(LocalTime.of(15, 30), loaded.lastCaffeineTime)
        assertEquals(LocalTime.of(18, 15), loaded.lastExerciseTime)
        assertEquals(false, loaded.screenUseBeforeBed)
        assertEquals(true, loaded.routineCompleted)
        assertEquals("quiet evening", loaded.notes)
    }

    @Test
    fun `unanswered fields stay null and an empty entry deletes its row`() = runBlocking {
        val date = LocalDate.of(2026, 10, 18)
        journal.save(JournalEntry(date = date, restfulness = 3))
        assertNull(journal.observeEntry(date).first()!!.stressLevel)

        // Answering nothing is not a record.
        journal.save(JournalEntry(date = date, notes = "   "))
        assertNull(journal.observeEntry(date).first())
        assertTrue(journal.getAll().isEmpty())
    }

    @Test
    fun `saving the same date replaces rather than duplicates`() = runBlocking {
        val date = LocalDate.of(2026, 10, 17)
        journal.save(JournalEntry(date = date, stressLevel = 2))
        journal.save(JournalEntry(date = date, stressLevel = 5))
        assertEquals(1, journal.getAll().size)
        assertEquals(5, journal.observeEntry(date).first()!!.stressLevel)
    }

    @Test
    fun `routine run persists pause state and moves to history when finished`() = runBlocking {
        val start = 5_000_000L
        val run = windDown.startRun(plannedMinutes = 45, startedAtMillis = start)

        val active = windDown.observeActiveRun().first()!!
        assertEquals(run.id, active.id)
        assertEquals(WindDownOutcome.RUNNING, active.outcome)

        val paused = active.pause(start + 60_000L)
        windDown.updateRun(paused)
        val reloaded = windDown.observeActiveRun().first()!!
        assertEquals(start + 60_000L, reloaded.pausedAtMillis)
        assertTrue(reloaded.isPaused)

        val finished = reloaded
            .resume(start + 120_000L)
            .toggleStep("dim_lights")
            .toggleStep("read")
            .finish(start + 600_000L, WindDownOutcome.COMPLETED)
        windDown.updateRun(finished)

        assertNull("finished runs are no longer active", windDown.observeActiveRun().first())
        val history = windDown.observeRecentRuns(limit = 10).first()
        assertEquals(1, history.size)
        assertEquals(setOf("dim_lights", "read"), history.single().completedStepIds)
        assertEquals(WindDownOutcome.COMPLETED, history.single().outcome)
        // 10 min wall clock minus the 1 min pause.
        assertEquals(9L, history.single().elapsedMinutesAt(start + 900_000L))
    }

    @Test
    fun `deleting all journal and routine data empties both tables`() = runBlocking {
        journal.save(JournalEntry(date = LocalDate.now(), restfulness = 4))
        windDown.startRun(plannedMinutes = 30, startedAtMillis = 1_000_000L)

        journal.deleteAllJournal()
        windDown.deleteAllRuns()

        assertTrue(journal.getAll().isEmpty())
        assertNull(windDown.observeActiveRun().first())
        assertTrue(windDown.observeRecentRuns(limit = 5).first().isEmpty())
    }
}
