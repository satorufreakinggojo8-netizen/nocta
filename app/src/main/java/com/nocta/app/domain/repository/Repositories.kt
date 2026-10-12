

package com.nocta.app.domain.repository

import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.model.SleepTrackingSession
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SleepRepository {
    fun observeLastNight(): Flow<SleepSession?>
    fun observeRecentSessions(days: Int): Flow<List<SleepSession>>
    suspend fun logSession(session: SleepSession)
    suspend fun syncPending()
}

// Tracking is stored separately from the normalized nightly sleep log.
interface SleepTrackingRepository {
    fun observeActive(): Flow<SleepTrackingSession?>
    fun observeLatestCompleted(): Flow<SleepTrackingSession?>
    suspend fun start(startedAt: Instant): SleepTrackingSession
    suspend fun end(endedAt: Instant): SleepTrackingSession
    suspend fun discardActive()
}

interface AiCoachRepository {
    /** Emits assistant tokens as they stream in; the final emission has isStreaming = false. */
    suspend fun saveUserMessage(message: AiMessage)
    fun observeMostRecentConversationId(): Flow<String?>
    fun ask(conversationId: String?, message: String): Flow<AiMessage>
    fun observeConversation(conversationId: String): Flow<List<AiMessage>>
}
