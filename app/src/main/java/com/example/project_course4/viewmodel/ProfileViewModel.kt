package com.example.project_course4.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_course4.SessionManager
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.utils.ErrorHandler
import com.example.project_course4.composable_elements.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val sessionManager: SessionManager,
    private val clientAPI: ClientAPI? = null,
    private val dataUpdateEvent: kotlinx.coroutines.flow.SharedFlow<Unit>? = null
) : ViewModel() {
    
    private val _profileData = MutableStateFlow(ProfileData())
    val profileData: StateFlow<ProfileData> = _profileData.asStateFlow()
    
    private val _dailyCalories = MutableStateFlow(0f)
    val dailyCalories: StateFlow<Float> = _dailyCalories.asStateFlow()
    
    private val _macroNutrients = MutableStateFlow(MacroNutrients(0f, 0f, 0f))
    val macroNutrients: StateFlow<MacroNutrients> = _macroNutrients.asStateFlow()
    
    init {
        // Сразу загружаем данные из SharedPreferences для немедленного расчета
        loadLocalProfileData()
        
        // Подписываемся на события обновления данных от AuthViewModel
        dataUpdateEvent?.let { eventFlow ->
            viewModelScope.launch {
                eventFlow.collect {
                    Log.d("ProfileViewModel", "Получено событие обновления данных, перезагружаем профиль...")
                    loadProfileData()
                }
            }
        }
        
        loadProfileData()
    }
    
    fun refreshProfileData() {
        Log.d("ProfileViewModel", "=== ВЫЗОВ refreshProfileData ===")
        loadProfileData()
    }
    
    private fun loadLocalProfileData() {
        val weight = sessionManager.getProfileWeight()
        val height = sessionManager.getProfileHeight()
        val age = sessionManager.getProfileAge()
        val goalString = sessionManager.getProfileGoal()
        val genderString = sessionManager.getProfileGender()
        
        // Если есть сохраненные данные, используем их для расчета
        if (weight > 0f && height > 0f && age > 0) {
            val goal = when (goalString) {
                "GAIN" -> NutritionGoal.GAIN
                "LOSE" -> NutritionGoal.LOSE
                else -> NutritionGoal.MAINTAIN
            }
            
            val gender = when (genderString) {
                "FEMALE" -> Gender.FEMALE
                else -> Gender.MALE
            }
            
            val localProfileData = ProfileData(
                weight = weight,
                height = height,
                age = age,
                goal = goal,
                gender = gender
            )
            
            _profileData.value = localProfileData
            calculateNutrition()
        } else {
            Log.w("ProfileViewModel", "Нет локальных данных для расчета")
        }
    }
    
    private fun loadProfileData() {
        viewModelScope.launch {
            // Сначала пытаемся загрузить данные с сервера
            clientAPI?.let { api ->
                try {
                    Log.d("ProfileViewModel", "Попытка загрузки данных профиля с сервера...")
                    val result = api.getProfileData()
                    result.fold(
                        onSuccess = { profileData ->
                            Log.d("ProfileViewModel", "Полученные данные:")
                            Log.d("ProfileViewModel", "  - Height: ${profileData.height}")
                            Log.d("ProfileViewModel", "  - Bodyweight: ${profileData.bodyweight}")
                            Log.d("ProfileViewModel", "  - Age: ${profileData.age}")
                            Log.d("ProfileViewModel", "  - Goal: ${profileData.goal}")
                            Log.d("ProfileViewModel", "  - Gender: ${profileData.gender}")
                            
                            // Сохраняем полученные данные в SharedPreferences
                            Log.d("ProfileViewModel", "Сохранение данных в SharedPreferences...")
                            sessionManager.saveProfileData(
                                weight = profileData.bodyweight,
                                height = profileData.height,
                                age = profileData.age,
                                goal = profileData.goal,
                                gender = profileData.gender
                            )
                            Log.d("ProfileViewModel", "Данные сохранены в SharedPreferences")
                            
                            // Обновляем StateFlow
                            val goal = when (profileData.goal) {
                                "GAIN" -> NutritionGoal.GAIN
                                "LOSE" -> NutritionGoal.LOSE
                                else -> NutritionGoal.MAINTAIN
                            }
                            
                            val gender = when (profileData.gender) {
                                "FEMALE" -> Gender.FEMALE
                                else -> Gender.MALE
                            }
                            
                            val newProfileData = ProfileData(
                                weight = profileData.bodyweight,
                                height = profileData.height,
                                age = profileData.age,
                                goal = goal,
                                gender = gender
                            )
                            
                            _profileData.value = newProfileData
                            
                            calculateNutrition()
                            return@launch
                        },
                        onFailure = { error ->
                            Log.e("ProfileViewModel", "Ошибка загрузки данных профиля с сервера: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Исключение при загрузке данных профиля с сервера: ${e.message}", e)
                    val errorMessage = ErrorHandler.handleNetworkException(e)
                    Log.e("ProfileViewModel", "Обработанное сообщение: $errorMessage")
                }
            } ?: run {
                Log.w("ProfileViewModel", "ClientAPI недоступен, пропускаем загрузку с сервера")
            }
            
            // Если не удалось загрузить с сервера, загружаем из SharedPreferences
            val weight = sessionManager.getProfileWeight()
            val height = sessionManager.getProfileHeight()
            val age = sessionManager.getProfileAge()
            val goalString = sessionManager.getProfileGoal()
            val genderString = sessionManager.getProfileGender()
            
            Log.d("ProfileViewModel", "Данные из SharedPreferences:")
            Log.d("ProfileViewModel", "  - Weight: $weight")
            Log.d("ProfileViewModel", "  - Height: $height")
            Log.d("ProfileViewModel", "  - Age: $age")
            Log.d("ProfileViewModel", "  - Goal: $goalString")
            Log.d("ProfileViewModel", "  - Gender: $genderString")
            
            // Если есть сохраненные данные, используем их
            if (weight > 0f && height > 0f && age > 0) {
                Log.d("ProfileViewModel", "Используем сохраненные данные из SharedPreferences")
                val goal = when (goalString) {
                    "GAIN" -> NutritionGoal.GAIN
                    "LOSE" -> NutritionGoal.LOSE
                    else -> NutritionGoal.MAINTAIN
                }
                
                val gender = when (genderString) {
                    "FEMALE" -> Gender.FEMALE
                    else -> Gender.MALE
                }
                
                val localProfileData = ProfileData(
                    weight = weight,
                    height = height,
                    age = age,
                    goal = goal,
                    gender = gender
                )
                
                _profileData.value = localProfileData
            } else {
                Log.w("ProfileViewModel", "Нет сохраненных данных в SharedPreferences")
            }
            
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
        val profile = _profileData.value
        sessionManager.saveProfileData(
            weight = profile.weight,
            height = profile.height,
            age = profile.age,
            goal = profile.goal.name,
            gender = profile.gender.name
        )
        
        // Также синхронизируем данные с сервером, если API доступен (включая goal и gender)
        clientAPI?.let { api ->
            viewModelScope.launch {
                try {
                    val result = api.updateProfileData(
                        height = profile.height,
                        bodyweight = profile.weight,
                        age = profile.age,
                        goal = profile.goal.name,
                        gender = profile.gender.name
                    )
                    result.fold(
                        onSuccess = { message ->
                            Log.d("ProfileViewModel", "Данные профиля успешно синхронизированы с сервером: $message")
                        },
                        onFailure = { error ->
                            Log.e("ProfileViewModel", "Ошибка синхронизации данных профиля с сервером: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Исключение при синхронизации данных профиля: ${e.message}", e)
                    val errorMessage = ErrorHandler.handleNetworkException(e)
                    Log.e("ProfileViewModel", "Обработанное сообщение: $errorMessage")
                }
            }
        }
    }
    
    fun getUserEmail(): String? {
        return sessionManager.fetchEmail()
    }
}
