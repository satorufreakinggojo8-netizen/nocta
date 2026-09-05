package com.nocta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nocta.app.domain.model.Difficulty
import com.nocta.app.domain.model.Recommendation
import com.nocta.app.ui.theme.*

/** A compact stat tile — e.g. bedtime, wake time, duration on "Last Night". */
@Composable
fun MetricCard(
    label: String,
    value: String,
    caption: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .padding(NoctaSpacing.md)
    ) {
        Text(label, style = NoctaTypography.labelSmall, color = NoctaTextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = NoctaTypography.headlineMedium, color = NoctaTextPrimary)
        caption?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, style = NoctaTypography.labelSmall, color = NoctaTextTertiary)
        }
    }
}

/** Today's Recommendation card and Sleep Optimization list items share this shape. */
@Composable
fun RecommendationCard(
    recommendation: Recommendation,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .padding(NoctaSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                recommendation.title,
                style = NoctaTypography.titleMedium,
                color = NoctaTextPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            DifficultyBadge(recommendation.difficulty)
        }
        Spacer(Modifier.height(NoctaSpacing.sm))
        Text(recommendation.why, style = NoctaTypography.bodyMedium, color = NoctaTextSecondary)
        Spacer(Modifier.height(NoctaSpacing.sm))
        Text(
            "Try this: ${recommendation.whatToDo}",
            style = NoctaTypography.bodyMedium,
            color = NoctaTextPrimary
        )
    }
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val label = when (difficulty) {
        Difficulty.EASY -> "Easy"
        Difficulty.MODERATE -> "Moderate"
        Difficulty.CHALLENGING -> "Challenging"
    }
    Text(
        text = label,
        style = NoctaTypography.labelSmall,
        color = NoctaAccent,
        modifier = Modifier
            .clip(NoctaShapes.small)
            .background(NoctaAccentMuted)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
