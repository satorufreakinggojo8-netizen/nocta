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
