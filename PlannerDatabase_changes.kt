/*
Merge this pattern into your existing PlannerDatabase.kt.
Do not keep two PlannerDatabase classes.

@Entity classes:
  entities = [PlannerTask::class, PlannerSettings::class]

Database:
  @Database(entities = [PlannerTask::class, PlannerSettings::class],
            version = 2, exportSchema = false)
  abstract class PlannerDatabase : RoomDatabase() {
      abstract fun taskDao(): TaskDao
      abstract fun settingsDao(): SettingsDao
      companion object {
          @Volatile private var INSTANCE: PlannerDatabase? = null
          fun get(context: Context): PlannerDatabase =
              INSTANCE ?: synchronized(this) {
                  INSTANCE ?: Room.databaseBuilder(
                      context.applicationContext,
                      PlannerDatabase::class.java,
                      "my_day_planner.db"
                  )
                  // Add a Migration(1, 2) that creates planner_settings table before
                  // shipping to existing users. Do not use destructive migration in production.
                  .build().also { INSTANCE = it }
              }
      }
  }

For development only, if you have no user data to preserve, uninstall the app before
installing this schema or clear app storage. For a released app, implement a Room Migration.
*/
