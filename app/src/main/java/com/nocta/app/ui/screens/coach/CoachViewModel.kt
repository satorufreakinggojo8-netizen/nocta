package com.nocta.app.ui.screens.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.MessageRole
import com.nocta.app.domain.repository.AiCoachRepository
import com.nocta.app.domain.repository.SleepProfileRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.usecase.CalculateSleepScore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

enum class CompanionExpression { IDLE, THINKING, RESPONDING }

data class CoachUiState(
    val conversationId: String? = null,
    val messages: List<AiMessage> = emptyList(),
    val isSending: Boolean = false,
    val inputText: String = "",
    val expression: CompanionExpression = CompanionExpression.IDLE,
    val userAge: Int? = null,
    val targetSleepHours: Double? = null,
    val lastNightDurationHours: Double? = null,
    val lastUserMessageText: String? = null,
    /** Real, computed sleep context sent with every message — never invented. */
    val scoreOverall: Int? = null,
    val scoreLabel: String? = null,
    val weakestFactor: String? = null,
    val sevenDayAvgHours: Double? = null,
    val trackedNightsLast14: Int = 0
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val aiCoachRepository: AiCoachRepository,
    private val sleepProfileRepository: SleepProfileRepository,
    private val sleepRepository: SleepRepository,
    private val calculateSleepScore: CalculateSleepScore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    init {
        observeContextData()
        observeLatestSavedConversation()
    }

    private fun observeLatestSavedConversation() {
        viewModelScope.launch {
            aiCoachRepository.observeMostRecentConversationId().collectLatest { conversationId ->
                if (conversationId != null) {
                    aiCoachRepository.observeConversation(conversationId).collect { messages ->
                        _uiState.value = _uiState.value.copy(
                            conversationId = conversationId,
                            messages = messages
                        )
                    }
                }
            }
        }
    }

    private fun observeContextData() {
        viewModelScope.launch {
            sleepProfileRepository.observeProfile().collect { profile ->
                if (profile != null) {
                    val recommendedMidMinutes = (profile.recommendedMinMinutes + profile.recommendedMaxMinutes) / 2.0
                    _uiState.value = _uiState.value.copy(
                        userAge = profile.age,
                        targetSleepHours = recommendedMidMinutes / 60.0
                    )
                }
            }
        }
        viewModelScope.launch {
            sleepRepository.observeLastNight().collect { lastSession ->
                if (lastSession != null) {
                    val durationHours = lastSession.durationMinutes / 60.0
                    _uiState.value = _uiState.value.copy(lastNightDurationHours = durationHours)
                }
            }
        }
        viewModelScope.launch {
            // Real score context so Vela explains the user's actual numbers
            // instead of guessing. Insufficient history stays null — the
            // context block then says so rather than fabricating a score.
            sleepRepository.observeRecentSessions(days = 14).collect { recent ->
                if (recent.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        scoreOverall = null,
                        scoreLabel = null,
                        weakestFactor = null,
                        sevenDayAvgHours = null,
                        trackedNightsLast14 = 0
                    )
                    return@collect
                }
                val breakdown = calculateSleepScore(recent)
                val weakest = listOf(
                    "duration" to breakdown.durationScore,
                    "schedule consistency" to breakdown.consistencyScore,
                    "sleep quality" to breakdown.qualityScore,
                    "schedule timing" to breakdown.timingScore,
                    "night awakenings" to breakdown.routineScore
                ).minByOrNull { it.second }?.first
                val last7 = recent.take(7)
                _uiState.value = _uiState.value.copy(
                    scoreOverall = breakdown.overall,
                    scoreLabel = breakdown.label,
                    weakestFactor = weakest,
                    sevenDayAvgHours = last7.map { it.durationMinutes }.average() / 60.0,
                    trackedNightsLast14 = recent.size
                )
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun retryLastMessage() {
        val lastText = _uiState.value.lastUserMessageText
        if (!lastText.isNullOrBlank() && !_uiState.value.isSending) {
            // Remove the failed error message if present
            val filteredMessages = _uiState.value.messages.filterNot {
                it.role == MessageRole.ASSISTANT && it.content.contains("couldn't reach", ignoreCase = true)
            }
            _uiState.value = _uiState.value.copy(messages = filteredMessages)
            send(promptOverride = lastText)
        }
    }

    fun send(promptOverride: String? = null) {
        val text = promptOverride ?: _uiState.value.inputText
        if (text.isBlank() || _uiState.value.isSending) return

        val currentState = _uiState.value
        val conversationId = currentState.conversationId ?: UUID.randomUUID().toString()
        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = text,
            createdAt = LocalDateTime.now(),
            conversationId = conversationId
        )

        // Real context only: every line below is computed from stored records.
        // With no history at all the block is empty and Vela must say data is
        // missing rather than inventing numbers (backend prompt enforces this).
        val contextInfo = buildList {
            currentState.userAge?.let { add("User Age: $it") }
            currentState.targetSleepHours?.let { add("Target Sleep Goal: ${"%.1f".format(it)} hrs") }
            currentState.lastNightDurationHours?.let { add("Last Night Sleep: ${"%.1f".format(it)} hrs") }
            currentState.scoreOverall?.let { overall ->
                add("Sleep Score (14-night window): $overall (${currentState.scoreLabel})")
                currentState.weakestFactor?.let { add("Weakest factor: $it") }
            }
            currentState.sevenDayAvgHours?.let { add("7-day average sleep: ${"%.1f".format(it)} hrs") }
            if (currentState.trackedNightsLast14 > 0) {
                add("Tracked nights in window: ${currentState.trackedNightsLast14}/14 — treat thin history cautiously")
            }
        }

        val enrichedText = if (contextInfo.isNotEmpty()) {
            "[Sleep Context: ${contextInfo.joinToString(", ")}]\n$text"
        } else {
            text
        }

        val userAlreadyExists = currentState.messages.any { it.role == MessageRole.USER && it.content == text }
        val updatedMessages = if (userAlreadyExists) currentState.messages else currentState.messages + userMessage

        _uiState.value = currentState.copy(
            conversationId = conversationId,
            messages = updatedMessages,
            inputText = "",
            isSending = true,
            expression = CompanionExpression.THINKING,
            lastUserMessageText = text
        )

        viewModelScope.launch {
            try {
                if (!userAlreadyExists) aiCoachRepository.saveUserMessage(userMessage)
                aiCoachRepository.ask(conversationId, enrichedText).collect { assistantMessage ->
                    val current = _uiState.value
                    val withoutPreviousStreamOfSameId = current.messages.filterNot { it.id == assistantMessage.id }
                    val newExpression = if (assistantMessage.isStreaming) CompanionExpression.RESPONDING else CompanionExpression.IDLE

                    _uiState.value = current.copy(
                        conversationId = current.conversationId ?: assistantMessage.conversationId,
                        messages = withoutPreviousStreamOfSameId + assistantMessage,
                        isSending = assistantMessage.isStreaming,
                        expression = newExpression
                    )
                }
            } catch (_: Exception) {
                val current = _uiState.value
                _uiState.value = current.copy(
                    messages = current.messages + AiMessage(
                        id = UUID.randomUUID().toString(),
                        role = MessageRole.ASSISTANT,
                        content = "I couldn't save this message. Please try again.",
                        createdAt = LocalDateTime.now(),
                        conversationId = conversationId
                    ),
                    isSending = false,
                    expression = CompanionExpression.IDLE
                )
            }
        }
    }
}
