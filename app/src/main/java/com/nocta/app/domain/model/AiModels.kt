package com.nocta.app.domain.model

import java.time.LocalDateTime

enum class MessageRole { USER, ASSISTANT }

data class AiMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val createdAt: LocalDateTime,
    val isStreaming: Boolean = false,
    val conversationId: String? = null,
    /**
     * True for a delivery failure bubble. Error messages are never persisted and
     * are rendered with a retry affordance instead of being treated as Vela's
     * answer.
     */
    val isError: Boolean = false
)

data class AiConversation(
    val id: String,
    val messages: List<AiMessage>,
    val startedAt: LocalDateTime
)

/** A ready-made prompt shown as a tappable chip on the AI Coach landing screen. */
data class SuggestedPrompt(val label: String, val prompt: String)

/**
 * Vela's starter actions. Each one names the records it should use, and the
 * ones that need history say so, so tapping a chip never produces an invented
 * trend.
 */
val defaultSuggestedPrompts = listOf(
    SuggestedPrompt(
        "Analyze last night",
        "Analyze my last recorded night. Use only my logged data and tell me what stands out."
    ),
    SuggestedPrompt(
        "Plan tonight",
        "Help me plan tonight around my recorded bedtime and wake time. Keep it to one or two changes."
    ),
    SuggestedPrompt(
        "Explain my score",
        "Explain my sleep score factor by factor in plain language, using my actual numbers."
    ),
    SuggestedPrompt(
        "Review my week",
        "Review my last seven days of records. Only claim what the data supports, and say when it is too thin for a trend."
    ),
    SuggestedPrompt(
        "Journal patterns",
        "Look for possible patterns between my journal entries and my sleep. Call them possible associations, not causes."
    ),
    SuggestedPrompt(
        "One improvement",
        "Suggest one achievable improvement based on my recorded sleep and routine."
    ),
    SuggestedPrompt(
        "What's missing?",
        "What information is missing that would make your analysis more useful? Tell me exactly what to log."
    )
)
