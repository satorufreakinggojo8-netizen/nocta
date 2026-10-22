package com.nocta.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Executes the real migration DDL against SQLite (Robolectric), so the SQL the
 * app will run on upgrade is checked even though no device is available here.
 * Room's own schema-identity validation still needs an instrumented test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MigrationSqlTest {

    private var helper: SupportSQLiteOpenHelper? = null
    private var db: SupportSQLiteDatabase? = null

    private fun database(): SupportSQLiteDatabase {
        if (db == null) {
            val context = RuntimeEnvironment.getApplication()
            val created = FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name("migration-${System.nanoTime()}.db")
                    .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int
                        ) = Unit
                    })
                    .build()
            )
            helper = created
            db = created.writableDatabase
        }
        return db!!
    }

    @After
    fun tearDown() {
        helper?.close()
    }

    private fun columns(table: String): List<String> {
        val names = mutableListOf<String>()
        database().query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) names += cursor.getString(nameIndex)
        }
        return names
    }

    private fun rowCount(table: String): Int {
        database().query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    @Test
    fun `migration 4 to 5 adds the journal and routine tables`() {
        MIGRATION_4_5.migrate(database())

        assertEquals(
            listOf(
                "dateEpochDay", "restfulness", "stressLevel", "napMinutes",
                "caffeineDrinks", "lastCaffeineMinutes", "exercisedMinutes",
                "lastExerciseMinutes", "screenUseBeforeBed", "routineCompleted",
                "notes", "updatedAtEpochMillis"
            ),
            columns("journal_entries")
        )
        assertEquals(
            listOf(
                "id", "dateEpochDay", "plannedMinutes", "startedAtEpochMillis",
                "accumulatedPauseMillis", "pausedAtEpochMillis", "endedAtEpochMillis",
                "outcome", "completedStepIds"
            ),
            columns("wind_down_runs")
        )
    }

    @Test
    fun `journal rows accept null answers and reject a second row for one date`() {
        MIGRATION_4_5.migrate(database())

        database().execSQL(
            "INSERT INTO journal_entries (dateEpochDay, restfulness, updatedAtEpochMillis) " +
                "VALUES (20000, NULL, 42)"
        )
        database().execSQL(
            "INSERT OR REPLACE INTO journal_entries (dateEpochDay, restfulness, updatedAtEpochMillis) " +
                "VALUES (20000, 4, 43)"
        )

        assertEquals(1, rowCount("journal_entries"))
        database().query(
            "SELECT restfulness, stressLevel FROM journal_entries WHERE dateEpochDay = 20000"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(4, cursor.getInt(0))
            assertTrue(cursor.isNull(1))
        }
    }

    @Test
    fun `routine rows store pause bookkeeping and survive a re-run of the migration`() {
        MIGRATION_4_5.migrate(database())
        // Idempotent: an interrupted upgrade that runs again must not throw.
        MIGRATION_4_5.migrate(database())

        database().execSQL(
            "INSERT INTO wind_down_runs (id, dateEpochDay, plannedMinutes, startedAtEpochMillis, " +
                "accumulatedPauseMillis, pausedAtEpochMillis, endedAtEpochMillis, outcome, " +
                "completedStepIds) VALUES ('r1', 20000, 45, 1000, 5000, NULL, 9000, 'COMPLETED', " +
                "'dim_lights|read')"
        )

        assertEquals(1, rowCount("wind_down_runs"))
        database().query(
            "SELECT outcome, completedStepIds FROM wind_down_runs WHERE id = 'r1'"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("COMPLETED", cursor.getString(0))
            assertEquals("dim_lights|read", cursor.getString(1))
        }
    }
}
