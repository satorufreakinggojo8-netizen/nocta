package com.nocta.app.ui.screens.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.MessageRole
import com.nocta.app.domain.repository.AiCoachRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class CoachUiState(
    val conversationId: String? = null,
    val messages: List<AiMessage> = emptyList(),
    val isSending: Boolean = false,
    val inputText: String = ""
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val aiCoachRepository: AiCoachRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
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
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            inputText = "",
            isSending = true
        )

        viewModelScope.launch {
            aiCoachRepository.ask(_uiState.value.conversationId, text).collect { assistantMessage ->
                val current = _uiState.value
                val withoutPreviousStreamOfSameId = current.messages.filterNot { it.id == assistantMessage.id }
                _uiState.value = current.copy(
                    conversationId = current.conversationId ?: assistantMessage.id,
                    messages = withoutPreviousStreamOfSameId + assistantMessage,
                    isSending = assistantMessage.isStreaming
                )
            }
        }
    }
}
