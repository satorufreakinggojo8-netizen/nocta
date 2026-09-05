package com.nocta.app.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ripple.rememberRipple
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
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.ui.theme.*

/**
 * The Home screen's hero element. Animates from 0 to the true score once on
 * first composition (per the "score counting animation" requirement) rather
 * than re-animating on every recomposition.
 */
@Composable
fun SleepScoreCard(
    breakdown: SleepScoreBreakdown,
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
                indication = rememberRipple(),
                onClick = onTap
            )
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
            ScoreFactorPip("Duration", breakdown.durationScore)
            ScoreFactorPip("Consistency", breakdown.consistencyScore)
            ScoreFactorPip("Timing", breakdown.timingScore)
            ScoreFactorPip("Recovery", breakdown.qualityScore)
            ScoreFactorPip("Routine", breakdown.routineScore)
        }
    }
}

@Composable
private fun ScoreFactorPip(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(scoreColor(value))
        )
        Spacer(Modifier.height(4.dp))
        Text(text = label, style = NoctaTypography.labelSmall, color = NoctaTextTertiary)
    }
}

private fun scoreColor(score: Int) = when (score) {
    in 85..100 -> NoctaScoreExcellent
    in 70..84 -> NoctaScoreGood
    in 50..69 -> NoctaScoreFair
    else -> NoctaScorePoor
}
