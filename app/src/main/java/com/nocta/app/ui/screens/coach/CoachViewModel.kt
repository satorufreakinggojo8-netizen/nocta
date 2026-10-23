package com.nocta.app.ui.screens.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.data.remote.BackendConfig
import com.nocta.app.data.remote.CoachFailures
import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.domain.model.MessageRole
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.model.WindDownRun
import com.nocta.app.domain.profile.SleepProfile
import com.nocta.app.domain.repository.AiCoachRepository
import com.nocta.app.domain.repository.SleepJournalRepository
import com.nocta.app.domain.repository.SleepProfileRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.repository.WindDownRepository
import com.nocta.app.domain.usecase.BuildCoachContext
import com.nocta.app.domain.usecase.CalculateSleepScore
import com.nocta.app.domain.usecase.CoachContext
import com.nocta.app.domain.usecase.CoachContextInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
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
    val trackedNightsLast14: Int = 0,
    val journalDays: Int = 0,
    /** False when the build has no coach backend; the UI must say so. */
    val backendConfigured: Boolean = BackendConfig.isConfigured,
    /** One-line summary of what Vela can actually see, for the empty state. */
    val dataSummary: String? = null,
    /** Real gaps in the data, shown so the user knows what to log next. */
    val missingData: List<String> = emptyList()
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val aiCoachRepository: AiCoachRepository,
    private val sleepProfileRepository: SleepProfileRepository,
    private val sleepRepository: SleepRepository,
    private val journalRepository: SleepJournalRepository,
    private val windDownRepository: WindDownRepository,
    private val buildCoachContext: BuildCoachContext,
    private val calculateSleepScore: CalculateSleepScore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    private var sessions: List<SleepSession> = emptyList()
    private var profile: SleepProfile? = null
    private var journalEntries: List<JournalEntry> = emptyList()
    private var completedRuns: List<WindDownRun> = emptyList()
    private var cachedScore: SleepScoreBreakdown? = null

    init {
        observeContextData()
        observeLatestSavedConversation()
    }

    private fun observeLatestSavedConversation() {
        viewModelScope.launch {
            aiCoachRepository.observeMostRecentConversationId().collectLatest { conversationId ->
                if (conversationId != null) {
                    aiCoachRepository.observeConversation(conversationId).collect { messages ->
                        _uiState.update {
                            it.copy(conversationId = conversationId, messages = messages)
                        }
                    }
                }
            }
        }
    }

    private fun observeContextData() {
        viewModelScope.launch {
            sleepProfileRepository.observeProfile().collect { stored ->
                profile = stored
                if (stored != null) {
                    val recommendedMid = (stored.recommendedMinMinutes + stored.recommendedMaxMinutes) / 2.0
                    _uiState.update {
                        it.copy(userAge = stored.age, targetSleepHours = recommendedMid / 60.0)
                    }
                }
                refreshSummary()
            }
        }
        viewModelScope.launch {
            sleepRepository.observeRecentSessions(days = CONTEXT_DAYS).collect { recent ->
                sessions = recent
                val last = recent.maxByOrNull { it.date }
                val score = if (recent.isEmpty()) null else calculateSleepScore(recent)
                cachedScore = score
                val weakest = score?.let { breakdown ->
                    listOf(
                        "duration" to breakdown.durationScore,
                        "schedule consistency" to breakdown.consistencyScore,
                        "sleep quality" to breakdown.qualityScore,
                        "schedule timing" to breakdown.timingScore,
                        "night awakenings" to breakdown.routineScore
                    ).minByOrNull { it.second }?.first
                }
                val last7 = recent.take(7)
                _uiState.update {
                    it.copy(
                        lastNightDurationHours = last?.durationMinutes?.let { minutes ->
                            minutes / 60.0
                        },
                        scoreOverall = score?.takeIf { breakdown -> breakdown.hasData }?.overall,
                        scoreLabel = score?.takeIf { breakdown -> breakdown.hasData }?.label,
                        weakestFactor = weakest,
                        sevenDayAvgHours = last7
                            .takeIf { it.isNotEmpty() }
                            ?.map { night -> night.durationMinutes }
                            ?.average()
                            ?.div(60.0),
                        trackedNightsLast14 = recent.count { night ->
                            !night.date.isBefore(java.time.LocalDate.now().minusDays(14))
                        }
                    )
                }
                refreshSummary()
            }
        }
        viewModelScope.launch {
            journalRepository.observeRecent(days = CONTEXT_DAYS).collect { entries ->
                journalEntries = entries
                _uiState.update { it.copy(journalDays = entries.count { entry -> entry.hasAnyAnswer }) }
                refreshSummary()
            }
        }
        viewModelScope.launch {
            windDownRepository.observeRecentRuns(limit = 14).collect { runs ->
                completedRuns = runs.filter { run ->
                    run.outcome == com.nocta.app.domain.model.WindDownOutcome.COMPLETED
                }
                refreshSummary()
            }
        }
    }

    private fun refreshSummary() {
        val summary = buildList {
            if (sessions.isNotEmpty()) {
                add("${sessions.size} logged night${if (sessions.size == 1) "" else "s"}")
            }
            if (journalEntries.isNotEmpty()) {
                add("${journalEntries.count { it.hasAnyAnswer }} journal days")
            }
            if (completedRuns.isNotEmpty()) add("${completedRuns.size} completed routines")
        }
        val gaps = buildContext("").missing
        _uiState.update {
            it.copy(
                dataSummary = if (summary.isEmpty()) null else summary.joinToString(" · "),
                missingData = gaps
            )
        }
    }

    private fun buildContext(prompt: String): CoachContext = buildCoachContext(
        CoachContextInput(
            prompt = prompt,
            sessions = sessions,
            profile = profile,
            journalEntries = journalEntries,
            completedWindDownRuns = completedRuns,
            score = cachedScore
        )
    )

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun retryLastMessage() {
        val lastText = _uiState.value.lastUserMessageText
        if (lastText.isNullOrBlank() || _uiState.value.isSending) return
        // Drop the failed bubble(s) so retry doesn't stack error messages.
        val filtered = _uiState.value.messages.filterNot { it.isError }
        _uiState.update { it.copy(messages = filtered) }
        send(promptOverride = lastText)
    }

    /**
     * Sends a question that arrived from outside the chat (a tip bubble). An
     * identical question already in the conversation is not asked twice, so a
     * re-entered screen cannot duplicate the exchange.
     */
    fun sendIfAbsent(prompt: String) {
        if (prompt.isBlank() || _uiState.value.isSending) return
        val alreadyAsked = _uiState.value.messages.any { message ->
            message.role == MessageRole.USER && message.content == prompt
        }
        if (alreadyAsked) return
        send(promptOverride = prompt)
    }

    fun send(promptOverride: String? = null) {
        val text = promptOverride ?: _uiState.value.inputText
        if (text.isBlank() || _uiState.value.isSending) return

        val current = _uiState.value
        val conversationId = current.conversationId ?: UUID.randomUUID().toString()
        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = text,
            createdAt = LocalDateTime.now(),
            conversationId = conversationId
        )

        val enriched = buildContext(text).envelope(text)

        val alreadyPresent = current.messages.any { it.role == MessageRole.USER && it.content == text }
        val updatedMessages = if (alreadyPresent) current.messages else current.messages + userMessage

        _uiState.update {
            it.copy(
                conversationId = conversationId,
                messages = updatedMessages,
                inputText = "",
                isSending = true,
                expression = CompanionExpression.THINKING,
                lastUserMessageText = text
            )
        }

        viewModelScope.launch {
            if (!alreadyPresent) {
                runCatching { aiCoachRepository.saveUserMessage(userMessage) }
            }

            if (!current.backendConfigured) {
                // Truthful unavailable state: no fake answer, no wasted request.
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMessage(CoachFailures.NOT_CONFIGURED),
                        isSending = false,
                        expression = CompanionExpression.IDLE
                    )
                }
                return@launch
            }

            try {
                aiCoachRepository.ask(conversationId, enriched).collect { assistantMessage ->
                    val state = _uiState.value
                    val withoutSameId = state.messages.filterNot { it.id == assistantMessage.id }
                    _uiState.update {
                        it.copy(
                            conversationId = state.conversationId ?: assistantMessage.conversationId,
                            messages = withoutSameId + assistantMessage,
                            isSending = assistantMessage.isStreaming,
                            expression = if (assistantMessage.isStreaming) {
                                CompanionExpression.RESPONDING
                            } else {
                                CompanionExpression.IDLE
                            }
                        )
                    }
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMessage(CoachFailures.describe(t)),
                        isSending = false,
                        expression = CompanionExpression.IDLE
                    )
                }
            }
        }
    }

    private fun errorMessage(content: String) = AiMessage(
        id = UUID.randomUUID().toString(),
        role = MessageRole.ASSISTANT,
        content = content,
        createdAt = LocalDateTime.now(),
        isError = true
    )

    private companion object {
        const val CONTEXT_DAYS = 30
    }
}
