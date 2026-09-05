package com.nocta.app.domain.repository

import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepSession
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    fun observeLastNight(): Flow<SleepSession?>
    fun observeRecentSessions(days: Int): Flow<List<SleepSession>>
    fun computeScore(sessions: List<SleepSession>): SleepScoreBreakdown
    suspend fun logSession(session: SleepSession)
    suspend fun syncPending()
}

interface AiCoachRepository {
    /** Emits assistant tokens as they stream in; the final emission has isStreaming = false. */
    fun ask(conversationId: String?, message: String): Flow<AiMessage>
    fun observeConversation(conversationId: String): Flow<List<AiMessage>>
}
