package com.nocta.app.domain.usecase

import com.nocta.app.testing.FakeCheckInRepository
import com.nocta.app.testing.FakeJournalRepository
import com.nocta.app.testing.FakeSleepRepository
import com.nocta.app.testing.FakeTrackingRepository
import com.nocta.app.testing.FakeWindDownRepository
import com.nocta.app.testing.testSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DeleteAllSleepDataTest {

    @Test
    fun `deleting all sleep data reaches every repository`() = runBlocking {
        val sleep = FakeSleepRepository(listOf(testSession(LocalDate.now(), 420)))
        val tracking = FakeTrackingRepository()
        val checkIns = FakeCheckInRepository()
        val journal = FakeJournalRepository()
        val windDown = FakeWindDownRepository()

        DeleteAllSleepData(sleep, tracking, checkIns, journal, windDown)()

        assertTrue("sleep sessions", sleep.deleted)
        assertTrue("tracking sessions", tracking.deleted)
        assertTrue("morning check-ins", checkIns.deleted)
        assertTrue("journal entries", journal.deleted)
        assertTrue("wind-down runs", windDown.deleted)
        assertEquals(emptyList<Any>(), sleep.sessions)
    }
}
