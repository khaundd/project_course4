package com.example.project_course4.local_db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_plan")
data class MealPlan(
    @PrimaryKey(true) val planId: Int = 0,
    val name: String,
    val description: String,
    val isPublic: Boolean,
    val createdBy: Int
)
