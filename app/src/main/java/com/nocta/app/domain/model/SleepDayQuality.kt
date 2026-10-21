package com.nocta.app.domain.model

/**
 * Sleep-quality levels shown by the sleep-history calendar.
 *
 * Five real levels plus [NO_RECORD], the neutral state for days (and months)
 * without any saved sleep session. This is a wellness visualization built
 * from the user's own saved records — never a medical judgment.
 */
enum class SleepDayQuality(val displayName: String) {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    FAIR("Fair"),
    POOR("Poor"),
    VERY_POOR("Very poor"),
    NO_RECORD("No record")
}
