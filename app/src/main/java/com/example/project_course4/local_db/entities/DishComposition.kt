package com.example.project_course4.local_db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dish_composition",
    foreignKeys = [
        ForeignKey(
            entity = Products::class,
            parentColumns = ["productId"],
            childColumns = ["dishId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Products::class,
            parentColumns = ["productId"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
data class DishComposition(
    @PrimaryKey(true) val id: Int = 0,
    val dishId: Int,
    val productId: Int,
    val productWeight: UShort
)
