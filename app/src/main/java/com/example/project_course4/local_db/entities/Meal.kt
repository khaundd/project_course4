package com.example.project_course4.local_db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal")
data class Meal(
    @PrimaryKey(autoGenerate = true) val mealId: Int = 0,
    val name: String,
    val mealTime: Long,
)
