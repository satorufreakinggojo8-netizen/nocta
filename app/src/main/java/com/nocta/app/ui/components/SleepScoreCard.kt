package com.nocta.app.ui.components

import android.content.Context
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.ui.theme.*

enum class SleepScoreCardScale { COMPACT, STANDARD, EXPANDED }

private const val PREFS_NAME = "nocta_prefs"
private const val KEY_CARD_SCALE = "sleep_score_card_scale"

/**
 * The Home screen's hero element. Animates from 0 to the true score once on
 * first composition. Features interactive size scaling (Compact, Standard,
 * Expanded) with local preference persistence.
 */
@Composable
fun SleepScoreCard(
    breakdown: SleepScoreBreakdown,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var selectedScale by remember {
        val savedName = prefs.getString(KEY_CARD_SCALE, SleepScoreCardScale.STANDARD.name)
        mutableStateOf(
            try { SleepScoreCardScale.valueOf(savedName ?: SleepScoreCardScale.STANDARD.name) }
            catch (e: Exception) { SleepScoreCardScale.STANDARD }
        )
    }

    fun updateScale(newScale: SleepScoreCardScale) {
        selectedScale = newScale
        prefs.edit().putString(KEY_CARD_SCALE, newScale.name).apply()
    }

    var hasAnimated by remember { mutableStateOf(false) }
    val animatedScore by animateIntAsState(
        targetValue = if (hasAnimated) breakdown.overall else 0,
        animationSpec = tween(durationMillis = 900),
        label = "sleepScoreCount"
    )
    remember { hasAnimated = true }

    val (scoreFontSize, cardPadding, spacerHeight) = when (selectedScale) {
        SleepScoreCardScale.COMPACT -> Triple(38.sp, NoctaSpacing.sm, NoctaSpacing.xs)
        SleepScoreCardScale.STANDARD -> Triple(56.sp, NoctaSpacing.lg, NoctaSpacing.md)
        SleepScoreCardScale.EXPANDED -> Triple(76.sp, 28.dp, NoctaSpacing.lg)
    }

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
            .padding(cardPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Size Selector Toggle Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = NoctaSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sleep Score",
                style = NoctaTypography.labelSmall,
                color = NoctaTextSecondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SleepScoreCardScale.entries.forEach { scale ->
                    val isSelected = scale == selectedScale
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NoctaAccent else NoctaSurface)
                            .border(
                                1.dp,
                                if (isSelected) NoctaAccent else Color(0xFF2A3555),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { updateScale(scale) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = scale.name.take(1),
                            style = NoctaTypography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isSelected) Color.White else NoctaTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(spacerHeight))

        Text(
            text = "$animatedScore",
            style = NoctaScoreDisplay.copy(fontSize = scoreFontSize),
            color = scoreColor(breakdown.overall)
        )

        Text(
            text = breakdown.label,
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )

        if (selectedScale != SleepScoreCardScale.COMPACT) {
            Spacer(Modifier.height(spacerHeight))
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
