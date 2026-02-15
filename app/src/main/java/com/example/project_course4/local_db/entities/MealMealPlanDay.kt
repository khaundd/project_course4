package com.example.project_course4.local_db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_meal_plan_day",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["mealId"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MealPlanDay::class,
            parentColumns = ["mealPlanDayId"],
            childColumns = ["mealPlanDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
    )
data class MealMealPlanDay(
    @PrimaryKey(true) val id: Int = 0,
    val mealId: Int,
    val mealPlanDayId: Int
)
