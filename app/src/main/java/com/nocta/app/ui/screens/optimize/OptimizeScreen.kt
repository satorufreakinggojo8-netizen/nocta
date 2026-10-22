package com.nocta.app.ui.screens.optimize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.domain.model.PlannerLimits
import com.nocta.app.domain.model.SchedulePreset
import com.nocta.app.domain.model.WindDownRun
import com.nocta.app.domain.repository.WindDownStep
import com.nocta.app.ui.theme.NoctaAccent
import com.nocta.app.ui.theme.NoctaAccentMuted
import com.nocta.app.ui.theme.NoctaBackground
import com.nocta.app.ui.theme.NoctaShapes
import com.nocta.app.ui.theme.NoctaSpacing
import com.nocta.app.ui.theme.NoctaSurface
import com.nocta.app.ui.theme.NoctaSurfaceBorder
import com.nocta.app.ui.theme.NoctaTextPrimary
import com.nocta.app.ui.theme.NoctaTextSecondary
import com.nocta.app.ui.theme.NoctaTextTertiary
import com.nocta.app.ui.theme.NoctaTypography
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

/**
 * Optimize — tonight's recommended schedule (built backwards from the wake time
 * using real sleep history), the named schedule presets with a preview before
 * anything is saved, and the wind-down routine as a real timed run with history.
 */
@Composable
fun OptimizeScreen(
    viewModel: OptimizeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NoctaBackground),
        contentPadding = PaddingValues(NoctaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NoctaSpacing.lg)
    ) {
        item {
            Text(
                "Optimize",
                style = NoctaTypography.displayLarge,
                color = NoctaTextPrimary,
                modifier = Modifier.padding(top = NoctaSpacing.md)
            )
        }

        if (state.isLoading) {
            item { Text("Building tonight's plan…", color = NoctaTextTertiary) }
        } else {
            state.plan?.let { plan ->
                item {
                    BedtimePlanCard(
                        state = state,
                        onShiftWake = viewModel::shiftWakeTime,
                        onShiftWindDown = viewModel::shiftWindDown
                    )
                }
            }

            item {
                SchedulePresetCard(
                    state = state,
                    onPreview = viewModel::previewPreset,
                    onApply = viewModel::applyPreview,
                    onCancel = viewModel::cancelPreview
                )
            }

            item {
                val run = state.activeRun
                if (run != null) {
                    RoutineRunCard(
                        run = run,
                        remainingSeconds = state.runRemainingSeconds,
                        progress = state.runProgress,
                        steps = state.enabledSteps,
                        onToggleStep = viewModel::toggleRoutineStep,
                        onPause = viewModel::pauseRoutine,
                        onResume = viewModel::resumeRoutine,
                        onFinish = viewModel::finishRoutine,
                        onDismiss = viewModel::dismissRoutine
                    )
                } else {
                    RoutineSetupCard(
                        steps = state.windDownSteps,
                        windDownMinutes = state.windDownMinutes,
                        windDownStart = state.plan?.windDownStart,
                        onToggle = viewModel::setStepEnabled,
                        onStart = viewModel::startRoutine
                    )
                }
            }

            item {
                RoutineHistoryCard(runs = state.completedRuns)
            }
        }

        state.message?.let { message ->
            item {
                Text(message, style = NoctaTypography.bodyMedium, color = NoctaAccent)
            }
        }

        item {
            Text(
                "Plans are guidance from your own records and age-based ranges — " +
                    "not medical advice. Adjust anything that doesn't fit your life.",
                style = NoctaTypography.labelSmall,
                color = NoctaTextTertiary
            )
        }
    }
}

@Composable
private fun BedtimePlanCard(
    state: OptimizeUiState,
    onShiftWake: (Int) -> Unit,
    onShiftWindDown: (Int) -> Unit
) {
    val plan = state.plan ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.large)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.large)
            .padding(NoctaSpacing.lg)
    ) {
        Text(
            "Tonight's plan",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(NoctaSpacing.lg))

        Row(modifier = Modifier.fillMaxWidth()) {
            PlanStat("Wind-down starts", plan.windDownStart.format(TIME_FMT), Modifier.weight(1f))
            PlanStat("Lights out", plan.recommendedBedtime.format(TIME_FMT), Modifier.weight(1f))
        }
        Spacer(Modifier.height(NoctaSpacing.md))
        Row(modifier = Modifier.fillMaxWidth()) {
            PlanStat("Wake up", plan.wakeTime.format(TIME_FMT), Modifier.weight(1f))
            PlanStat("Sleep opportunity", formatMinutes(plan.sleepOpportunityMinutes), Modifier.weight(1f))
        }

        Spacer(Modifier.height(NoctaSpacing.md))

        Text("Shift wake time", style = NoctaTypography.labelSmall, color = NoctaTextSecondary)
        Spacer(Modifier.height(NoctaSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
        ) {
            CompactButton("−30 min", Modifier.weight(1f)) { onShiftWake(-30) }
            CompactButton("+30 min", Modifier.weight(1f)) { onShiftWake(30) }
        }

        Spacer(Modifier.height(NoctaSpacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Wind-down length", style = NoctaTypography.labelSmall, color = NoctaTextSecondary)
                Text(
                    "${state.windDownMinutes} min",
                    style = NoctaTypography.titleMedium,
                    color = NoctaTextPrimary
                )
            }
            CompactButton("−5", Modifier) { onShiftWindDown(-PlannerLimits.STEP_WIND_DOWN_MINUTES) }
            Spacer(Modifier.padding(horizontal = 3.dp))
            CompactButton("+5", Modifier) { onShiftWindDown(PlannerLimits.STEP_WIND_DOWN_MINUTES) }
        }

        Spacer(Modifier.height(NoctaSpacing.lg))

        Text(
            "Why this plan",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(NoctaSpacing.sm))
        plan.explanation.forEach { line ->
            Text("• $line", style = NoctaTypography.bodyMedium, color = NoctaTextSecondary)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SchedulePresetCard(
    state: OptimizeUiState,
    onPreview: (SchedulePreset) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .padding(NoctaSpacing.md)
    ) {
        Text(
            "Schedule",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Switching presets previews the change first — nothing is saved until you apply.",
            style = NoctaTypography.labelSmall,
            color = NoctaTextTertiary
        )
        Spacer(Modifier.height(NoctaSpacing.sm))

        Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.xs)) {
            SchedulePreset.entries.forEach { preset ->
                PresetChip(
                    label = preset.shortLabel,
                    selected = state.preview?.preset?.let { it == preset }
                        ?: (state.activePreset == preset && state.preview == null),
                    onClick = { onPreview(preset) }
                )
            }
        }

        state.preview?.let { preview ->
            Spacer(Modifier.height(NoctaSpacing.md))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NoctaShapes.small)
                    .background(NoctaAccentMuted)
                    .padding(NoctaSpacing.sm)
            ) {
                Text(
                    "Preview · ${preview.preset.label}",
                    style = NoctaTypography.labelSmall,
                    color = NoctaAccent
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Wake ${preview.wakeTime.format(TIME_FMT)} · " +
                        "wind-down ${preview.plan.windDownStart.format(TIME_FMT)} · " +
                        "lights out ${preview.plan.recommendedBedtime.format(TIME_FMT)}",
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextPrimary
                )
                preview.plan.explanation.lastOrNull()?.let { line ->
                    Spacer(Modifier.height(4.dp))
                    Text(line, style = NoctaTypography.labelSmall, color = NoctaTextSecondary)
                }
                Spacer(Modifier.height(NoctaSpacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                    Button(
                        onClick = onApply,
                        shape = NoctaShapes.small,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Apply", style = NoctaTypography.labelSmall, maxLines = 1)
                    }
                    TextButton(onClick = onCancel) {
                        Text(
                            "Cancel",
                            style = NoctaTypography.labelSmall,
                            color = NoctaTextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = NoctaTypography.labelSmall,
        color = if (selected) NoctaAccent else NoctaTextSecondary,
        maxLines = 1,
        modifier = Modifier
            .clip(NoctaShapes.small)
            .background(if (selected) NoctaAccentMuted else NoctaBackground)
            .border(
                1.dp,
                if (selected) NoctaAccent.copy(alpha = 0.5f) else NoctaSurfaceBorder,
                NoctaShapes.small
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun RoutineRunCard(
    run: WindDownRun,
    remainingSeconds: Long,
    progress: Float,
    steps: List<WindDownStep>,
    onToggleStep: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.large)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.large)
            .padding(NoctaSpacing.lg)
    ) {
        Text(
            "Winding down",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(NoctaSpacing.xs))
        Text(
            formatCountdown(remainingSeconds),
            style = NoctaTypography.displayLarge,
            color = NoctaAccent
        )
        Text(
            if (remainingSeconds == 0L) "Time's up — finish when you're ready."
            else "${run.plannedMinutes} min routine" + if (run.isPaused) " · paused" else " · running",
            style = NoctaTypography.labelSmall,
            color = NoctaTextTertiary
        )

        Spacer(Modifier.height(NoctaSpacing.sm))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(NoctaShapes.small),
            color = NoctaAccent,
            trackColor = NoctaSurfaceBorder
        )

        Spacer(Modifier.height(NoctaSpacing.md))
        steps.forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = step.id in run.completedStepIds,
                    onCheckedChange = { onToggleStep(step.id) }
                )
                Text(
                    step.label,
                    style = NoctaTypography.bodyMedium,
                    color = if (step.id in run.completedStepIds) NoctaTextSecondary else NoctaTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(NoctaSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
        ) {
            OutlinedButton(
                onClick = if (run.isPaused) onResume else onPause,
                modifier = Modifier.weight(1f),
                shape = NoctaShapes.small,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text(if (run.isPaused) "Resume" else "Pause", maxLines = 1)
            }
            Button(
                onClick = onFinish,
                modifier = Modifier.weight(1f),
                shape = NoctaShapes.small,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text("Finish", maxLines = 1)
            }
        }
        Spacer(Modifier.height(NoctaSpacing.xs))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Dismiss routine",
                style = NoctaTypography.labelSmall,
                color = NoctaTextTertiary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RoutineSetupCard(
    steps: List<WindDownStep>,
    windDownMinutes: Int,
    windDownStart: java.time.LocalTime?,
    onToggle: (String, Boolean) -> Unit,
    onStart: () -> Unit
) {
    val enabled = steps.count { it.isEnabled }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .padding(NoctaSpacing.md)
    ) {
        Text(
            "Wind-down routine",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Starts ${windDownStart?.format(TIME_FMT) ?: "—"} · $windDownMinutes min · " +
                "choose the steps that fit your evening.",
            style = NoctaTypography.labelSmall,
            color = NoctaTextTertiary
        )
        Spacer(Modifier.height(NoctaSpacing.sm))

        steps.forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = step.isEnabled,
                    onCheckedChange = { onToggle(step.id, it) }
                )
                Text(
                    step.label,
                    style = NoctaTypography.bodyMedium,
                    color = if (step.isEnabled) NoctaTextPrimary else NoctaTextTertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(NoctaSpacing.sm))
        Button(
            onClick = onStart,
            enabled = enabled > 0,
            modifier = Modifier.fillMaxWidth(),
            shape = NoctaShapes.small
        ) {
            Text(
                if (enabled == 0) "Enable at least one step" else "Start wind-down",
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RoutineHistoryCard(runs: List<WindDownRun>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .padding(NoctaSpacing.md)
    ) {
        Text(
            "Routine history",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(NoctaSpacing.xs))
        if (runs.isEmpty()) {
            Text(
                "No completed routines yet. Start one tonight and it will appear here — " +
                    "only real runs are recorded.",
                style = NoctaTypography.bodyMedium,
                color = NoctaTextTertiary
            )
        } else {
            runs.take(5).forEach { run ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        run.date.format(DATE_FMT),
                        style = NoctaTypography.bodyMedium,
                        color = NoctaTextPrimary
                    )
                    Text(
                        if (run.outcome.name == "COMPLETED") {
                            "${run.elapsedMinutesAt(run.endedAtMillis ?: run.startedAtMillis)} min · " +
                                "${run.completedStepIds.size} steps"
                        } else {
                            "Dismissed"
                        },
                        style = NoctaTypography.labelSmall,
                        color = NoctaTextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = NoctaTypography.labelSmall, color = NoctaTextSecondary, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = NoctaTypography.headlineMedium,
            color = NoctaAccent,
            maxLines = 1
        )
    }
}

@Composable
private fun CompactButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = NoctaShapes.small,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, style = NoctaTypography.labelSmall, maxLines = 1)
    }
}

private fun formatMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0L) "${h}h" else "${h}h ${m}m"
}

private fun formatCountdown(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return "%d:%02d".format(minutes, seconds)
}
