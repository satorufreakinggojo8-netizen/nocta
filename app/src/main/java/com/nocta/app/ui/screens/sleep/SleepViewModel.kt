package com.nocta.app.ui.screens.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.SleepTrackingError
import com.nocta.app.domain.model.SleepTrackingSession
import com.nocta.app.domain.repository.SleepTrackingRepository
import com.nocta.app.domain.usecase.SleepTrackingRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class SleepUiState(
    val isLoading: Boolean = true,
    val active: SleepTrackingSession? = null,
    val latestCompleted: SleepTrackingSession? = null,
    val isStale: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val repository: SleepTrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepUiState())
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    /**
     * Clock tick used both for the elapsed-time display and for recomputing
     * "is this session stale?". Updated by a single ticker coroutine that is
     * launched ONCE in init below and never re-launched — this avoids the
     * multiple-concurrent-ticker bug from the earlier draft.
     */
    private val now = MutableStateFlow(Instant.now())

    init {
        // One ticker coroutine for the lifetime of the ViewModel. Cheap (1 Hz),
        // and importantly there is exactly one regardless of state changes.
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                now.value = Instant.now()
            }
        }

        // Combine persisted state with the clock. This flow emits whenever
        // either the DB changes or the ticker fires, so `isStale` recomputes
        // automatically as time passes.
        viewModelScope.launch {
            combine(
                repository.observeActive(),
                repository.observeLatestCompleted(),
                now
            ) { active, completed, tick ->
                SleepUiState(
                    isLoading = false,
                    active = active,
                    latestCompleted = completed,
                    isStale = active != null &&
                        SleepTrackingRules.isStale(active.startedAt, tick),
                    // Preserve any transient message currently on screen.
                    message = _uiState.value.message
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun startTracking() {
        viewModelScope.launch {
            try {
                repository.start(Instant.now())
            } catch (t: Throwable) {
                emitMessage(t)
            }
        }
    }

    fun endTracking() {
        viewModelScope.launch {
            try {
                repository.end(Instant.now())
            } catch (t: Throwable) {
                emitMessage(t)
            }
        }
    }

    fun discardActive() {
        viewModelScope.launch {
            try {
                repository.discardActive()
            } catch (t: Throwable) {
                emitMessage(t)
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun emitMessage(t: Throwable) {
        val text = when (t) {
            is SleepTrackingError -> t.message
            else -> "Something went wrong. Please try again."
        }
        _uiState.value = _uiState.value.copy(message = text)
    }
}
