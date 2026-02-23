package com.example.project_course4

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuthToken(token: String) {
        sharedPreferences.edit { putString("auth_token", token) }
    }

    fun fetchAuthToken(): String? = sharedPreferences.getString("auth_token", null)

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
}