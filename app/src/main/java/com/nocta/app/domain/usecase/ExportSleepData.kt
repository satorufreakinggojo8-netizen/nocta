package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.SleepSession
import com.nocta.app.domain.repository.MorningCheckInRepository
import com.nocta.app.domain.repository.SleepRepository
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
 */
class ExportSleepData @Inject constructor(
    private val sleepRepository: SleepRepository,
    private val morningCheckInRepository: MorningCheckInRepository
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
        return root.toString(2)
    }
}
