package com.example.project_course4

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit
import android.util.Log

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("SessionManager", "Ошибка при создании EncryptedSharedPreferences: ${e.message}")
        // Удаляем поврежденные данные и создаем новые
        context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveAuthToken(token: String) {
        Log.d("SessionManager", "Сохранение токена: ${if (token.length > 10) token.take(10) + "..." else token}")
        sharedPreferences.edit { putString("auth_token", token) }
        Log.d("SessionManager", "Токен сохранен")
    }

    fun fetchAuthToken(): String? {
        val token = sharedPreferences.getString("auth_token", null)
        Log.d("SessionManager", "Получение токена: ${if (token != null && token.length > 10) token.take(10) + "..." else token}")
        return token
    }

    fun clearData() {
        sharedPreferences.edit { clear() }
    }

    fun saveUserId(userId: Int) {
        sharedPreferences.edit { putInt("user_id", userId) }
    }

    fun fetchUserId(): Int = sharedPreferences.getInt("user_id", -1)

    fun saveEmail(email: String) {
        sharedPreferences.edit { putString("user_email", email) }
    }

    fun fetchEmail(): String? = sharedPreferences.getString("user_email", null)

    fun clearEmail() {
        sharedPreferences.edit { remove("user_email") }
    }

    // Методы для сохранения данных профиля
    fun saveProfileData(weight: Float, height: Float, age: Int, goal: String, gender: String) {
        Log.d("SessionManager", "=== СОХРАНЕНИЕ ДАННЫХ ПРОФИЛЯ ===")
        Log.d("SessionManager", "  - Weight: $weight")
        Log.d("SessionManager", "  - Height: $height")
        Log.d("SessionManager", "  - Age: $age")
        Log.d("SessionManager", "  - Goal: $goal")
        Log.d("SessionManager", "  - Gender: $gender")
        
        sharedPreferences.edit {
            putFloat("profile_weight", weight)
            putFloat("profile_height", height)
            putInt("profile_age", age)
            putString("profile_goal", goal)
            putString("profile_gender", gender)
        }
        
        Log.d("SessionManager", "Данные профиля сохранены в SharedPreferences")
    }

    fun getProfileWeight(): Float {
        val weight = sharedPreferences.getFloat("profile_weight", 0f)
        Log.d("SessionManager", "Загрузка weight: $weight")
        return weight
    }
    
    fun getProfileHeight(): Float {
        val height = sharedPreferences.getFloat("profile_height", 0f)
        Log.d("SessionManager", "Загрузка height: $height")
        return height
    }
    
    fun getProfileAge(): Int {
        val age = sharedPreferences.getInt("profile_age", 0)
        Log.d("SessionManager", "Загрузка age: $age")
        return age
    }
    
    fun getProfileGoal(): String? {
        val goal = sharedPreferences.getString("profile_goal", null)
        Log.d("SessionManager", "Загрузка goal: $goal")
        return goal
    }
    
    fun getProfileGender(): String? {
        val gender = sharedPreferences.getString("profile_gender", null)
        Log.d("SessionManager", "Загрузка gender: $gender")
        return gender
    }

    fun clearProfileData() {
        sharedPreferences.edit {
            remove("profile_weight")
            remove("profile_height")
            remove("profile_age")
            remove("profile_goal")
            remove("profile_gender")
        }
    }
}