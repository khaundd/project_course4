package com.example.project_course4.api

import com.example.project_course4.Product
import kotlinx.serialization.SerialName
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

@Serializable
data class NameCheckResponse(
    val exists: Boolean
)

@Serializable
data class ProductCreateRequest(
    @SerialName("product_name") val name: String,
    @SerialName("proteins") val protein: Float,
    @SerialName("fats") val fats: Float,
    @SerialName("carbs") val carbs: Float,
    val calories: Float,
    val barcode: String = ""
)

@Serializable
data class ProductCreateResponse(
    val success: Boolean = true,
    val product: ProductResponse? = null,
    val message: String? = null
)

@Serializable
data class ProductResponse(
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("product_name") val name: String? = null,
    @SerialName("proteins") val protein: Float? = null,
    @SerialName("fats") val fats: Float? = null,
    @SerialName("carbs") val carbs: Float? = null,
    val calories: Float? = null,
    val barcode: String? = null,
    val isDish: Boolean? = false,
    val createdBy: Int? = null
)
