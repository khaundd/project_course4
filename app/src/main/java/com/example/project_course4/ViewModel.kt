package com.example.project_course4

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_course4.api.ClientAPI
import kotlinx.coroutines.launch

class ViewModel : ViewModel() {
    private val clientAPI = ClientAPI()
    
    // Функция для регистрации пользователя
    fun register(
        username: String,
        password: String,
        email: String,
        height: Float,
        bodyweight: Float,
        age: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = clientAPI.register(username, password, email, height, bodyweight, age)
                result.fold(
                    onSuccess = { message ->
                        onSuccess(message)
                    },
                    onFailure = { error ->
                        onError(error.message ?: "Неизвестная ошибка при регистрации")
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Неизвестная ошибка при регистрации")
            }
        }
    }
    
    // Функция для авторизации пользователя
    fun login(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("ViewModel", "Попытка входа для: $email")
                val result = clientAPI.login(email, password)
                result.fold(
                    onSuccess = { message ->
                        Log.d("ViewModel", "Вход успешен: $message")
                        onSuccess(message)
                    },
                    onFailure = { error ->
                        Log.d("ViewModel", "Ошибка входа: ${'$'}{error.message}")
                        onError(error.message ?: "Неизвестная ошибка при авторизации")
                    }
                )
            } catch (e: Exception) {
                Log.e("ViewModel", "Исключение при входе: ${'$'}{e.message}", e)
                onError(e.message ?: "Неизвестная ошибка при авторизации")
            }
        }
    }
    
    // Функция для подтверждения email
    fun verifyEmail(
        email: String,
        code: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("ViewModel", "Попытка подтверждения email: $email, код: $code")
                val result = clientAPI.verifyEmail(email, code)
                result.fold(
                    onSuccess = { message ->
                        Log.d("ViewModel", "Подтверждение email успешно: $message")
                        onSuccess(message)
                    },
                    onFailure = { error ->
                        Log.d("ViewModel", "Ошибка подтверждения email: ${'$'}{error.message}")
                        onError(error.message ?: "Неизвестная ошибка при подтверждении email")
                    }
                )
            } catch (e: Exception) {
                Log.e("ViewModel", "Исключение при подтверждении email: ${'$'}{e.message}", e)
                onError(e.message ?: "Неизвестная ошибка при подтверждении email")
            }
        }
    }
}