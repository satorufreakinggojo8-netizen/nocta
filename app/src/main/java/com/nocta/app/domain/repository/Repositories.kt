

package com.nocta.app.domain.repository

import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.model.SleepTrackingSession
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SleepRepository {
    fun observeLastNight(): Flow<SleepSession?>
    fun observeRecentSessions(days: Int): Flow<List<SleepSession>>
    /** A single saved night, for the detailed report screen. */
    fun observeSession(id: String): Flow<SleepSession?>
    suspend fun logSession(session: SleepSession)
    suspend fun syncPending()
    /** Every saved night, oldest first — used by data export. */
    suspend fun getAllSessions(): List<SleepSession>
    /** Permanently removes all locally stored sleep data. */
    suspend fun deleteAllSleepData()
}

// Tracking is stored separately from the normalized nightly sleep log.
interface SleepTrackingRepository {
    fun observeActive(): Flow<SleepTrackingSession?>
    fun observeLatestCompleted(): Flow<SleepTrackingSession?>
    suspend fun start(startedAt: Instant): SleepTrackingSession
    suspend fun end(endedAt: Instant): SleepTrackingSession
    suspend fun discardActive()
    /** Permanently removes all locally stored tracking sessions. */
    suspend fun deleteAllTrackingData()
}

interface AiCoachRepository {
    /** Emits assistant tokens as they stream in; the final emission has isStreaming = false. */
    suspend fun saveUserMessage(message: AiMessage)
    fun observeMostRecentConversationId(): Flow<String?>
    fun ask(conversationId: String?, message: String): Flow<AiMessage>
    fun observeConversation(conversationId: String): Flow<List<AiMessage>>
    /** Permanently removes all locally stored coach messages. */
    suspend fun deleteAllMessages()
}
