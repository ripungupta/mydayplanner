package com.example.mydayplanner

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "planner_settings")
data class PlannerSettings(
    @PrimaryKey val id: Int = 1,
    val morningEnabled: Boolean = true,
    val morningHour: Int = 7,
    val morningMinute: Int = 0,
    val officeHour: Int = 9,
    val officeMinute: Int = 0,
    val personalHour: Int = 19,
    val personalMinute: Int = 0,
    val eveningEnabled: Boolean = true,
    val eveningHour: Int = 21,
    val eveningMinute: Int = 30,
    // Monday=1 ... Sunday=7. Workdays default Monday through Saturday.
    val workdaysCsv: String = "1,2,3,4,5,6",
    val relaxedSunday: Boolean = true
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM planner_settings WHERE id = 1")
    fun observeSettings(): Flow<PlannerSettings?>

    @Query("SELECT * FROM planner_settings WHERE id = 1")
    suspend fun getSettings(): PlannerSettings?

    @Upsert
    suspend fun save(settings: PlannerSettings)
}

/*
Merge these additions into PlannerDatabase:
  abstract fun settingsDao(): SettingsDao
and add PlannerSettings::class to @Database entities.
For an existing installed build, bump the database version and add a real migration.
*/
