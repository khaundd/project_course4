package com.example.project_course4.local_db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.project_course4.local_db.entities.MealEntity
import com.example.project_course4.local_db.dao.MealDao
import com.example.project_course4.local_db.dao.ProductsDao
import com.example.project_course4.local_db.dao.ActiveWorkoutDao
import com.example.project_course4.local_db.entities.*

@Database(
    entities = [
        DishComposition::class,
        MealEntity::class,
        MealComponent::class,
        MealMealComponent::class,
        MealMealPlanDay::class,
        MealPlan::class,
        MealPlanDay::class,
        MealPlanUserMealPlan::class,
        Products::class,
        UserMealPlan::class,
        ActiveWorkoutEntity::class,
        ActiveWorkoutSetEntity::class
    ],
    version = 7
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun productsDao(): ProductsDao
    abstract fun activeWorkoutDao(): ActiveWorkoutDao
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE products ADD COLUMN lastUsedAt INTEGER")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE meal ADD COLUMN fromPlanId INTEGER")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS active_workout (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                startedAt INTEGER NOT NULL,
                finishedAt INTEGER,
                notes TEXT NOT NULL DEFAULT '',
                sourceTrainingId INTEGER
            )
        """.trimIndent())
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS active_workout_set (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                workoutId INTEGER NOT NULL,
                exerciseId INTEGER NOT NULL,
                exerciseNameRu TEXT NOT NULL,
                setNumber INTEGER NOT NULL,
                weightKg REAL,
                reps INTEGER,
                durationSec INTEGER,
                restTimeSec INTEGER NOT NULL DEFAULT 90,
                isSkipped INTEGER NOT NULL DEFAULT 0,
                completedAt INTEGER,
                FOREIGN KEY(workoutId) REFERENCES active_workout(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_set_workoutId ON active_workout_set(workoutId)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE active_workout ADD COLUMN pausedElapsedSec INTEGER NOT NULL DEFAULT 0")
    }
}

object DatabaseProvider {
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            Log.d("DatabaseProvider", "Создание или получение экземпляра базы данных")
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_db"
            )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build()
            INSTANCE = instance
            Log.d("DatabaseProvider", "Экземпляр базы данных создан/получен")
            instance
        }
    }
}