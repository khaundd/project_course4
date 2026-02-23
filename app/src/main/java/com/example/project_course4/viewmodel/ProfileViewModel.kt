package com.example.project_course4.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_course4.SessionManager
import com.example.project_course4.composable_elements.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _profileData = MutableStateFlow(ProfileData())
    val profileData: StateFlow<ProfileData> = _profileData.asStateFlow()
    
    private val _dailyCalories = MutableStateFlow(1950f)
    val dailyCalories: StateFlow<Float> = _dailyCalories.asStateFlow()
    
    private val _macroNutrients = MutableStateFlow(MacroNutrients(100f, 70f, 230f))
    val macroNutrients: StateFlow<MacroNutrients> = _macroNutrients.asStateFlow()
    
    init {
        loadProfileData()
    }
    
    private fun loadProfileData() {
        viewModelScope.launch {
            // Загружаем сохраненные данные из SharedPreferences
            // В будущем здесь можно добавить загрузку других параметров
            calculateNutrition()
        }
    }
    
    fun updateWeight(weight: Float) {
        _profileData.value = _profileData.value.copy(weight = weight)
        calculateNutrition()
        saveProfileData()
    }
    
    fun updateHeight(height: Float) {
        _profileData.value = _profileData.value.copy(height = height)
        calculateNutrition()
        saveProfileData()
    }
    
    fun updateAge(age: Int) {
        _profileData.value = _profileData.value.copy(age = age)
        calculateNutrition()
        saveProfileData()
    }
    
    fun updateGoal(goal: NutritionGoal) {
        _profileData.value = _profileData.value.copy(goal = goal)
        calculateNutrition()
        saveProfileData()
    }
    
    fun updateGender(gender: Gender) {
        _profileData.value = _profileData.value.copy(gender = gender)
        calculateNutrition()
        saveProfileData()
    }
    
    private fun calculateNutrition() {
        val calories = NutritionCalculator.calculateDailyCalories(_profileData.value)
        val macros = NutritionCalculator.calculateMacroNutrients(calories)
        
        _dailyCalories.value = calories
        _macroNutrients.value = macros
    }
    
    private fun saveProfileData() {
        // В будущем можно добавить сохранение всех параметров в SharedPreferences
    }
    
    fun getUserEmail(): String? {
        return sessionManager.fetchEmail()
    }
}
