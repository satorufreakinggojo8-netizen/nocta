package com.nocta.app.ui.screens.profile

import android.content.Intent
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.ui.components.MetricCard
import com.nocta.app.ui.theme.NoctaAlert
import com.nocta.app.ui.theme.NoctaBackground
import com.nocta.app.ui.theme.NoctaShapes
import com.nocta.app.ui.theme.NoctaSpacing
import com.nocta.app.ui.theme.NoctaSurface
import com.nocta.app.ui.theme.NoctaSurfaceBorder
import com.nocta.app.ui.theme.NoctaTextPrimary
import com.nocta.app.ui.theme.NoctaTextSecondary
import com.nocta.app.ui.theme.NoctaTextTertiary
import com.nocta.app.ui.theme.NoctaTypography

/**
 * Profile & Settings — sleep target, wake time, privacy and data controls.
 * Everything here writes through the real repositories (Room / prefs).
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Hand the generated JSON to the system share sheet when ready.
    LaunchedEffect(state.exportText) {
        val json = state.exportText ?: return@LaunchedEffect
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "Nocta sleep data export")
            putExtra(Intent.EXTRA_TEXT, json)
        }
        runCatching { context.startActivity(Intent.createChooser(send, "Export sleep data")) }
        viewModel.clearExport()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NoctaBackground),
        contentPadding = PaddingValues(NoctaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NoctaSpacing.lg)
    ) {
        item {
            Text(
                "Profile",
                style = NoctaTypography.displayLarge,
                color = NoctaTextPrimary,
                modifier = Modifier.padding(top = NoctaSpacing.md)
            )
        }

        if (state.isLoading) {
            item { Text("Loading…", color = NoctaTextTertiary) }
            return@LazyColumn
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NoctaShapes.medium)
                    .background(NoctaSurface)
                    .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
                    .padding(NoctaSpacing.md)
            ) {
                Text(
                    "Sleep target",
                    style = NoctaTypography.titleMedium,
                    color = NoctaTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(NoctaSpacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
                ) {
                    MetricCard(
                        "Your age",
                        state.age?.toString() ?: "—",
                        caption = if (state.age == null) "set it below" else null,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        "Target",
                        state.sleepTargetLabel ?: "—",
                        caption = "per age range",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(NoctaSpacing.md))

                Text(
                    if (state.age == null)
                        "Add your age so scores, plans, and recommendations match the recommended range for you."
                    else "Age ${state.age}",
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextSecondary
                )
                Spacer(Modifier.height(NoctaSpacing.sm))

                AgeEditor(
                    currentAge = state.age,
                    onSave = viewModel::saveAge
                )
            }
        }

        item {
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
                Spacer(Modifier.height(NoctaSpacing.sm))
                Text(
                    "Wake time — the bedtime plan is built backwards from this.",
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextSecondary
                )
                Spacer(Modifier.height(NoctaSpacing.sm))
                WakeTimeEditor(
                    wakeTime = state.wakeTime,
                    onSave = viewModel::setWakeTime
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NoctaShapes.medium)
                    .background(NoctaSurface)
                    .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
                    .padding(NoctaSpacing.md)
            ) {
                Text(
                    "Privacy & your data",
                    style = NoctaTypography.titleMedium,
                    color = NoctaTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(NoctaSpacing.sm))
                Text(
                    "Where your data lives:\n" +
                        "• Sleep records, check-ins, journal entries, routine runs, and coach conversations are stored on this device (Room database).\n" +
                        "• Display and schedule settings are stored on this device (SharedPreferences).\n" +
                        "• Sleep sessions are sent to your own backend server only to sync your account, when a backend is configured. The app itself contains no API keys — server credentials stay server-side.\n" +
                        "• No audio, microphone, or sensor data is collected. Nothing is shared with third parties.",
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextSecondary
                )

                Spacer(Modifier.height(NoctaSpacing.md))

                OutlinedButton(
                    onClick = viewModel::exportData,
                    enabled = !state.exporting,
                    modifier = Modifier.fillMaxWidth()
                ) {                Text(
                    if (state.exporting) "Preparing export…" else "Export my data (JSON)"
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Includes nights, check-ins, journal entries, routine history, and your schedule settings.",
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextTertiary
                )
                }

                Spacer(Modifier.height(NoctaSpacing.sm))

                OutlinedButton(
                    onClick = viewModel::requestDeleteSleepData,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete all sleep data", color = NoctaAlert)
                }

                Spacer(Modifier.height(NoctaSpacing.sm))

                OutlinedButton(
                    onClick = viewModel::requestDeleteCoachData,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete coach conversations", color = NoctaAlert)
                }

                Spacer(Modifier.height(NoctaSpacing.sm))
                Text(
                    "Deletion is permanent and applies to this device. This build has no " +
                        "deletion endpoint on the server, so any copy your backend already received " +
                        "must be removed there separately.",
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextTertiary
                )
            }
        }

        state.savedMessage?.let { message ->
            item {
                Text(message, style = NoctaTypography.bodyMedium, color = NoctaTextSecondary)
            }
        }
        state.deleteDoneMessage?.let { message ->
            item {
                Text(message, style = NoctaTypography.bodyMedium, color = NoctaTextSecondary)
            }
        }
    }

    if (state.confirmDeleteSleep) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteSleepData,
            title = { Text("Delete all sleep data?") },
            text = {
                Text(
                    "This permanently removes every sleep record, tracking session, " +
                        "morning check-in, journal entry, and routine run stored on this device. " +
                        "Coach conversations are deleted separately. This cannot be undone — " +
                        "export your data first if you want to keep a copy."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteSleepData) {
                    Text("Delete", color = NoctaAlert)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteSleepData) {
                    Text("Cancel")
                }
            }
        )
    }

    if (state.confirmDeleteCoach) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteCoachData,
            title = { Text("Delete coach conversations?") },
            text = {
                Text(
                    "This permanently removes your full chat history with Vela " +
                        "from this device. Sleep records are not affected."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteCoachData) {
                    Text("Delete", color = NoctaAlert)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteCoachData) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AgeEditor(currentAge: Int?, onSave: (Int) -> Unit) {
    // Seed the editor from the stored value; re-seed if the loaded age changes.
    var age by remember(currentAge) { mutableIntStateOf(currentAge ?: 25) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepperButton("−", onClick = { if (age > 6) age-- })
            Box(
                modifier = Modifier
                    .widthIn(min = 52.dp)
                    .clip(NoctaShapes.small)
                    .background(NoctaBackground)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$age",
                    style = NoctaTypography.titleMedium,
                    color = NoctaTextPrimary,
                    maxLines = 1
                )
            }
            StepperButton("+", onClick = { if (age < 100) age++ })
            Spacer(Modifier.weight(1f))
            Button(onClick = { onSave(age) }, shape = NoctaShapes.small) {
                Text("Save", maxLines = 1)
            }
        }
        Spacer(Modifier.height(NoctaSpacing.sm))
        Text(
            "Recommended target for age $age: " +
                com.nocta.app.domain.profile.SleepProfile(age).recommendedSleepLabel,
            style = NoctaTypography.labelSmall,
            color = NoctaTextTertiary
        )
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = NoctaShapes.small,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun WakeTimeEditor(
    wakeTime: java.time.LocalTime,
    onSave: (java.time.LocalTime) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton("−30m", onClick = { onSave(wakeTime.minusMinutes(30)) })
        Box(
            modifier = Modifier
                .clip(NoctaShapes.small)
                .background(NoctaBackground)
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            val hour24 = wakeTime.hour
            val hour12 = when (val h = hour24 % 12) { 0 -> 12; else -> h }
            val amPm = if (hour24 < 12) "AM" else "PM"
            Text(
                "%d:%02d %s".format(hour12, wakeTime.minute, amPm),
                style = NoctaTypography.titleMedium,
                color = NoctaTextPrimary
            )
        }
        StepperButton("+30m", onClick = { onSave(wakeTime.plusMinutes(30)) })
    }
}
