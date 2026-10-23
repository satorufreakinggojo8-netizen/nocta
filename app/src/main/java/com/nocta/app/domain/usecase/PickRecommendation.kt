package com.nocta.app.domain.usecase

import com.nocta.app.domain.model.Difficulty
import com.nocta.app.domain.model.Recommendation
import com.nocta.app.domain.model.RecommendationCategory
import com.nocta.app.domain.model.SleepScoreBreakdown
import com.nocta.app.domain.model.SleepSession
import javax.inject.Inject

/**
 * Minimal, transparent rule-based picker shared by the dashboard's one-line
 * focus and its detail page — so the two can never disagree about what today's
 * suggestion is.
 *
 * Every branch is driven by recorded values; with no scored history there is
 * no suggestion at all rather than a generic tip presented as personalised.
 */
class PickRecommendation @Inject constructor() {

    operator fun invoke(
        lastNight: SleepSession?,
        breakdown: SleepScoreBreakdown?
    ): Recommendation? {
        if (breakdown == null || !breakdown.hasData) return null

        return when {
            breakdown.consistencyScore < LOW_CONSISTENCY_THRESHOLD -> Recommendation(
                id = "rec_consistency",
                title = "Steady your bedtime",
                why = "Your bedtime has varied more than usual over the last two weeks.",
                whatToDo = "Pick one bedtime and hold it for the next 5 nights, weekends included.",
                difficulty = Difficulty.MODERATE,
                expectedBenefit = "More consistent sleep timing tends to improve how rested you feel.",
                category = RecommendationCategory.SCHEDULE
            )

            (lastNight?.nightAwakenings ?: 0) >= MANY_AWAKENINGS -> Recommendation(
                id = "rec_environment",
                title = "Check your sleep environment",
                why = "You logged multiple awakenings last night.",
                whatToDo = "Try a cooler, darker room tonight and see if awakenings drop.",
                difficulty = Difficulty.EASY,
                expectedBenefit = "A quieter, darker, cooler room commonly reduces night waking.",
                category = RecommendationCategory.ENVIRONMENT
            )

            else -> Recommendation(
                id = "rec_screens",
                title = "Ease off screens before bed",
                why = "Reducing late-night screen exposure may help you fall asleep more consistently.",
                whatToDo = "Try keeping your phone away from your bed tonight.",
                difficulty = Difficulty.EASY,
                expectedBenefit = "Many people fall asleep faster with less late-night screen light.",
                category = RecommendationCategory.WIND_DOWN
            )
        }
    }

    private companion object {
        const val LOW_CONSISTENCY_THRESHOLD = 70
        const val MANY_AWAKENINGS = 2
    }
}
