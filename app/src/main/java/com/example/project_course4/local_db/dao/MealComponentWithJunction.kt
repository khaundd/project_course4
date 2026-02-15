package com.example.project_course4.local_db.dao

/**
 * Результат join meal_component и meal_meal_component для загрузки приёмов пищи из БД.
 */
data class MealComponentWithJunction(
    val mealId: Int,
    val junctionId: Int,
    val productId: Int,
    val weight: Int
)
