package com.nocta.app.testing

import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.DataSource
import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.domain.model.MorningCheckIn
import com.nocta.app.domain.model.SchedulePreset
import com.nocta.app.domain.model.SleepScoreCardMode
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.model.SleepTrackingSession
import com.nocta.app.domain.model.WindDownOutcome
import com.nocta.app.domain.model.WindDownRun
import com.nocta.app.domain.repository.AiCoachRepository
import com.nocta.app.domain.repository.MorningCheckInRepository
import com.nocta.app.domain.repository.SettingsRepository
import com.nocta.app.domain.repository.SleepJournalRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.repository.SleepTrackingRepository
import com.nocta.app.domain.repository.WindDownRepository
import com.nocta.app.domain.repository.WindDownStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** A valid self-reported night of [minutes] asleep, starting at 23:00. */
fun testSession(
    date: LocalDate,
    minutes: Long,
    quality: Int = 3
): SleepSession {
    val sleep = date.atTime(23, 0)
    return SleepSession(
        id = "session-$date",
        date = date,
        bedtime = sleep,
        sleepAttemptTime = sleep,
        estimatedSleepTime = sleep,
        wakeTime = sleep.plusMinutes(minutes),
        nightAwakenings = 0,
        sleepQuality = quality,
        morningEnergy = quality,
        source = DataSource.SELF_REPORTED
    )
}

fun testRun(
    date: LocalDate,
    plannedMinutes: Int = 45,
    completed: Boolean = true,
    steps: Set<String> = setOf("dim_lights")
): WindDownRun = WindDownRun(
    id = "run-$date",
    date = date,
    plannedMinutes = plannedMinutes,
    startedAtMillis = 1_000_000L,
    endedAtMillis = 1_000_000L + plannedMinutes * 60_000L,
    outcome = if (completed) WindDownOutcome.COMPLETED else WindDownOutcome.DISMISSED,
    completedStepIds = steps
)

class FakeSleepRepository(
    var sessions: List<SleepSession> = emptyList()
) : SleepRepository {
    var deleted = false
        private set

    override fun observeLastNight(): Flow<SleepSession?> =
        flowOf(sessions.maxByOrNull { it.date })

    override fun observeRecentSessions(days: Int): Flow<List<SleepSession>> = flowOf(sessions)

    override fun observeSession(id: String): Flow<SleepSession?> =
        flowOf(sessions.firstOrNull { it.id == id })

    override suspend fun logSession(session: SleepSession) {
        sessions = sessions + session
    }

    override suspend fun syncPending() = Unit

    override suspend fun getAllSessions(): List<SleepSession> = sessions

    override suspend fun deleteAllSleepData() {
        deleted = true
        sessions = emptyList()
    }
}

class FakeTrackingRepository : SleepTrackingRepository {
    var deleted = false
        private set

    override fun observeActive(): Flow<SleepTrackingSession?> = flowOf(null)
    override fun observeLatestCompleted(): Flow<SleepTrackingSession?> = flowOf(null)
    override suspend fun start(startedAt: Instant) = SleepTrackingSession(
        id = "t", startedAt = startedAt, endedAt = null, source = DataSource.ESTIMATED
    )
    override suspend fun end(endedAt: Instant) = SleepTrackingSession(
        id = "t", startedAt = Instant.EPOCH, endedAt = endedAt, source = DataSource.ESTIMATED
    )
    override suspend fun discardActive() = Unit
    override suspend fun discardSession(sessionId: String) = Unit
    override suspend fun deleteAllTrackingData() {
        deleted = true
    }
}

class FakeCheckInRepository : MorningCheckInRepository {
    var deleted = false
        private set

    override fun observeForSession(trackingSessionId: String): Flow<MorningCheckIn?> = flowOf(null)
    override suspend fun getForSession(trackingSessionId: String): MorningCheckIn? = null
    override suspend fun save(trackingSessionId: String, checkIn: MorningCheckIn) = Unit
    override suspend fun deleteAllCheckIns() {
        deleted = true
    }
}

class FakeJournalRepository(
    var entries: List<JournalEntry> = emptyList()
) : SleepJournalRepository {
    var deleted = false
        private set

    override fun observeEntry(date: LocalDate): Flow<JournalEntry?> =
        flowOf(entries.firstOrNull { it.date == date })

    override fun observeRecent(days: Int): Flow<List<JournalEntry>> = flowOf(entries)

    override suspend fun save(entry: JournalEntry) {
        entries = entries.filterNot { it.date == entry.date } + entry
    }

    override suspend fun delete(date: LocalDate) {
        entries = entries.filterNot { it.date == date }
    }

    override suspend fun deleteAllJournal() {
        deleted = true
        entries = emptyList()
    }

    override suspend fun getAll(): List<JournalEntry> = entries

    override suspend fun getRecent(days: Int): List<JournalEntry> = entries
}

class FakeWindDownRepository(
    var runs: List<WindDownRun> = emptyList()
) : WindDownRepository {
    var deleted = false
        private set

    override fun observeActiveRun(): Flow<WindDownRun?> = flowOf(null)
    override fun observeRecentRuns(limit: Int): Flow<List<WindDownRun>> = flowOf(runs)
    override suspend fun activeRun(): WindDownRun? = null

    override suspend fun startRun(plannedMinutes: Int, startedAtMillis: Long) = WindDownRun(
        id = "new", date = LocalDate.now(), plannedMinutes = plannedMinutes, startedAtMillis = startedAtMillis
    )

    override suspend fun updateRun(run: WindDownRun) = Unit

    override suspend fun deleteAllRuns() {
        deleted = true
        runs = emptyList()
    }

    override suspend fun completedRunsForExport(): List<WindDownRun> =
        runs.filter { it.outcome == WindDownOutcome.COMPLETED }
}

class FakeSettingsRepository(
    var wakeTime: LocalTime = LocalTime.of(7, 0),
    var windDownMinutes: Int = 45,
    var activePreset: SchedulePreset = SchedulePreset.CUSTOM,
    var steps: List<WindDownStep> = listOf(WindDownStep("dim_lights", "Dim the lights", true))
) : SettingsRepository {
    private val presetWakeTimes = mutableMapOf<SchedulePreset, LocalTime>()

    override fun observeSleepScoreCardMode(): Flow<SleepScoreCardMode> =
        flowOf(SleepScoreCardMode.STANDARD)

    override suspend fun setSleepScoreCardMode(mode: SleepScoreCardMode) = Unit

    override fun observeWakeTime(): Flow<LocalTime> = flowOf(wakeTime)

    override suspend fun setWakeTime(time: LocalTime) {
        wakeTime = time
    }

    override suspend fun getWakeTime(): LocalTime = wakeTime

    override fun observeActivePreset(): Flow<SchedulePreset> = flowOf(activePreset)

    override suspend fun setActivePreset(preset: SchedulePreset) {
        activePreset = preset
    }

    override suspend fun getActivePreset(): SchedulePreset = activePreset

    override suspend fun getPresetWakeTime(preset: SchedulePreset): LocalTime? =
        presetWakeTimes[preset]

    override suspend fun setPresetWakeTime(preset: SchedulePreset, time: LocalTime) {
        presetWakeTimes[preset] = time
    }

    override fun observeWindDownMinutes(): Flow<Int> = flowOf(windDownMinutes)

    override suspend fun setWindDownMinutes(minutes: Int) {
        windDownMinutes = minutes
    }

    override suspend fun getWindDownMinutes(): Int = windDownMinutes

    override fun observeWindDownSteps(): Flow<List<WindDownStep>> = flowOf(steps)

    override suspend fun setWindDownSteps(steps: List<WindDownStep>) {
        this.steps = steps
    }

    override suspend fun getWindDownSteps(): List<WindDownStep> = steps
}

class FakeAiCoachRepository : AiCoachRepository {
    var deleted = false
        private set

    override suspend fun saveUserMessage(message: AiMessage) = Unit
    override fun observeMostRecentConversationId(): Flow<String?> = flowOf(null)
    override fun ask(conversationId: String?, message: String): Flow<AiMessage> = flowOf()
    override fun observeConversation(conversationId: String): Flow<List<AiMessage>> = flowOf(emptyList())
    override suspend fun deleteAllMessages() {
        deleted = true
    }
}
