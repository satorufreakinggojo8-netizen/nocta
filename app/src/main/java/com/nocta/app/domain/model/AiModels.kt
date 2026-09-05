package com.nocta.app.domain.model

import java.time.LocalDateTime

enum class MessageRole { USER, ASSISTANT }

data class AiMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val createdAt: LocalDateTime,
    val isStreaming: Boolean = false
)

data class AiConversation(
    val id: String,
    val messages: List<AiMessage>,
    val startedAt: LocalDateTime
)

/** A ready-made prompt shown as a tappable chip on the AI Coach landing screen. */
data class SuggestedPrompt(val label: String, val prompt: String)

val defaultSuggestedPrompts = listOf(
    SuggestedPrompt("Analyze my sleep", "Analyze my recent sleep and tell me what stands out."),
    SuggestedPrompt("Optimize tonight", "What's the single best thing I can change tonight?"),
    SuggestedPrompt("Build my routine", "Build me a wind-down routine based on my habits."),
    SuggestedPrompt("Why am I tired?", "Why might I be feeling tired today?")
)
