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
    object SharedRecipe: Screen("sharedRecipe/{token}")
    object MealPlans: Screen("mealPlans")
    object MealPlanEditor: Screen("mealPlanEditor")
    object SelectProductForMealPlan: Screen("selectProductForMealPlan/{dayIndex}/{mealIndex}")
    // Fitness
    object Fitness: Screen("fitness")
    object ExerciseCatalog: Screen("exerciseCatalog")
    object ExerciseCatalogSelect: Screen("exerciseCatalogSelect")
    object ExerciseCatalogSelectPlan: Screen("exerciseCatalogSelectPlan/{dayIndex}")
    object ExerciseDetail: Screen("exerciseDetail/{exerciseId}")
    object TrainingLog: Screen("trainingLog")
    object TrainingEditor: Screen("trainingEditor")
    object TrainingPlans: Screen("trainingPlans")
    object TrainingPlanEditor: Screen("trainingPlanEditor")
    object TrainingPlanDetail: Screen("trainingPlanDetail/{planId}")
    object TrainingDetail: Screen("trainingDetail/{trainingId}")
    // Roles
    object RoleFeature: Screen("roleFeature/{roleId}")
    // Trainer
    object ClientStats : Screen("clientStats/{clientId}") {
        fun createRoute(clientId: Int) = "clientStats/$clientId"
    }
    object SelectTrainer : Screen("selectTrainer")
    // Active workout
    object ActiveWorkout: Screen("activeWorkout")
    object WorkoutSummary: Screen("workoutSummary")
    object ExerciseCatalogSelectActive: Screen("exerciseCatalogSelectActive")
    // Statistics
    object Statistics: Screen("statistics")
}