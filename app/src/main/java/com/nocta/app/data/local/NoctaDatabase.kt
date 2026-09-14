package com.nocta.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SleepSessionEntity::class,
        AiMessageEntity::class,
        SleepTrackingSessionEntity::class,
        MorningCheckInEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class NoctaDatabase : RoomDatabase() {

    abstract fun sleepDao(): SleepDao

    abstract fun aiMessageDao(): AiMessageDao

    abstract fun sleepTrackingDao(): SleepTrackingDao

    abstract fun morningCheckInDao(): MorningCheckInDao
}
