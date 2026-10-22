package com.nocta.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.SleepScoreCardMode
import com.nocta.app.domain.profile.SleepProfile
import com.nocta.app.domain.repository.AiCoachRepository
import com.nocta.app.domain.repository.SettingsRepository
import com.nocta.app.domain.repository.SleepProfileRepository
import com.nocta.app.domain.usecase.DeleteAllSleepData
import com.nocta.app.domain.usecase.ExportSleepData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val age: Int? = null,
    val sleepTargetLabel: String? = null,
    val wakeTime: LocalTime = LocalTime.of(7, 0),
    val cardMode: SleepScoreCardMode = SleepScoreCardMode.STANDARD,
    val savedMessage: String? = null,
    val exportText: String? = null,
    val exporting: Boolean = false,
    val confirmDeleteSleep: Boolean = false,
    val confirmDeleteCoach: Boolean = false,
    val deleteDoneMessage: String? = null
)

/**
 * Profile & settings: age-appropriate sleep target, wake time, display
 * preference, and the privacy controls (export / delete) from Section 13.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sleepProfileRepository: SleepProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val aiCoachRepository: AiCoachRepository,
    private val exportSleepData: ExportSleepData,
    private val deleteAllSleepData: DeleteAllSleepData
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sleepProfileRepository.observeProfile().collect { profile ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        age = profile?.age,
                        sleepTargetLabel = profile?.recommendedSleepLabel
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeWakeTime().collect { time ->
                _uiState.update { it.copy(wakeTime = time) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeSleepScoreCardMode().collect { mode ->
                _uiState.update { it.copy(cardMode = mode) }
            }
        }
    }

    fun saveAge(age: Int) {
        if (age !in MIN_AGE..MAX_AGE) return
        viewModelScope.launch {
            sleepProfileRepository.saveAge(age)
            val label = SleepProfile(age = age).recommendedSleepLabel
            _uiState.update {
                it.copy(
                    age = age,
                    sleepTargetLabel = label,
                    savedMessage = "Sleep target updated to $label"
                )
            }
        }
    }

    fun setWakeTime(time: LocalTime) {
        viewModelScope.launch {
            settingsRepository.setWakeTime(time)
            _uiState.update {
                it.copy(wakeTime = time, savedMessage = "Wake time updated")
            }
        }
    }

    fun setCardMode(mode: SleepScoreCardMode) {
        viewModelScope.launch { settingsRepository.setSleepScoreCardMode(mode) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(savedMessage = null, deleteDoneMessage = null) }
    }

    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(exporting = true) }
            runCatching { exportSleepData() }
                .onSuccess { json ->
                    _uiState.update { it.copy(exporting = false, exportText = json) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            exporting = false,
                            savedMessage = "Export failed: ${error.message ?: "unknown error"}"
                        )
                    }
                }
        }
    }

    fun clearExport() {
        _uiState.update { it.copy(exportText = null) }
    }

    fun requestDeleteSleepData() {
        _uiState.update { it.copy(confirmDeleteSleep = true) }
    }

    fun cancelDeleteSleepData() {
        _uiState.update { it.copy(confirmDeleteSleep = false) }
    }

    fun confirmDeleteSleepData() {
        viewModelScope.launch {
            runCatching { deleteAllSleepData() }.onSuccess {
                _uiState.update {
                    it.copy(
                        confirmDeleteSleep = false,
                        deleteDoneMessage = "All sleep records, tracking sessions, check-ins, " +
                            "journal entries, and routine history were deleted from this device."
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        confirmDeleteSleep = false,
                        savedMessage = "Delete failed: ${error.message ?: "unknown error"}"
                    )
                }
            }
        }
    }

    fun requestDeleteCoachData() {
        _uiState.update { it.copy(confirmDeleteCoach = true) }
    }

    fun cancelDeleteCoachData() {
        _uiState.update { it.copy(confirmDeleteCoach = false) }
    }

    fun confirmDeleteCoachData() {
        viewModelScope.launch {
            runCatching { aiCoachRepository.deleteAllMessages() }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            confirmDeleteCoach = false,
                            deleteDoneMessage = "Coach conversation history was deleted from this device."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            confirmDeleteCoach = false,
                            savedMessage = "Delete failed: ${error.message ?: "unknown error"}"
                        )
                    }
                }
        }
    }

    private companion object {
        const val MIN_AGE = 6
        const val MAX_AGE = 100
    }
}
