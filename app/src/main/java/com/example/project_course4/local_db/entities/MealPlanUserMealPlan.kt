package com.example.project_course4.local_db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_plan_user_meal_plan",
    foreignKeys = [
        ForeignKey(
            entity = UserMealPlan::class,
            parentColumns = ["id"],
            childColumns = ["userMealPlanId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MealPlan::class,
            parentColumns = ["planId"],
            childColumns = ["mealPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    )
data class MealPlanUserMealPlan(
    @PrimaryKey(true) val id: Int = 0,
    val mealPlanId: Int,
    val userMealPlanId: Int
)
