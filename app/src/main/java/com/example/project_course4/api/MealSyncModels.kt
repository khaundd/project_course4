package com.example.project_course4.api

import kotlinx.serialization.Serializable

@Serializable
data class MealSyncRequest(
    val meals: List<MealData>
)

@Serializable
data class MealData(
    val name: String,
    val mealTime: String, // Формат: "YYYY-MM-DD hh:mm:ss"
    val components: List<MealComponentData>
)

@Serializable
data class MealComponentData(
    val productId: Int,
    val weight: Int
)

@Serializable
data class MealSyncResponse(
    val success: Boolean,
    val message: String? = null
)

@Serializable
data class MealLoadResponse(
    val success: Boolean,
    val meals: List<MealData>? = null,
    val message: String? = null
)
