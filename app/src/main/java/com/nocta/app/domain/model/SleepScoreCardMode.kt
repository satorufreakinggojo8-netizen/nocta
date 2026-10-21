package com.nocta.app.domain.model

/**
 * Display modes for the Home screen's Sleep Score card. The user's choice is
 * persisted (see SettingsRepository) so it survives app restarts.
 */
enum class SleepScoreCardMode(val displayLabel: String) {
    COMPACT("Compact"),
    STANDARD("Standard"),
    EXPANDED("Expanded")
}
