package com.nocta.app.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepScoreCardMode
import com.nocta.app.ui.theme.*

/**
 * The Home screen's hero element. Animates from 0 to the true score once on
 * first composition (per the "score counting animation" requirement) rather
 * than re-animating on every recomposition.
 *
 * Three display modes are supported — Compact (score and verdict only),
 * Standard (adds the five factor pips) and Expanded (adds the per-factor
 * values with bars and a short explanation). The mode is chosen with the
 * selector at the bottom of the card and is persisted through
 * SettingsRepository, so it survives an app restart.
 */
@Composable
fun SleepScoreCard(
    breakdown: SleepScoreBreakdown,
    mode: SleepScoreCardMode,
    onModeChange: (SleepScoreCardMode) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasAnimated by remember { mutableStateOf(false) }
    val animatedScore by animateIntAsState(
        targetValue = if (hasAnimated) breakdown.overall else 0,
        animationSpec = tween(durationMillis = 900),
        label = "sleepScoreCount"
    )
    remember { hasAnimated = true } // trigger the animation exactly once

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NoctaShapes.large)
            .background(
                Brush.verticalGradient(listOf(NoctaSurfaceElevated, NoctaSurface))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
            .padding(if (mode == SleepScoreCardMode.COMPACT) NoctaSpacing.md else NoctaSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (mode) {
            SleepScoreCardMode.COMPACT -> CompactScoreContent(breakdown, animatedScore)
            SleepScoreCardMode.STANDARD -> StandardScoreContent(breakdown, animatedScore)
            SleepScoreCardMode.EXPANDED -> ExpandedScoreContent(breakdown, animatedScore)
        }

        Spacer(Modifier.height(NoctaSpacing.md))

        ScoreCardModeSelector(current = mode, onModeChange = onModeChange)
    }
}

@Composable
private fun CompactScoreContent(breakdown: SleepScoreBreakdown, animatedScore: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$animatedScore",
            style = NoctaScoreDisplay.copy(fontSize = 44.sp),
            color = scoreColor(breakdown.overall)
        )
        Spacer(Modifier.width(NoctaSpacing.md))
        Column {
            Text(
                text = "Sleep Score",
                style = NoctaTypography.labelSmall,
                color = NoctaTextSecondary
            )
            Text(
                text = breakdown.label,
                style = NoctaTypography.titleMedium,
                color = NoctaTextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StandardScoreContent(breakdown: SleepScoreBreakdown, animatedScore: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sleep Score",
            style = NoctaTypography.labelSmall,
            color = NoctaTextSecondary
        )
        Spacer(Modifier.height(NoctaSpacing.sm))
        Text(
            text = "$animatedScore",
            style = NoctaScoreDisplay,
            color = scoreColor(breakdown.overall)
        )
        Text(
            text = breakdown.label,
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(NoctaSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScoreFactorPip("Duration", breakdown.durationScore, Modifier.weight(1f))
            ScoreFactorPip("Consistency", breakdown.consistencyScore, Modifier.weight(1f))
            ScoreFactorPip("Timing", breakdown.timingScore, Modifier.weight(1f))
            ScoreFactorPip("Recovery", breakdown.qualityScore, Modifier.weight(1f))
            ScoreFactorPip("Routine", breakdown.routineScore, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExpandedScoreContent(breakdown: SleepScoreBreakdown, animatedScore: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StandardScoreContent(breakdown, animatedScore)

        Spacer(Modifier.height(NoctaSpacing.lg))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
        ) {
            ScoreFactorRow("Duration", breakdown.durationScore)
            ScoreFactorRow("Consistency", breakdown.consistencyScore)
            ScoreFactorRow("Timing", breakdown.timingScore)
            ScoreFactorRow("Recovery", breakdown.qualityScore)
            ScoreFactorRow("Routine", breakdown.routineScore)
        }

        Spacer(Modifier.height(NoctaSpacing.md))

        Text(
            text = "A wellness score from your last 14 nights and your morning " +
                "check-in. It isn't a medical assessment.",
            style = NoctaTypography.bodySmall,
            color = NoctaTextTertiary
        )
    }
}

/** A single factor with its 0–100 value and a thin progress bar. */
@Composable
private fun ScoreFactorRow(label: String, value: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = NoctaTypography.labelSmall,
                color = NoctaTextSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$value",
                style = NoctaTypography.labelSmall,
                color = NoctaTextPrimary
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(NoctaSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (value.coerceIn(0, 100) / 100f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(scoreColor(value))
            )
        }
    }
}

@Composable
private fun ScoreCardModeSelector(
    current: SleepScoreCardMode,
    onModeChange: (SleepScoreCardMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SleepScoreCardMode.entries.forEach { candidate ->
            val selected = candidate == current
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) NoctaAccentMuted else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onModeChange(candidate) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = candidate.displayLabel,
                    style = NoctaTypography.labelSmall,
                    color = if (selected) NoctaAccent else NoctaTextTertiary
                )
            }
        }
    }
}

/**
 * Shown instead of a numeric score when no night has passed validation. A
 * "0 — needs attention" verdict would be misleading: there is simply nothing
 * to score yet.
 */
@Composable
fun SleepScoreEmptyCard(
    onLogSleep: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NoctaShapes.large)
            .background(Brush.verticalGradient(listOf(NoctaSurfaceElevated, NoctaSurface)))
            .padding(NoctaSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sleep Score",
            style = NoctaTypography.labelSmall,
            color = NoctaTextSecondary
        )
        Spacer(Modifier.height(NoctaSpacing.sm))
        Text(
            text = "—",
            style = NoctaScoreDisplay,
            color = NoctaTextTertiary
        )
        Text(
            text = "No score yet",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(NoctaSpacing.sm))
        Text(
            text = "Log or track a night of sleep and Nocta will score your duration, " +
                "consistency, timing and recovery.",
            style = NoctaTypography.bodyMedium,
            color = NoctaTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(NoctaSpacing.lg))
        androidx.compose.material3.Button(
            onClick = onLogSleep,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = NoctaAccent,
                contentColor = androidx.compose.ui.graphics.Color(0xFF14121F)
            ),
            shape = NoctaShapes.medium
        ) {
            Text("Log a night", style = NoctaTypography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ScoreFactorPip(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(scoreColor(value))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = NoctaTypography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
            color = NoctaTextTertiary,
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun scoreColor(score: Int) = when (score) {
    in 85..100 -> NoctaScoreExcellent
    in 70..84 -> NoctaScoreGood
    in 50..69 -> NoctaScoreFair
    else -> NoctaScorePoor
}
