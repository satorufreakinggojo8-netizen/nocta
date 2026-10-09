package com.nocta.app.ui.screens.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.MessageRole
import com.nocta.app.domain.repository.AiCoachRepository
import com.nocta.app.domain.repository.SleepProfileRepository
import com.nocta.app.domain.repository.SleepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val lastNightScore: Int? = null,
    val lastUserMessageText: String? = null
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val aiCoachRepository: AiCoachRepository,
    private val sleepProfileRepository: SleepProfileRepository,
    private val sleepRepository: SleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    init {
        observeContextData()
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
                    val score = sleepRepository.computeScore(listOf(lastSession)).overall
                    _uiState.value = _uiState.value.copy(
                        lastNightDurationHours = durationHours,
                        lastNightScore = score
                    )
                }
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

        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = text,
            createdAt = LocalDateTime.now()
        )

        // Enrich text with available sleep context if present
        val currentState = _uiState.value
        val contextInfo = buildList {
            currentState.userAge?.let { add("User Age: $it") }
            currentState.targetSleepHours?.let { add("Target Sleep Goal: ${"%.1f".format(it)} hrs") }
            currentState.lastNightDurationHours?.let { add("Last Night Sleep: ${"%.1f".format(it)} hrs") }
            currentState.lastNightScore?.let { add("Last Night Score: $it/100") }
        }

        val enrichedText = if (contextInfo.isNotEmpty()) {
            "[Sleep Context: ${contextInfo.joinToString(", ")}]\n$text"
        } else {
            text
        }

        val userAlreadyExists = currentState.messages.any { it.role == MessageRole.USER && it.content == text }
        val updatedMessages = if (userAlreadyExists) currentState.messages else currentState.messages + userMessage

        _uiState.value = currentState.copy(
            messages = updatedMessages,
            inputText = "",
            isSending = true,
            expression = CompanionExpression.THINKING,
            lastUserMessageText = text
        )

        viewModelScope.launch {
            aiCoachRepository.ask(_uiState.value.conversationId, enrichedText).collect { assistantMessage ->
                val current = _uiState.value
                val withoutPreviousStreamOfSameId = current.messages.filterNot { it.id == assistantMessage.id }
                val newExpression = if (assistantMessage.isStreaming) CompanionExpression.RESPONDING else CompanionExpression.IDLE

                _uiState.value = current.copy(
                    conversationId = current.conversationId ?: assistantMessage.id,
                    messages = withoutPreviousStreamOfSameId + assistantMessage,
                    isSending = assistantMessage.isStreaming,
                    expression = newExpression
                )
            }
        }
    }
}
