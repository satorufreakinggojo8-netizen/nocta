package com.nocta.app.domain.model

import java.time.LocalDateTime

enum class MessageRole { USER, ASSISTANT }

data class AiMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val createdAt: LocalDateTime,
    val isStreaming: Boolean = false,
    val conversationId: String? = null
)

data class AiConversation(
    val id: String,
    val messages: List<AiMessage>,
    val startedAt: LocalDateTime
)

/** A ready-made prompt shown as a tappable chip on the AI Coach landing screen. */
data class SuggestedPrompt(val label: String, val prompt: String)

val defaultSuggestedPrompts = listOf(
    SuggestedPrompt("Analyze my last night", "Analyze my last night's sleep and tell me what stands out."),
    SuggestedPrompt("Explain my score", "Explain my sleep score factor by factor in plain language."),
    SuggestedPrompt("Build my bedtime routine", "Build me a bedtime routine based on my recorded habits."),
    SuggestedPrompt("Review my week", "Give me a weekly review of my sleep trends — only what my records support."),
    SuggestedPrompt("Fix my schedule", "Help me improve my schedule consistency. What should I change first?")
)
