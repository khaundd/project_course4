package com.example.project_course4.local_db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "meal_component",
    foreignKeys = [
        ForeignKey(
            entity = Products::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE // Если продукт удалится, удалятся и компоненты
        ),
        ForeignKey(
            entity = MealMealComponent::class,
            parentColumns = ["id"],
            childColumns = ["mealMealComponentId"], // Ссылка на ID из таблицы выше
            onDelete = ForeignKey.CASCADE
        ),
    ])
data class MealComponent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val mealMealComponentId: Int,
    val weight: UShort,
)
