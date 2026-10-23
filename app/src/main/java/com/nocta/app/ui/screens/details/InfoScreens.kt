package com.nocta.app.ui.screens.details

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.domain.model.SleepDebtStatus
import com.nocta.app.ui.screens.home.HomeViewModel
import com.nocta.app.ui.guide.ShadeLayout
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

/**
 * Shared header + scroll body for pages pushed on top of a tab. Keeping the
 * back affordance in one place means every details page behaves the same.
 */
@Composable
private fun DetailPage(
    title: String,
    onBack: () -> Unit,
    // Only pages Shade has a tip for need the stage's room at the bottom.
    bottomInset: Dp = 0.dp,
    content: LazyListScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoctaBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NoctaSpacing.sm, vertical = NoctaSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NoctaTextSecondary
                )
            }
            Text(
                text = title,
                style = NoctaTypography.titleMedium,
                color = NoctaTextPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = NoctaSpacing.md,
                end = NoctaSpacing.md,
                top = NoctaSpacing.xs,
                bottom = bottomInset
            ),
            verticalArrangement = Arrangement.spacedBy(NoctaSpacing.lg),
            content = content
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoctaShapes.medium)
            .background(NoctaSurface)
            .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
            .padding(NoctaSpacing.md)
    ) {
        content()
    }
}

@Composable
private fun Paragraph(text: String, muted: Boolean = false) {
    Text(
        text = text,
        style = NoctaTypography.bodyMedium,
        color = if (muted) NoctaTextTertiary else NoctaTextSecondary
    )
}

/**
 * The one-line focus from Home, expanded. Shows why this suggestion was picked,
 * what to actually try, and what it is not.
 */
@Composable
fun RecommendationDetailScreen(
    onBack: () -> Unit,
    onAskVela: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    DetailPage(
        title = "Today's focus",
        onBack = onBack,
        bottomInset = ShadeLayout.CompactBottomInset
    ) {
        item {
            val recommendation = state.todaysRecommendation
            if (recommendation == null) {
                EmptyNote(
                    title = "No suggestion yet",
                    body = "A suggestion appears once at least one night has been recorded and " +
                        "scored. Log a night and it will be based on your own history — never on " +
                        "a generic template."
                )
            } else {
                Column {
                    Text(
                        text = recommendation.title,
                        style = NoctaTypography.headlineMedium,
                        color = NoctaTextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${recommendation.category.label()} · ${recommendation.difficulty.label()}",
                        style = NoctaTypography.labelSmall,
                        color = NoctaAccent
                    )
                    Spacer(Modifier.height(NoctaSpacing.md))
                    Paragraph(recommendation.why)
                }
            }
        }

        state.todaysRecommendation?.let { recommendation ->
            item {
                Card {
                    Text(
                        text = "Try this",
                        style = NoctaTypography.labelSmall,
                        color = NoctaTextTertiary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = recommendation.whatToDo,
                        style = NoctaTypography.bodyLarge,
                        color = NoctaTextPrimary
                    )
                    Spacer(Modifier.height(NoctaSpacing.sm))
                    Text(
                        text = recommendation.expectedBenefit,
                        style = NoctaTypography.labelSmall,
                        color = NoctaTextTertiary
                    )
                }
            }

            item {
                Column {
                    Button(
                        onClick = onAskVela,
                        modifier = Modifier.fillMaxWidth(),
                        shape = NoctaShapes.medium
                    ) {
                        Text("Ask Vela about this")
                    }
                    Spacer(Modifier.height(NoctaSpacing.sm))
                    Paragraph(
                        text = "Suggestions come from your recorded nights and are general " +
                            "wellness guidance, not a treatment plan.",
                        muted = true
                    )
                }
            }
        }
    }
}

/**
 * The methodology behind the estimate, with the user's own figures — the long
 * explanation lives here instead of on the Insights screen.
 */
@Composable
fun SleepDebtScreen(
    onBack: () -> Unit,
    viewModel: SleepDebtViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    DetailPage(
        title = "Sleep debt",
        onBack = onBack,
        bottomInset = ShadeLayout.CompactBottomInset
    ) {
        when {
            state.isLoading -> item { Paragraph("Loading…", muted = true) }

            state.debt == null -> item {
                EmptyNote(
                    title = "No estimate yet",
                    body = "Sleep debt needs at least one recorded night. Missing nights are " +
                        "never counted as zero sleep, so the app will not invent a number."
                )
            }

            else -> {
                val debt = state.debt!!
                item {
                    Column {
                        Text(
                            text = when (debt.status) {
                                SleepDebtStatus.WELL_RESTED -> "You're well rested"
                                SleepDebtStatus.SLIGHTLY_BEHIND -> "You're slightly behind on sleep"
                                SleepDebtStatus.CATCHING_UP -> "You're catching up on sleep"
                            },
                            style = NoctaTypography.headlineMedium,
                            color = NoctaTextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = debtSummary(debt.approximateDeficitMinutes),
                            style = NoctaTypography.bodyLarge,
                            color = NoctaTextSecondary
                        )
                        Spacer(Modifier.height(NoctaSpacing.sm))
                        Text(
                            text = "Estimated from ${state.nightsConsidered} recorded " +
                                "night${if (state.nightsConsidered == 1) "" else "s"}" +
                                (state.averageMinutes?.let { " · average ${formatDuration(it)}" }
                                    ?: ""),
                            style = NoctaTypography.labelSmall,
                            color = NoctaTextTertiary
                        )
                    }
                }

                item {
                    Card {
                        Text(
                            text = "How this is estimated",
                            style = NoctaTypography.labelSmall,
                            color = NoctaTextTertiary
                        )
                        Spacer(Modifier.height(NoctaSpacing.sm))
                        Bullet("Each recorded night in the last 14 days is compared with an 8-hour target.")
                        Bullet("Nights under target add to the estimate; nights over it do not subtract a made-up credit from the others.")
                        Bullet("The shortfall is multiplied by the number of recorded nights — missing nights are not counted at all.")
                        Bullet("Bedtime regularity is not part of this figure; it has its own score on Insights.")
                    }
                }

                item {
                    Paragraph(
                        text = "This is an estimate for self-awareness, not a measurement and not " +
                            "medical advice. If you feel persistently exhausted, talk to a clinician.",
                        muted = true
                    )
                }
            }
        }
    }
}

/** Static explanation of the five score factors, reachable from the Sleep tab. */
@Composable
fun ScoreHelpScreen(onBack: () -> Unit) {
    DetailPage(title = "Your sleep score", onBack = onBack) {
        item {
            Paragraph(
                "The score is a 0–100 wellness summary built from recorded nights only. " +
                    "Nothing is shown until at least one night has passed validation."
            )
        }

        item {
            Card {
                ScoreFactor("Duration", 40, "Time asleep against the age-appropriate range.")
                ScoreFactor("Bedtime consistency", 25, "How tightly your bedtimes cluster, measured circularly so 11:50 p.m. and 12:10 a.m. are close.")
                ScoreFactor("Sleep quality", 15, "Your morning check-in's quality and restedness answers.")
                ScoreFactor("Schedule timing", 10, "How close lights-out is to your plan.")
                ScoreFactor("Night awakenings", 10, "How often you woke, from your check-in.")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "The five factors add up to 100.",
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextTertiary
                )
            }
        }

        item {
            Paragraph(
                "A score appears only when there is a real night behind it. No history means " +
                    "an explicit empty card, never a zero that reads like a bad night.",
                muted = true
            )
        }

        item {
            Paragraph(
                "This is a wellness score, not a clinical sleep assessment.",
                muted = true
            )
        }
    }
}

/** The reasoning list from Optimize, on its own page. */
@Composable
fun PlanWhyScreen(
    onBack: () -> Unit,
    viewModel: PlanWhyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val plan = state.plan

    DetailPage(title = "Why this plan", onBack = onBack) {
        when {
            state.isLoading || plan == null -> item { Paragraph("Loading…", muted = true) }

            else -> {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                        PlanValue("Lights out", plan.recommendedBedtime.format(TIME_FMT), Modifier.weight(1f))
                        PlanValue("Wake up", plan.wakeTime.format(TIME_FMT), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(NoctaSpacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
                        PlanValue("Wind-down", plan.windDownStart.format(TIME_FMT), Modifier.weight(1f))
                        PlanValue(
                            "Sleep window",
                            formatDuration(plan.sleepOpportunityMinutes),
                            Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Column {
                        plan.explanation.forEach { line ->
                            Text(
                                text = "• $line",
                                style = NoctaTypography.bodyMedium,
                                color = NoctaTextSecondary
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                item {
                    Paragraph(
                        text = "Plans are guidance built from your own records and age-based " +
                            "ranges — not medical advice. Adjust anything that doesn't fit your life.",
                        muted = true
                    )
                }
            }
        }
    }
}

/** Full data-flow explanation; Profile keeps only the summary and the controls. */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    DetailPage(title = "Privacy & your data", onBack = onBack) {
        item {
            Card {
                Text(
                    text = "Where your data lives",
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextTertiary
                )
                Spacer(Modifier.height(NoctaSpacing.sm))
                Bullet("Sleep records, check-ins, journal entries, routine runs, and coach conversations are stored on this device in a Room database.")
                Bullet("Display and schedule settings are stored on this device in SharedPreferences.")
                Bullet("Sleep sessions are sent to your own backend server only to sync your account, when a backend is configured. The app contains no API keys — server credentials stay server-side.")
                Bullet("No audio, microphone, or sensor data is collected. Nothing is shared with third parties.")
                Bullet("Muting or re-enabling Vela's tips is stored on this device like any other display setting.")
            }
        }

        item {
            Paragraph(
                text = "Export gives you a JSON copy of nights, check-ins, journal entries, " +
                    "routine history and your schedule settings. Deleting is permanent and " +
                    "applies to this device — a copy your backend already received must be " +
                    "removed there separately, because this build has no server deletion endpoint.",
                muted = true
            )
        }
    }
}

@Composable
private fun EmptyNote(title: String, body: String) {
    Card {
        Text(
            text = title,
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(NoctaSpacing.sm))
        Paragraph(body)
    }
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            "•",
            style = NoctaTypography.bodyMedium,
            color = NoctaTextTertiary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = NoctaTypography.bodyMedium,
            color = NoctaTextSecondary
        )
    }
}

@Composable
private fun ScoreFactor(name: String, weight: Int, description: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = NoctaTypography.bodyMedium,
                color = NoctaTextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$weight%",
                style = NoctaTypography.labelSmall,
                color = NoctaAccent
            )
        }
        Text(
            text = description,
            style = NoctaTypography.labelSmall,
            color = NoctaTextTertiary
        )
    }
}

@Composable
private fun PlanValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = NoctaTypography.labelSmall, color = NoctaTextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = NoctaTypography.headlineMedium,
            color = NoctaAccent,
            maxLines = 1
        )
    }
}

private fun debtSummary(deficitMinutes: Int): String = when {
    deficitMinutes < 60 -> "Less than an hour behind across those nights."
    else -> "Roughly ${formatDuration(deficitMinutes.toLong())} behind across those nights."
}

private fun formatDuration(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0L) "${h}h" else "${h}h ${m}m"
}

private fun com.nocta.app.domain.model.RecommendationCategory.label(): String =
    name.lowercase(Locale.getDefault()).replace('_', ' ').replaceFirstChar { it.uppercase(Locale.getDefault()) }

private fun com.nocta.app.domain.model.Difficulty.label(): String =
    name.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase(Locale.getDefault()) }
