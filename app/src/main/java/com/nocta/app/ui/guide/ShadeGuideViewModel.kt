package com.nocta.app.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocta.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShadeGuideUiState(
    /** Mirrors the saved preference; false hides the bubble but not the character. */
    val enabled: Boolean = true,
    val tip: ShadeTip? = null,
    /** The longer in-app guidance, used when the coach backend is unavailable. */
    val detailExpanded: Boolean = false
)

/**
 * Owns the companion's current tip for the visible screen and the mute
 * preference. Tips advance only when the screen actually changes, so a
 * recomposition (or an orientation change) never rewrites the bubble.
 */
@HiltViewModel
class ShadeGuideViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val rotator = ShadeTipRotator()

    private val _uiState = MutableStateFlow(ShadeGuideUiState())
    val uiState: StateFlow<ShadeGuideUiState> = _uiState.asStateFlow()

    private var currentBaseRoute: String? = null

    init {
        viewModelScope.launch {
            settingsRepository.observeShadeGuideEnabled().collect { enabled ->
                _uiState.update { it.copy(enabled = enabled) }
            }
        }
    }

    /**
     * Called once per destination change. Repeated calls for the same screen
     * are ignored, which is what keeps the tip stable while scrolling.
     */
    fun onRouteChanged(baseRoute: String?) {
        if (baseRoute == currentBaseRoute) return
        currentBaseRoute = baseRoute
        _uiState.update {
            it.copy(tip = rotator.next(baseRoute), detailExpanded = false)
        }
    }

    /** Expands or collapses the in-app guidance (used when AI is unavailable). */
    fun toggleDetail() {
        _uiState.update { it.copy(detailExpanded = !it.detailExpanded) }
    }

    /** Persisted immediately, so the choice survives a restart. */
    fun mute() {
        viewModelScope.launch { settingsRepository.setShadeGuideEnabled(false) }
    }

    fun unmute() {
        viewModelScope.launch { settingsRepository.setShadeGuideEnabled(true) }
    }
}
