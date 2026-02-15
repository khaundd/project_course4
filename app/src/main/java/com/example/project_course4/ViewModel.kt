package com.example.project_course4

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.project_course4.api.ClientAPI
import kotlinx.coroutines.launch

class ViewModel(
    private val clientAPI: ClientAPI,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    fun logout(
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = clientAPI.logout()
            result.fold(
                onSuccess = { message ->
                    sessionManager.clearData()
                    onSuccess(message)
                },
                onFailure = { error ->
                    sessionManager.clearData()
                    onError(error.message ?: "Ошибка при выходе")
                }
            )
        }
    }
    
    fun logoutAndNavigate(
        navController: NavController,
        onLoggingOut: (Boolean) -> Unit = {}
    ) {
        onLoggingOut(true)
        logout(
            onSuccess = { message ->
                onLoggingOut(false)
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Main.route) { inclusive = true }
                }
            },
            onError = { error ->
                onLoggingOut(false)
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Main.route) { inclusive = true }
                }
            }
        )
    }
    
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
    
    fun login(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = clientAPI.login(email, password)
            result.fold(
                onSuccess = { token ->
                    sessionManager.saveAuthToken(token) // сохраняем полученный токен
                    onSuccess("Вход выполнен")
                },
                onFailure = { error -> onError(error.message ?: "Ошибка") }
            )
        }
    }
    
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