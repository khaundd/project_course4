package com.example.project_course4

sealed class Screen(val route: String){
    object Main: Screen("main")
    object SelectProduct: Screen("selectProduct")
    data class SelectProductWithMeal(val mealId: String) : Screen("selectProductWithMeal/$mealId")
}