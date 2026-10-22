package com.nocta.app.ui.screens.optimize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.model.PlannerLimits
import com.nocta.app.domain.model.SchedulePreset
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.model.WindDownOutcome
import com.nocta.app.domain.model.WindDownRun
import com.nocta.app.domain.profile.SleepProfile
import com.nocta.app.domain.repository.SettingsRepository
import com.nocta.app.domain.repository.SleepProfileRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.repository.WindDownRepository
import com.nocta.app.domain.repository.WindDownStep
import com.nocta.app.domain.usecase.BedtimePlan
import com.nocta.app.domain.usecase.PlanBedtime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** A schedule change waiting for the user's confirmation. */
data class PlannerPreview(
    val preset: SchedulePreset,
    val wakeTime: LocalTime,
    val plan: BedtimePlan
)

data class OptimizeUiState(
    val isLoading: Boolean = true,
    val plan: BedtimePlan? = null,
    val wakeTime: LocalTime = LocalTime.of(7, 0),
    val windDownMinutes: Int = PlannerLimits.DEFAULT_WIND_DOWN_MINUTES,
    val activePreset: SchedulePreset = SchedulePreset.CUSTOM,
    val windDownSteps: List<WindDownStep> = emptyList(),
    /** Non-null while a preset change is being previewed, before saving. */
    val preview: PlannerPreview? = null,
    val activeRun: WindDownRun? = null,
    val runRemainingSeconds: Long = 0,
    val runProgress: Float = 0f,
    val completedRuns: List<WindDownRun> = emptyList(),
    val message: String? = null
) {
    val enabledSteps: List<WindDownStep> get() = windDownSteps.filter { it.isEnabled }
}

/**
 * The Optimize area: an adaptive bedtime plan grounded in the user's real
 * sleep history, the schedule presets, and the wind-down routine itself
 * (start → pause → resume → finish, with real completed history).
 */
@HiltViewModel
class OptimizeViewModel @Inject constructor(
    private val sleepRepository: SleepRepository,
    sleepProfileRepository: SleepProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val windDownRepository: WindDownRepository,
    private val planBedtime: PlanBedtime
) : ViewModel() {

    private val _uiState = MutableStateFlow(OptimizeUiState())
    val uiState: StateFlow<OptimizeUiState> = _uiState.asStateFlow()

    /** Injectable clock keeps the timer testable without real waiting. */
    internal var clock: () -> Long = System::currentTimeMillis

    private var recentSessions: List<SleepSession> = emptyList()
    private var profile: SleepProfile = SleepProfile(age = DEFAULT_PROFILE_AGE)
    private var loaded = false

    init {
        viewModelScope.launch {
            sleepRepository.observeRecentSessions(days = HISTORY_DAYS).collect { recent ->
                recentSessions = recent
                refreshPlan()
            }
        }
        viewModelScope.launch {
            sleepProfileRepository.observeProfile().collect { stored ->
                profile = stored ?: SleepProfile(age = DEFAULT_PROFILE_AGE)
                refreshPlan()
            }
        }
        viewModelScope.launch {
            settingsRepository.observeWakeTime().collect { time ->
                _uiState.update { it.copy(wakeTime = time) }
                refreshPlan()
            }
        }
        viewModelScope.launch {
            settingsRepository.observeWindDownMinutes().collect { minutes ->
                _uiState.update { it.copy(windDownMinutes = minutes) }
                refreshPlan()
            }
        }
        viewModelScope.launch {
            settingsRepository.observeActivePreset().collect { preset ->
                _uiState.update { it.copy(activePreset = preset) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeWindDownSteps().collect { steps ->
                _uiState.update { it.copy(windDownSteps = steps) }
                loaded = true
                refreshPlan()
            }
        }
        viewModelScope.launch {
            windDownRepository.observeActiveRun().collect { run ->
                _uiState.update { it.copy(activeRun = run) }
                tick()
            }
        }
        viewModelScope.launch {
            windDownRepository.observeRecentRuns(limit = HISTORY_LIMIT).collect { runs ->
                _uiState.update { it.copy(completedRuns = runs) }
            }
        }
        // One ticker for the whole screen; it only computes while a run exists.
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                if (_uiState.value.activeRun != null) tick()
            }
        }
    }

    private fun refreshPlan() {
        val state = _uiState.value
        val plan = planBedtime(
            today = LocalDate.now(),
            wakeTime = state.wakeTime,
            recentSessions = recentSessions,
            profile = profile,
            windDownMinutes = state.windDownMinutes
        )
        val preview = state.preview?.let { pending ->
            pending.copy(
                plan = planBedtime(
                    today = LocalDate.now(),
                    wakeTime = pending.wakeTime,
                    recentSessions = recentSessions,
                    profile = profile,
                    windDownMinutes = state.windDownMinutes
                )
            )
        }
        _uiState.update {
            it.copy(
                isLoading = it.isLoading && !loaded,
                plan = plan,
                preview = preview
            )
        }
    }

    private fun tick() {
        val run = _uiState.value.activeRun ?: return
        val now = clock()
        _uiState.update {
            it.copy(
                runRemainingSeconds = run.remainingSecondsAt(now),
                runProgress = run.progressAt(now)
            )
        }
    }

    fun setStepEnabled(stepId: String, enabled: Boolean) {
        val updated = _uiState.value.windDownSteps.map {
            if (it.id == stepId) it.copy(isEnabled = enabled) else it
        }
        if (updated == _uiState.value.windDownSteps) return
        _uiState.update { it.copy(windDownSteps = updated) }
        viewModelScope.launch { settingsRepository.setWindDownSteps(updated) }
    }

    fun shiftWakeTime(minutes: Int) {
        val next = wrapTime(_uiState.value.wakeTime, minutes)
        viewModelScope.launch {
            settingsRepository.setWakeTime(next)
            settingsRepository.setPresetWakeTime(_uiState.value.activePreset, next)
        }
    }

    fun shiftWindDown(minutes: Int) {
        val next = _uiState.value.windDownMinutes + minutes
        viewModelScope.launch { settingsRepository.setWindDownMinutes(next) }
    }

    /** Shows what a preset would change before anything is saved. */
    fun previewPreset(preset: SchedulePreset) {
        viewModelScope.launch {
            val savedWake = settingsRepository.getPresetWakeTime(preset)
            val wake = savedWake ?: when (preset) {
                SchedulePreset.CUSTOM -> _uiState.value.wakeTime
                else -> preset.defaultWakeTime
            }
            val plan = planBedtime(
                today = LocalDate.now(),
                wakeTime = wake,
                recentSessions = recentSessions,
                profile = profile,
                windDownMinutes = _uiState.value.windDownMinutes
            )
            _uiState.update { it.copy(preview = PlannerPreview(preset, wake, plan)) }
        }
    }

    fun applyPreview() {
        val pending = _uiState.value.preview ?: return
        viewModelScope.launch {
            settingsRepository.setPresetWakeTime(pending.preset, pending.wakeTime)
            settingsRepository.setActivePreset(pending.preset)
            settingsRepository.setWakeTime(pending.wakeTime)
            _uiState.update {
                it.copy(
                    preview = null,
                    message = "${pending.preset.label}: wake ${formatClock(pending.wakeTime)}, " +
                        "lights out ${formatClock(pending.plan.recommendedBedtime)}."
                )
            }
        }
    }

    fun cancelPreview() {
        _uiState.update { it.copy(preview = null) }
    }

    fun startRoutine() {
        if (_uiState.value.activeRun != null) return
        viewModelScope.launch {
            runCatching {
                windDownRepository.startRun(_uiState.value.windDownMinutes, clock())
            }.onFailure {
                _uiState.update { state -> state.copy(message = "Couldn't start the routine. Try again.") }
            }
        }
    }

    fun pauseRoutine() = mutateRun { it.pause(clock()) }

    fun resumeRoutine() = mutateRun { it.resume(clock()) }

    fun toggleRoutineStep(stepId: String) = mutateRun { it.toggleStep(stepId) }

    fun finishRoutine() = mutateRun { run ->
        run.finish(clock(), WindDownOutcome.COMPLETED).also {
            _uiState.update { state ->
                state.copy(
                    message = if (it.completedStepIds.isEmpty()) {
                        "Routine completed."
                    } else {
                        "Routine completed — ${it.completedStepIds.size} steps ticked."
                    }
                )
            }
        }
    }

    fun dismissRoutine() = mutateRun { it.finish(clock(), WindDownOutcome.DISMISSED) }

    private fun mutateRun(transform: (WindDownRun) -> WindDownRun) {
        val run = _uiState.value.activeRun ?: return
        val updated = transform(run)
        if (updated == run) return
        _uiState.update { it.copy(activeRun = updated) }
        viewModelScope.launch {
            windDownRepository.updateRun(updated)
            tick()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun wrapTime(current: LocalTime, minutes: Int): LocalTime =
        LocalTime.ofSecondOfDay(
            (((current.toSecondOfDay() + minutes * 60L) % 86_400 + 86_400) % 86_400)
        )

    private fun formatClock(time: LocalTime): String =
        time.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))

    private companion object {
        const val HISTORY_DAYS = 30
        const val HISTORY_LIMIT = 10
        const val DEFAULT_PROFILE_AGE = 18
    }
}
