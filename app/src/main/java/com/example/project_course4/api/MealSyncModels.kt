package com.example.project_course4.api

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
data class RecipeIngredient(
    @SerialName("product_id") val productId: Int,
    val weight: Int
)

@Serializable
data class CreateRecipeRequest(
    @SerialName("dish_name") val dishName: String,
    val ingredients: List<RecipeIngredient>,
    @SerialName("after_cooking_weight") val afterCookingWeight: Float,
    val description: String = ""
)

@Serializable
data class CreateRecipeResponse(
    val result: String? = null,
    val error: String? = null
)

@Serializable
data class DishIngredient(
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("product_name") val productName: String = "",
    @SerialName("proteins") val protein: Float = 0f,
    val fats: Float = 0f,
    val carbs: Float = 0f,
    val calories: Float = 0f,
    val weight: Int = 0
)

@Serializable
data class RecipeResponse(
    @SerialName("product_name") val productName: String = "",
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("dish_composition") val dishComposition: List<DishIngredient> = emptyList()
) {
    val name: String get() = productName

    // БЖУ считаем из состава пропорционально весу (на 100г каждого ингредиента)
    val protein: Float get() = dishComposition.sumOf { it.protein * it.weight / 100.0 }.toFloat()
    val fats: Float get() = dishComposition.sumOf { it.fats * it.weight / 100.0 }.toFloat()
    val carbs: Float get() = dishComposition.sumOf { it.carbs * it.weight / 100.0 }.toFloat()
    val calories: Float get() = dishComposition.sumOf { it.calories * it.weight / 100.0 }.toFloat()
}

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
