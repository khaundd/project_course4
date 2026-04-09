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
    val components: List<MealComponentData>,
    @SerialName("from_plan_id") val fromPlanId: Int? = null
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
data class RecipeShareResponse(
    val link: String? = null,
    val error: String? = null
)

@Serializable
data class RecipeVisibilityRequest(
    @SerialName("is_public") val isPublic: Boolean
)

@Serializable
data class RecipeVisibilityResponse(
    val success: Boolean = false,
    @SerialName("is_public") val isPublicRaw: Int = 0,
    val link: String? = null,
    val error: String? = null
) {
    val isPublic: Boolean get() = isPublicRaw != 0
}

@Serializable
data class RecipeResponse(
    @SerialName("product_name") val productName: String = "",
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("dish_composition") val dishComposition: List<DishIngredient> = emptyList(),
    @SerialName("is_public") val isPublicRaw: Int = 0,
    @SerialName("recipe_link") val recipeLink: String? = null
) {
    val isPublic: Boolean get() = isPublicRaw != 0
    val name: String get() = productName

    // БЖУ считаем из состава пропорционально весу (на 100г каждого ингредиента)
    val protein: Float get() = dishComposition.sumOf { it.protein * it.weight / 100.0 }.toFloat()
    val fats: Float get() = dishComposition.sumOf { it.fats * it.weight / 100.0 }.toFloat()
    val carbs: Float get() = dishComposition.sumOf { it.carbs * it.weight / 100.0 }.toFloat()
    val calories: Float get() = dishComposition.sumOf { it.calories * it.weight / 100.0 }.toFloat()
}

// ─── Meal Plan models ───────────────────────────────────────────────────────

@Serializable
data class MealPlanComponentData(
    val productId: Int,
    val weight: Int
)

@Serializable
data class MealPlanMealData(
    val name: String,
    @SerialName("meal_time") val mealTime: String = "12:00", // "HH:mm"
    val components: List<MealPlanComponentData> = emptyList()
)

@Serializable
data class MealPlanDayData(
    @SerialName("day_number") val dayNumber: Int,
    @SerialName("day_of_week") val dayOfWeek: Int? = null,
    val meals: List<MealPlanMealData> = emptyList(),
    @SerialName("meal_plan_day_id") val mealPlanDayId: Int = 0,
    val notes: String? = null
)

@Serializable
data class MealPlanData(
    @SerialName("plan_id") val planId: Int = 0,
    val name: String,
    val description: String = "",
    @SerialName("is_public") val isPublicRaw: Int = 0,
    @SerialName("created_by") val createdBy: Int = 0,
    val days: List<MealPlanDayData> = emptyList(),
    @SerialName("day_count") val dayCount: Int? = null,
    @SerialName("target_calories") val targetCalories: Float = 2000f,
    @SerialName("protein_pct") val proteinPct: Float = 30f,
    @SerialName("fats_pct") val fatsPct: Float = 30f,
    @SerialName("carbs_pct") val carbsPct: Float = 40f
) {
    val isPublic: Boolean get() = isPublicRaw != 0
    // Граммы БЖУ из процентов и калорийности
    val targetProteinG: Float get() = targetCalories * proteinPct / 100f / 4f
    val targetFatsG: Float get() = targetCalories * fatsPct / 100f / 9f
    val targetCarbsG: Float get() = targetCalories * carbsPct / 100f / 4f
}

@Serializable
data class MealPlanListResponse(
    val success: Boolean,
    val plans: List<MealPlanData> = emptyList(),
    val message: String? = null
)

@Serializable
data class MealPlanSaveRequest(
    val name: String,
    val description: String = "",
    @SerialName("is_public") val isPublic: Boolean = false,
    val days: List<MealPlanDayData> = emptyList(),
    @SerialName("target_calories") val targetCalories: Float = 2000f,
    @SerialName("protein_pct") val proteinPct: Float = 30f,
    @SerialName("fats_pct") val fatsPct: Float = 30f,
    @SerialName("carbs_pct") val carbsPct: Float = 40f
)

@Serializable
data class MealPlanSaveResponse(
    val success: Boolean,
    @SerialName("plan_id") val planId: Int = 0,
    @SerialName("user_meal_plan_id") val userMealPlanId: Int = 0,
    val message: String? = null
)

@Serializable
data class MealPlanAssignResponse(
    val success: Boolean,
    @SerialName("user_meal_plan_id") val userMealPlanId: Int = 0,
    @SerialName("plan_end_datetime") val planEndDatetime: String? = null,
    val message: String? = null
)

@Serializable
data class MealPlanSharedUser(
    @SerialName("user_id") val userId: Int,
    val username: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null
)

@Serializable
data class MealPlanSharedUsersResponse(
    val success: Boolean,
    val users: List<MealPlanSharedUser> = emptyList(),
    val message: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────

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
