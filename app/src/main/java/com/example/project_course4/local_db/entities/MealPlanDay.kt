package com.example.project_course4.local_db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_plan_day",
    foreignKeys = [
        ForeignKey(
            entity = MealPlan::class,
            parentColumns = ["planId"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
    )
data class MealPlanDay(
    @PrimaryKey(true) val mealPlanDayId: Int = 0,
    val planId: Int,
    val dayNumber: UByte,
    val dayOfWeek: UByte
)
