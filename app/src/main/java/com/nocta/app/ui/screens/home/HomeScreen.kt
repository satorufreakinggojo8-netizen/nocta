package com.nocta.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.domain.model.SleepDebtStatus
import com.nocta.app.domain.model.SleepSession
import com.nocta.app.ui.components.RecommendationCard
import com.nocta.app.ui.components.SleepScoreCard
import com.nocta.app.ui.components.SleepScoreEmptyCard
import com.nocta.app.ui.components.companion.NoctaCompanionOverlay
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

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
    val greeting = greetingFor(LocalTime.now())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NoctaBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = NoctaSpacing.md,
                end = NoctaSpacing.md,
                top = NoctaSpacing.md,
                // Vela's stage: keeps the last card clear of the character so
                // nothing important sits underneath it at rest.
                bottom = VelaStageHeight
            ),
            verticalArrangement = Arrangement.spacedBy(NoctaSpacing.lg)
        ) {
            item {
                Column(modifier = Modifier.padding(top = NoctaSpacing.md)) {
                    Text(
                        greeting,
                        style = NoctaTypography.displayLarge,
                        color = NoctaTextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        LocalDate.now().format(DATE_FMT),
                        style = NoctaTypography.labelSmall,
                        color = NoctaTextTertiary
                    )
                }
            }

            item {
                val breakdown = uiState.scoreBreakdown
                when {
                    uiState.isLoading -> ScoreCardSkeleton()
                    breakdown != null && breakdown.hasData -> SleepScoreCard(
                        breakdown = breakdown,
                        mode = uiState.scoreCardMode,
                        onModeChange = viewModel::setScoreCardMode,
                        onTap = {
                            uiState.lastNight?.id?.let(onOpenSleepDetails)
                        }
                    )
                    else -> SleepScoreEmptyCard(onLogSleep = onLogSleep)
                }
            }

            uiState.lastNight?.let { session ->
                item {
                    Column {
                        SectionHeader("Last night")
                        LastNightCard(session = session, onOpen = { onOpenSleepDetails(session.id) })
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
                    ) {
                        QuickActionButton(
                            "Log Sleep",
                            Modifier.weight(1f),
                            onLogSleep
                        )

                        QuickActionButton(
                            "Wind Down",
                            Modifier.weight(1f),
                            onOpenWindDown
                        )
                    }

                    Spacer(Modifier.height(NoctaSpacing.sm))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
                    ) {
                        QuickActionButton(
                            "Ask Vela",
                            Modifier.weight(1f),
                            onOpenCoach
                        )

                        QuickActionButton(
                            "View Insights",
                            Modifier.weight(1f),
                            onOpenInsights
                        )
                    }
                }
            }
        }

        // Faint violet aura behind Vela — light, not a frame around the artwork.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(width = 260.dp, height = 320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NoctaAccent.copy(alpha = 0.13f),
                            NoctaAccent.copy(alpha = 0.04f),
                            NoctaBackground.copy(alpha = 0f)
                        )
                    )
                )
        )

        NoctaCompanionOverlay(
            onClick = onOpenCoach,
            characterHeight = VelaStageHeight
        )
    }
}

private val VelaStageHeight = 300.dp

@Composable
private fun LastNightCard(session: SleepSession, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .padding(horizontal = NoctaSpacing.md, vertical = NoctaSpacing.sm)
    ) {
        NightStatRow("Bedtime", session.bedtime.format(TIME_FMT))
        NightStatDivider()
        NightStatRow("Wake time", session.wakeTime.format(TIME_FMT))
        NightStatDivider()
        NightStatRow(
            "Duration",
            formatDuration(session.durationMinutes)
        )

        Spacer(Modifier.height(NoctaSpacing.xs))
        Text(
            text = buildString {
                append(session.source.name.lowercase().replace('_', ' '))
                append(" · tap for the full night report")
            },
            style = NoctaTypography.labelSmall,
            color = NoctaTextTertiary
        )
    }
}

@Composable
private fun NightStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = NoctaTypography.bodyMedium,
            color = NoctaTextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NightStatDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NoctaSurfaceBorder)
    )
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
private fun QuickActionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
            containerColor = NoctaSurface,
            contentColor = NoctaTextPrimary
        ),
        shape = NoctaShapes.medium
    ) {
        Text(
            label,
            style = NoctaTypography.bodyMedium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ScoreCardSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(NoctaShapes.large)
            .background(NoctaSurface)
    )
}

private fun greetingFor(time: LocalTime): String = when (time.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Late night"
}

internal fun formatDuration(minutes: Long): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0L) "${hours}h" else "${hours}h ${remainder}m"
}
