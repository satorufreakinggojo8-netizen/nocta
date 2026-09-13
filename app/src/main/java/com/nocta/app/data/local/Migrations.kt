package com.nocta.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: add the sleep tracking table. Purely additive — no existing table,
 * column, or index is touched, so existing SleepSession/AiMessage rows are
 * preserved exactly.
 *
 * The SQL here must match SleepTrackingSessionEntity's generated schema
 * byte-for-byte or Room throws at open time. Verify with:
 *   ./gradlew :app:assembleDebug
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sleep_tracking_sessions` (" +
                "`id` TEXT NOT NULL, " +
                "`startedAtEpochMillis` INTEGER NOT NULL, " +
                "`endedAtEpochMillis` INTEGER, " +
                "`source` TEXT NOT NULL, " +
                "`synced` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_sleep_tracking_sessions_startedAtEpochMillis` " +
                "ON `sleep_tracking_sessions` (`startedAtEpochMillis`)"
        )
    }
}
