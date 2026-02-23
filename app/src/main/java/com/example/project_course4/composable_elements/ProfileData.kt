package com.example.project_course4.composable_elements

data class ProfileData(
    val weight: Float = 0f,
    val height: Float = 0f,
    val age: Int = 0,
    val goal: NutritionGoal = NutritionGoal.MAINTAIN,
    val gender: Gender = Gender.MALE
)

enum class NutritionGoal(val displayName: String, val coefficient: Float) {
    GAIN("Набрать +10% кКал", 1.1f),
    MAINTAIN("Поддерживать", 1.0f),
    LOSE("Сбросить -10% кКал", 0.9f)
}

enum class Gender(val displayName: String) {
    MALE("Мужчина"),
    FEMALE("Женщина")
}

object NutritionCalculator {
    fun calculateBMR(profileData: ProfileData): Float {
        val (weight, height, age, goal, gender) = profileData
        
        return when (gender) {
            Gender.MALE -> (10 * weight) + (6.25f * height) - (5 * age) + 5f
            Gender.FEMALE -> (10 * weight) + (6.25f * height) - (5 * age) - 161f
        }
    }
    
    fun calculateDailyCalories(profileData: ProfileData): Float {
        val bmr = calculateBMR(profileData)
        return bmr * profileData.goal.coefficient
    }
    
    fun calculateMacroNutrients(calories: Float): MacroNutrients {
        val proteinCalories = calories * 0.2f
        val fatsCalories = calories * 0.3f
        val carbsCalories = calories * 0.5f
        
        return MacroNutrients(
            protein = (proteinCalories / 4f),
            fats = (fatsCalories / 9f),
            carbs = (carbsCalories / 4f)
        )
    }
}

data class MacroNutrients(
    val protein: Float,
    val fats: Float,
    val carbs: Float
)
