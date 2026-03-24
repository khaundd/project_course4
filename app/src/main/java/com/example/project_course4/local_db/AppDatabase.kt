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
        UserMealPlan::class
    ],
    version = 4
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun productsDao(): ProductsDao
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE products ADD COLUMN lastUsedAt INTEGER")
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
                .addMigrations(MIGRATION_3_4)
                .build()
            INSTANCE = instance
            Log.d("DatabaseProvider", "Экземпляр базы данных создан/получен")
            instance
        }
    }
}