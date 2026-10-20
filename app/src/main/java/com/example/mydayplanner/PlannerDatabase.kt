package com.example.mydayplanner

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class PlannerTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val dueAtMillis: Long,
    val completed: Boolean = false,
    val recurring: Boolean = false,
    val weekdays: String = "" // e.g. "1,2,3,4,5" where Monday=1 and Sunday=7
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueAtMillis ASC")
    fun observeTasks(): Flow<List<PlannerTask>>

    @Insert
    suspend fun insert(task: PlannerTask): Long

    @Update
    suspend fun update(task: PlannerTask)

    @Delete
    suspend fun delete(task: PlannerTask)

    @Query("SELECT * FROM tasks WHERE recurring = 1")
    suspend fun recurringTasks(): List<PlannerTask>
}

@Database(entities = [PlannerTask::class], version = 1, exportSchema = false)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: PlannerDatabase? = null
        fun get(context: Context): PlannerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    "my_day_planner.db"
                ).build().also { INSTANCE = it }
            }
    }
}
