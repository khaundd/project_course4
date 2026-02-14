package com.example.project_course4.local_db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.project_course4.local_db.entities.Meal
import com.example.project_course4.local_db.dao.MealDao
import com.example.project_course4.local_db.dao.ProductsDao
import com.example.project_course4.local_db.entities.*

@Database(
    entities = [
        DishComposition::class,
        Meal::class,
        MealComponent::class,
        MealMealComponent::class,
        MealMealPlanDay::class,
        MealPlan::class,
        MealPlanDay::class,
        MealPlanUserMealPlan::class,
        Products::class,
        UserMealPlan::class
    ],
    version = 1
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun productsDao(): ProductsDao
}

object DatabaseProvider {
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_db"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}