

package com.nocta.app.domain.repository

import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.model.SleepTrackingSession   // NEW
import kotlinx.coroutines.flow.Flow
import java.time.Instant                                 // NEW

interface SleepRepository {
    fun observeLastNight(): Flow<SleepSession?>
    fun observeRecentSessions(days: Int): Flow<List<SleepSession>>
    fun computeScore(sessions: List<SleepSession>): SleepScoreBreakdown
    suspend fun logSession(session: SleepSession)
    suspend fun syncPending()
}

// NEW — tracking is a separate repository so the existing SleepRepository
// (and the CalculateSleepScore pipeline it feeds) stays untouched.
interface SleepTrackingRepository {
    fun observeActive(): Flow<SleepTrackingSession?>
    fun observeLatestCompleted(): Flow<SleepTrackingSession?>
    suspend fun start(startedAt: Instant): SleepTrackingSession
    suspend fun end(endedAt: Instant): SleepTrackingSession
    suspend fun discardActive()
}

interface AiCoachRepository {
    /** Emits assistant tokens as they stream in; the final emission has isStreaming = false. */
    fun ask(conversationId: String?, message: String): Flow<AiMessage>
    fun observeConversation(conversationId: String): Flow<List<AiMessage>>
}
