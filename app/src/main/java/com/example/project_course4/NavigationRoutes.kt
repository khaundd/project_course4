package com.example.project_course4

sealed class Screen(val route: String){
    object Main: Screen("main")
    object SelectProduct: Screen("selectProduct")
    object ProductCreation: Screen("productCreation")
    object Login: Screen("login")
    object Registration: Screen("registration")
    object Verification: Screen("verification")
    object PasswordReset: Screen("passwordReset")
    object Profile: Screen("profile")
    object Products: Screen("products")
    object Recipes: Screen("recipes")
    object RecipeCreation: Screen("recipeCreation")
    object SelectProductForRecipe: Screen("selectProductForRecipe")
    object DishComposition: Screen("dishComposition/{dishName}")
    object RecipeEdit: Screen("recipeEdit/{dishName}")
}