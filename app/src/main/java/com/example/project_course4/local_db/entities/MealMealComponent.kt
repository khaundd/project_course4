package com.example.project_course4.local_db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_meal_component",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["mealId"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE // Если удалим прием пищи, связи удалятся сами
        )
    ],
    indices = [Index("mealId"),]
)

data class MealMealComponent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mealId: Int
)
