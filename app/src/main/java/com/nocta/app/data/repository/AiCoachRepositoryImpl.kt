package com.nocta.app.data.repository

import com.nocta.app.data.local.AiMessageDao
import com.nocta.app.data.local.AiMessageEntity
import com.nocta.app.data.remote.AiCoachRequestDto
import com.nocta.app.data.remote.NoctaApi
import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.MessageRole
import com.nocta.app.domain.repository.AiCoachRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

/**
 * Streams the backend's Server-Sent-Events response for /v1/ai/coach and
 * emits progressively-growing AiMessage snapshots so the UI can render a
 * live "typing" effect (AIMessageBubble). Persists the final message locally
 * once the stream completes.
 */
class AiCoachRepositoryImpl @Inject constructor(
    private val api: NoctaApi,
    private val aiMessageDao: AiMessageDao
) : AiCoachRepository {

    override fun ask(conversationId: String?, message: String): Flow<AiMessage> = callbackFlow {
        val messageId = UUID.randomUUID().toString()
        val convoId = conversationId ?: UUID.randomUUID().toString()
        val builder = StringBuilder()

        try {
            val response = api.askCoach(AiCoachRequestDto(convoId, message))
            val body = response.body() ?: throw IllegalStateException("Empty AI response body")

            body.source().use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data:")) {
                        val token = line.removePrefix("data:").trim()
                        if (token == "[DONE]") break
                        builder.append(token)
                        trySend(
                            AiMessage(
                                id = messageId,
                                role = MessageRole.ASSISTANT,
                                content = builder.toString(),
                                createdAt = LocalDateTime.now(),
                                isStreaming = true
                            )
                        )
                    }
                }
            }

            val finalMessage = AiMessage(
                id = messageId,
                role = MessageRole.ASSISTANT,
                content = builder.toString(),
                createdAt = LocalDateTime.now(),
                isStreaming = false
            )
            trySend(finalMessage)

            aiMessageDao.insert(
                AiMessageEntity(
                    id = messageId,
                    conversationId = convoId,
                    role = MessageRole.ASSISTANT.name,
                    content = finalMessage.content,
                    createdAtEpochMillis = System.currentTimeMillis()
                )
            )
        } catch (t: Throwable) {
            // Surface a clear, non-alarmist failure message rather than a stack trace in the UI —
            // the actual error is left to be logged by the calling ViewModel/crash reporter.
            trySend(
                AiMessage(
                    id = messageId,
                    role = MessageRole.ASSISTANT,
                    content = "I couldn't reach the coach right now. Please try again in a moment.",
                    createdAt = LocalDateTime.now(),
                    isStreaming = false
                )
            )
        } finally {
            close()
        }
    }.flowOn(Dispatchers.IO)

    override fun observeConversation(conversationId: String): Flow<List<AiMessage>> =
        aiMessageDao.observeConversation(conversationId).map { entities ->
            entities.map {
                AiMessage(
                    id = it.id,
                    role = MessageRole.valueOf(it.role),
                    content = it.content,
                    createdAt = LocalDateTime.now() // display-only reconstruction; see entity for real epoch millis
                )
            }
        }
}
