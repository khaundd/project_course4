package com.example.project_course4.local_db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_meal_component",
    foreignKeys = [
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["mealId"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE // Если удалим прием пищи, связи удалятся сами
        ),
        ForeignKey(
            entity = MealComponent::class,
            parentColumns = ["id"],
            childColumns = ["mealComponentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealId"), Index("mealComponentId")]
)

data class MealMealComponent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mealId: Int,
    val mealComponentId: Int
)
