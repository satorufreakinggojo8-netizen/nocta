package com.nocta.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SleepSessionEntity::class, AiMessageEntity::class],
    version = 1,
    exportSchema = true // schemas exported to app/schemas for migration diffing
)
abstract class NoctaDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
    abstract fun aiMessageDao(): AiMessageDao
}
