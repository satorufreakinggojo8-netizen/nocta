package com.nocta.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.domain.model.SleepDebtStatus
import com.nocta.app.ui.components.MetricCard
import com.nocta.app.ui.components.RecommendationCard
import com.nocta.app.ui.components.SleepScoreCard
import com.nocta.app.ui.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onOpenSleepDetails: (String) -> Unit,
    onOpenCoach: () -> Unit,
    onOpenWindDown: () -> Unit,
    onOpenInsights: () -> Unit,
    onLogSleep: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NoctaBackground),
        contentPadding = PaddingValues(NoctaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NoctaSpacing.lg)
    ) {
        item {
            Text(
                "Good evening",
                style = NoctaTypography.displayLarge,
                color = NoctaTextPrimary,
                modifier = Modifier.padding(top = NoctaSpacing.md)
            )
        }

        item {
            if (uiState.isLoading) {
                ScoreCardSkeleton()
            } else {
                uiState.scoreBreakdown?.let { breakdown ->
                    SleepScoreCard(
                        breakdown = breakdown,
                        onTap = { uiState.lastNight?.id?.let(onOpenSleepDetails) }
                    )
                }
            }
        }

        uiState.lastNight?.let { session ->
            item {
                Column {
                    SectionHeader("Last night")
                    Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                        MetricCard(
                            "Bedtime", session.bedtime.format(timeFmt),
                            caption = session.source.name.lowercase().replace('_', ' '),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            "Wake time", session.wakeTime.format(timeFmt),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            "Duration", "${session.durationMinutes / 60}h ${session.durationMinutes % 60}m",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        uiState.sleepDebt?.let { debt ->
            item {
                Column {
                    SectionHeader("Sleep debt")
                    Text(
                        text = when (debt.status) {
                            SleepDebtStatus.WELL_RESTED -> "You're well rested"
                            SleepDebtStatus.SLIGHTLY_BEHIND -> "You're slightly behind on sleep"
                            SleepDebtStatus.CATCHING_UP -> "You're catching up on sleep"
                        },
                        style = NoctaTypography.bodyLarge,
                        color = NoctaTextPrimary
                    )
                    Text(
                        "Estimated, based on your recent nights vs. an 8-hour target.",
                        style = NoctaTypography.labelSmall,
                        color = NoctaTextTertiary
                    )
                }
            }
        }

        uiState.todaysRecommendation?.let { rec ->
            item {
                Column {
                    SectionHeader("Today's recommendation")
                    RecommendationCard(rec)
                }
            }
        }

        item {
            Column {
                SectionHeader("Quick actions")
                Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                    QuickActionButton("Log Sleep", Modifier.weight(1f), onLogSleep)
                    QuickActionButton("Start Wind Down", Modifier.weight(1f), onOpenWindDown)
                }
                Spacer(Modifier.height(NoctaSpacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                    QuickActionButton("Ask AI Coach", Modifier.weight(1f), onOpenCoach)
                    QuickActionButton("View Insights", Modifier.weight(1f), onOpenInsights)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = NoctaTypography.titleMedium,
        color = NoctaTextPrimary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = NoctaSpacing.sm)
    )
}

@Composable
private fun QuickActionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = NoctaSurface,
            contentColor = NoctaTextPrimary
        ),
        shape = NoctaShapes.medium
    ) {
        Text(label, style = NoctaTypography.bodyMedium)
    }
}

@Composable
private fun ScoreCardSkeleton() {
    // Simple placeholder box in the score card's shape/color while data loads —
    // intentionally undecorated rather than a fake animated shimmer, kept minimal.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(NoctaShapes.large)
            .background(NoctaSurface)
    )
}
