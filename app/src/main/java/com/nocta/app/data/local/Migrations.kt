package com.nocta.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sleep_tracking_sessions (
                id TEXT NOT NULL,
                startedAtEpochMillis INTEGER NOT NULL,
                endedAtEpochMillis INTEGER,
                source TEXT NOT NULL,
                synced INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_sleep_tracking_sessions_startedAtEpochMillis
            ON sleep_tracking_sessions(startedAtEpochMillis)
            """.trimIndent()
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS morning_check_ins (
                trackingSessionId TEXT NOT NULL,
                sleepQuality INTEGER NOT NULL,
                nightAwakenings INTEGER NOT NULL,
                awakeMinutes INTEGER NOT NULL,
                morningEnergy INTEGER NOT NULL,
                disturbance TEXT NOT NULL,
                unusualEvent TEXT,
                PRIMARY KEY(trackingSessionId)
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_profile (
                id INTEGER NOT NULL,
                age INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
    }
}

/**
 * v5 adds the optional daily journal and the wind-down runs that back the
 * routine history. Purely additive: no existing table is touched, so a v4
 * database keeps every sleep record, check-in and chat message.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS journal_entries (
                dateEpochDay INTEGER NOT NULL,
                restfulness INTEGER,
                stressLevel INTEGER,
                napMinutes INTEGER,
                caffeineDrinks INTEGER,
                lastCaffeineMinutes INTEGER,
                exercisedMinutes INTEGER,
                lastExerciseMinutes INTEGER,
                screenUseBeforeBed INTEGER,
                routineCompleted INTEGER,
                notes TEXT,
                updatedAtEpochMillis INTEGER NOT NULL,
                PRIMARY KEY(dateEpochDay)
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS wind_down_runs (
                id TEXT NOT NULL,
                dateEpochDay INTEGER NOT NULL,
                plannedMinutes INTEGER NOT NULL,
                startedAtEpochMillis INTEGER NOT NULL,
                accumulatedPauseMillis INTEGER NOT NULL,
                pausedAtEpochMillis INTEGER,
                endedAtEpochMillis INTEGER,
                outcome TEXT NOT NULL,
                completedStepIds TEXT NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_wind_down_runs_startedAtEpochMillis
            ON wind_down_runs(startedAtEpochMillis)
            """.trimIndent()
        )
    }
}
