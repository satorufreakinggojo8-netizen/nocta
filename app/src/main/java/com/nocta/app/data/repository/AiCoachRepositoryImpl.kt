package com.nocta.app.data.repository

import com.nocta.app.data.local.AiMessageDao
import com.nocta.app.data.local.AiMessageEntity
import com.nocta.app.data.remote.AiCoachRequestDto
import com.nocta.app.data.remote.CoachFailures
import com.nocta.app.data.remote.NoctaApi
import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.MessageRole
import com.nocta.app.domain.repository.AiCoachRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import retrofit2.HttpException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * Streams the backend's Server-Sent-Events response for /v1/ai/coach and
 * emits progressively-growing AiMessage snapshots so the UI can render a
 * live "typing" effect (AIMessageBubble). Persists the final message locally
 * once the stream completes.
 *
 * Failures are emitted as `AiMessage(isError = true)` — never persisted — so
 * the UI can offer a retry instead of showing a half-answer as Vela's advice.
 */
class AiCoachRepositoryImpl @Inject constructor(
    private val api: NoctaApi,
    private val aiMessageDao: AiMessageDao
) : AiCoachRepository {

    override suspend fun saveUserMessage(message: AiMessage) {
        aiMessageDao.insert(
            AiMessageEntity(
                id = message.id,
                conversationId = requireNotNull(message.conversationId) { "User message must belong to a conversation." },
                role = MessageRole.USER.name,
                content = message.content,
                createdAtEpochMillis = message.createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        )
    }

    override fun observeMostRecentConversationId(): Flow<String?> =
        aiMessageDao.observeMostRecentConversationId()

    override fun ask(conversationId: String?, message: String): Flow<AiMessage> = callbackFlow {
        val messageId = UUID.randomUUID().toString()
        val convoId = conversationId ?: UUID.randomUUID().toString()
        val builder = StringBuilder()

        try {
            val response = api.askCoach(AiCoachRequestDto(convoId, message))
            if (!response.isSuccessful) throw HttpException(response)
            val body = response.body() ?: throw IllegalStateException("Empty AI response body")

            body.source().use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isEmpty()) continue
                    if (payload == "[DONE]") break

                    // Token frames are JSON; the backend's error path writes a
                    // plain-text data line, which is surfaced as the answer text
                    // rather than crashing the reader.
                    val json = runCatching { JSONObject(payload) }.getOrNull()
                    val token = if (json != null) {
                        val error = json.optString("error")
                        if (error.isNotEmpty()) throw IllegalStateException(error)
                        json.optString("token")
                    } else {
                        payload
                    }
                    if (token.isEmpty()) continue
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

            if (builder.isBlank()) throw IllegalStateException(CoachFailures.EMPTY)

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
            // Never surface a stack trace, and never persist a failure as advice.
            trySend(
                AiMessage(
                    id = messageId,
                    role = MessageRole.ASSISTANT,
                    content = CoachFailures.describe(t),
                    createdAt = LocalDateTime.now(),
                    isStreaming = false,
                    isError = true
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
                    // Reconstruct the real timestamp so restored history keeps
                    // its order and time instead of "now" for every bubble.
                    createdAt = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(it.createdAtEpochMillis),
                        ZoneId.systemDefault()
                    ),
                    conversationId = it.conversationId
                )
            }
        }

    override suspend fun deleteAllMessages() {
        aiMessageDao.deleteAll()
    }
}
