package com.example.project_course4.local_db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "products")
data class Products(
    @PrimaryKey(true) val productId: Int = 0,
    val productName: String,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
    val barcode: Long,
    val isDish: Boolean,
    val createdBy: Int,
    val isSavedLocally: Boolean = false
){
    val calories: Float
        get() = protein * 4 + fat * 9 + carbs * 4
}
