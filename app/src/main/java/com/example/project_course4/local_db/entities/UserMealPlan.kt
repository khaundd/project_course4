package com.example.project_course4.local_db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_meal_plan")
data class UserMealPlan(
    @PrimaryKey(true) val id: Int = 0,
    val userId: Int,
    val startedAt: Long, //TODO будет храниться в миллисекундах, добавить конвертер
    val endedAt: Long
)
