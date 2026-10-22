package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.JournalEntry
import com.nocta.app.domain.model.WindDownRun
import com.nocta.app.domain.repository.MorningCheckInRepository
import com.nocta.app.domain.repository.SettingsRepository
import com.nocta.app.domain.repository.SleepJournalRepository
import com.nocta.app.domain.repository.SleepRepository
import com.nocta.app.domain.repository.WindDownRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import javax.inject.Inject

/**
 * Builds a plain-JSON export of everything Nocta stores locally about sleep.
 *
 * The export is generated on-device and handed to the user (share/save via the
 * system share sheet) — it is never uploaded anywhere by the app. Morning
 * check-ins are keyed by tracking-session id, which equals the sleep-session
 * id (see CreateSleepSession), so each exported night carries its check-in.
 *
 * Since v5 the export also carries the journal (including its notes, because it
 * is the user's own data leaving to the user), wind-down run history, and the
 * schedule settings that shape the plan.
 */
class ExportSleepData @Inject constructor(
    private val sleepRepository: SleepRepository,
    private val morningCheckInRepository: MorningCheckInRepository,
    private val journalRepository: SleepJournalRepository,
    private val windDownRepository: WindDownRepository,
    private val settingsRepository: SettingsRepository
) {

    suspend operator fun invoke(): String {
        val sessions = sleepRepository.getAllSessions()

        val root = JSONObject()
        root.put("app", "Nocta")
        root.put("exportedOn", LocalDate.now().toString())
        root.put(
            "note",
            "Complete export of sleep records stored on this device. " +
                "Fields marked estimated reflect the tracking source; see source field."
        )

        val nights = JSONArray()
        sessions.forEach { session ->
            val night = JSONObject()
            night.put("date", session.date.toString())
            night.put("bedtime", session.bedtime.toString())
            night.put("estimatedSleepTime", session.estimatedSleepTime.toString())
            night.put("wakeTime", session.wakeTime.toString())
            night.put("timeAsleepMinutes", session.durationMinutes)
            night.put("awakeMinutes", session.awakeMinutes)
            night.put("nightAwakenings", session.nightAwakenings)
            night.put("source", session.source.name)
            // 0 means "not answered", never "score zero" — keep the distinction
            // explicit in the export so external tools don't misread it.
            night.put("sleepQuality", session.sleepQuality.takeIf { it in 1..5 } ?: JSONObject.NULL)
            night.put("morningEnergy", session.morningEnergy.takeIf { it in 1..5 } ?: JSONObject.NULL)

            val checkIn = morningCheckInRepository.getForSession(session.id)
            if (checkIn != null) {
                val checkInJson = JSONObject()
                checkInJson.put("sleepQuality", checkIn.sleepQuality)
                checkInJson.put("nightAwakenings", checkIn.nightAwakenings)
                checkInJson.put("awakeMinutes", checkIn.awakeMinutes)
                checkInJson.put("morningEnergy", checkIn.morningEnergy)
                checkInJson.put("disturbance", checkIn.disturbance.name)
                checkInJson.put("unusualEvent", checkIn.unusualEvent ?: JSONObject.NULL)
                night.put("morningCheckIn", checkInJson)
            }
            nights.put(night)
        }

        root.put("nights", nights)
        root.put("journal", journalToJson(journalRepository.getAll()))
        root.put("windDownRuns", runsToJson(windDownRepository))
        root.put("settings", settingsToJson())
        return root.toString(2)
    }

    private fun journalToJson(entries: List<JournalEntry>): JSONArray {
        val array = JSONArray()
        entries.forEach { entry ->
            // A null field means "not answered" and is exported as JSON null.
            val item = JSONObject()
            item.put("date", entry.date.toString())
            item.put("restfulness", entry.restfulness ?: JSONObject.NULL)
            item.put("stressLevel", entry.stressLevel ?: JSONObject.NULL)
            item.put("napMinutes", entry.napMinutes ?: JSONObject.NULL)
            item.put("caffeineDrinks", entry.caffeineDrinks ?: JSONObject.NULL)
            item.put("lastCaffeineTime", entry.lastCaffeineTime?.toString() ?: JSONObject.NULL)
            item.put("exercisedMinutes", entry.exercisedMinutes ?: JSONObject.NULL)
            item.put("lastExerciseTime", entry.lastExerciseTime?.toString() ?: JSONObject.NULL)
            item.put("screenUseBeforeBed", entry.screenUseBeforeBed ?: JSONObject.NULL)
            item.put("routineCompleted", entry.routineCompleted ?: JSONObject.NULL)
            item.put("notes", entry.notes ?: JSONObject.NULL)
            array.put(item)
        }
        return array
    }

    private suspend fun runsToJson(repository: WindDownRepository): JSONArray {
        // Only terminal runs are meaningful history; an active run is re-read
        // from the repository's active flow, not exported as a completed one.
        val array = JSONArray()
        val completed = repository.completedRunsForExport()
        completed.forEach { run: WindDownRun ->
            val item = JSONObject()
            item.put("date", run.date.toString())
            item.put("outcome", run.outcome.name)
            item.put("plannedMinutes", run.plannedMinutes)
            item.put("startedAtEpochMillis", run.startedAtMillis)
            item.put("elapsedMinutes", run.elapsedMinutesAt(run.endedAtMillis ?: run.startedAtMillis))
            item.put("completedSteps", JSONArray(run.completedStepIds.sorted()))
            array.put(item)
        }
        return array
    }

    private suspend fun settingsToJson(): JSONObject {
        val settings = JSONObject()
        settings.put("wakeTime", settingsRepository.getWakeTime().toString())
        settings.put("activePreset", settingsRepository.getActivePreset().name)
        settings.put("windDownMinutes", settingsRepository.getWindDownMinutes())
        val steps = JSONArray()
        settingsRepository.getWindDownSteps().forEach { step ->
            steps.put(
                JSONObject().apply {
                    put("id", step.id)
                    put("label", step.label)
                    put("enabled", step.isEnabled)
                }
            )
        }
        settings.put("windDownSteps", steps)
        return settings
    }
}
