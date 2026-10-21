package com.nocta.app.ui.screens.optimize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.ui.theme.NoctaAccent
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

/**
 * Optimize — tonight's recommended schedule (plan built backwards from the
 * wake time using real sleep history) and the user's editable wind-down
 * routine. Every line of the explanation comes from PlanBedtime's reasoning.
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

        when {
            state.isLoading -> item {
                Text("Building tonight's plan…", color = NoctaTextTertiary)
            }

            state.plan != null -> {
                item {
                    BedtimePlanCard(state, onShiftWake = viewModel::shiftWakeTime)
                }
                item { WindDownCard(state, viewModel::setStepEnabled) }
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
    onShiftWake: (Int) -> Unit
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
            PlanStat(
                "Wind-down starts",
                plan.windDownStart.format(TIME_FMT),
                modifier = Modifier.weight(1f)
            )
            PlanStat(
                "Lights out",
                plan.recommendedBedtime.format(TIME_FMT),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(NoctaSpacing.md))
        Row(modifier = Modifier.fillMaxWidth()) {
            PlanStat(
                "Wake up",
                plan.wakeTime.format(TIME_FMT),
                modifier = Modifier.weight(1f)
            )
            PlanStat(
                "Sleep opportunity",
                formatMinutes(plan.sleepOpportunityMinutes),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(NoctaSpacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
        ) {
            OutlinedButton(
                onClick = { onShiftWake(-30) },
                modifier = Modifier.weight(1f)
            ) { Text("Wake 30m earlier") }
            OutlinedButton(
                onClick = { onShiftWake(30) },
                modifier = Modifier.weight(1f)
            ) { Text("Wake 30m later") }
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
            Text(
                "• $line",
                style = NoctaTypography.bodyMedium,
                color = NoctaTextSecondary
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun WindDownCard(
    state: OptimizeUiState,
    onToggle: (String, Boolean) -> Unit
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
            "Wind-down routine",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Starts ${state.plan?.windDownStart?.format(TIME_FMT) ?: "—"} · " +
                "tick what you actually do — the routine is yours to edit.",
            style = NoctaTypography.labelSmall,
            color = NoctaTextTertiary
        )
        Spacer(Modifier.height(NoctaSpacing.sm))

        state.windDownSteps.forEach { step ->
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
                    color = if (step.isEnabled) NoctaTextPrimary else NoctaTextTertiary
                )
            }
        }
    }
}

@Composable
private fun PlanStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = NoctaTypography.labelSmall, color = NoctaTextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = NoctaTypography.headlineMedium,
            color = NoctaAccent
        )
    }
}

private fun formatMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0L) "${h}h" else "${h}h ${m}m"
}
