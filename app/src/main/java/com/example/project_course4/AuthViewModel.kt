package com.example.project_course4

import android.util.Log
import androidx.lifecycle.ViewModel as AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.local_db.AppDatabase
import com.example.project_course4.local_db.MealComponentWithJunction
import com.example.project_course4.local_db.entities.MealEntity
import com.example.project_course4.api.MealData
import com.example.project_course4.utils.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val clientAPI: ClientAPI,
    private val sessionManager: SessionManager,
    private val database: AppDatabase
) : AndroidViewModel() {
    
    // Публичный доступ к sessionManager для других ViewModel
    val sessionManagerPublic: SessionManager = sessionManager
    
    // SharedFlow для уведомления об обновлении данных
    private val _dataUpdateEvent = MutableSharedFlow<Unit>()
    val dataUpdateEvent: SharedFlow<Unit> = _dataUpdateEvent
    
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

    private suspend fun clearMealsFromServer(): Result<String> {
        return try {
            val result = clientAPI.clearMealsFromServer()
            result.fold(
                onSuccess = { message ->
                    Log.d("ViewModel", "Данные на сервере успешно очищены: $message")
                    Result.success(message)
                },
                onFailure = { error -> 
                    Log.e("ViewModel", "Ошибка очистки данных с сервера: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e("ViewModel", "Исключение при очистке данных с сервера: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun syncMealsToServer(): Result<String> {
        return try {
            val meals = database.mealDao().getAllMeals()
            val components = database.mealDao().getAllMealComponentsWithJunction()
            
            if (meals.isEmpty()) {
                Result.success("Нет данных для синхронизации")
            } else {
                val result = clientAPI.syncMealsToServer(meals, components)
                result.fold(
                    onSuccess = { message ->
                        // После успешной синхронизации удаляем локальные данные
                        database.mealDao().fullResetMeals()
                        Result.success(message)
                    },
                    onFailure = { error -> Result.failure(error) }
                )
            }
        } catch (e: Exception) {
            Log.e("ViewModel", "Ошибка при синхронизации данных на сервер: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun loadMealsFromServer(): Result<String> {
        return try {
            // Сначала очищаем локальную БД перед загрузкой новых данных
            database.mealDao().fullResetMeals()
            Log.d("ViewModel", "Локальная БД очищена перед загрузкой данных с сервера")
            
            // Очищаем UI состояние, отправляя событие об обновлении
            _dataUpdateEvent.emit(Unit)
            Log.d("ViewModel", "Отправлено событие очистки UI состояния")
            
            val result = clientAPI.loadMealsFromServer()
            result.fold(
                onSuccess = { mealsData ->
                    mealsData.forEach { mealData ->
                        Log.d("AuthViewModel", "Получен прием пищи с сервера: ${mealData.name}, время: ${mealData.mealTime}")
                        
                        // Конвертируем строку DATETIME из UTC в миллисекунды локального времени
                        val fullDateTime = DateUtils.parseDateTimeFromServer(mealData.mealTime)
                        val (mealTime, mealDate) = DateUtils.splitDateTime(fullDateTime)
                        
                        Log.d("AuthViewModel", "После конвертации: fullDateTime=$fullDateTime, mealTime=$mealTime, mealDate=$mealDate")
                        
                        val mealEntity = MealEntity(
                            name = mealData.name,
                            mealTime = mealTime,  // Возвращаем смещение от начала дня
                            mealDate = mealDate
                        )
                        
                        val components = mealData.components.map { component ->
                            Pair(component.productId, component.weight.toUShort())
                        }
                        
                        database.mealDao().insertFullMeal(mealEntity, components)
                    }
                    
                    // После загрузки данных в БД, уведомляем ProductViewModel
                    Log.d("ViewModel", "Данные загружены в БД, обновляем UI")
                    _dataUpdateEvent.emit(Unit) // Уведомляем об обновлении
                    Result.success("Данные успешно загружены")
                },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Log.e("ViewModel", "Ошибка при загрузке данных с сервера: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun logout(
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // 1. Сначала очищаем данные на сервере
                val clearResult = clearMealsFromServer()
                clearResult.fold(
                    onSuccess = { clearMessage ->
                        Log.d("ViewModel", "Очистка на сервере успешна: $clearMessage")
                        
                        // 2. Затем синхронизируем локальные данные на сервер
                        val syncResult = syncMealsToServer()
                        syncResult.fold(
                            onSuccess = { syncMessage ->
                                Log.d("ViewModel", "Синхронизация успешна: $syncMessage")
                                
                                // 3. Очищаем локальную БД
                                database.mealDao().fullResetMeals()
                                Log.d("ViewModel", "Локальная БД очищена после синхронизации")
                            },
                            onFailure = { error ->
                                Log.e("ViewModel", "Ошибка синхронизации: ${error.message}")
                                // При ошибке синхронизации все равно очищаем локальные данные
                                database.mealDao().fullResetMeals()
                                Log.d("ViewModel", "Локальная БД очищена из-за ошибки синхронизации")
                            }
                        )
                    },
                    onFailure = { error ->
                        Log.e("ViewModel", "Ошибка очистки сервера: ${error.message}")
                        // Даже если очистка сервера не удалась, продолжаем с синхронизацией
                        val syncResult = syncMealsToServer()
                        syncResult.fold(
                            onSuccess = { syncMessage ->
                                Log.d("ViewModel", "Синхронизация успешна: $syncMessage")
                                database.mealDao().fullResetMeals()
                            },
                            onFailure = { syncError ->
                                Log.e("ViewModel", "Ошибка синхронизации: ${syncError.message}")
                                database.mealDao().fullResetMeals()
                            }
                        )
                    }
                )
                
                // 4. Очищаем UI состояние перед выходом
                _dataUpdateEvent.emit(Unit)
                Log.d("ViewModel", "Отправлено событие очистки UI состояния при выходе")
                
                // 5. Выполняем обычный выход
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
            } catch (e: Exception) {
                // При исключении также очищаем БД на всякий случай
                try {
                    database.mealDao().fullResetMeals()
                } catch (dbException: Exception) {
                    Log.e("ViewModel", "Ошибка при очистке БД: ${dbException.message}")
                }
                sessionManager.clearData()
                onError(e.message ?: "Ошибка при выходе")
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
                    sessionManager.saveAuthToken(token)
                    
                    // После успешного входа загружаем данные с сервера
                    val loadResult = loadMealsFromServer()
                    loadResult.fold(
                        onSuccess = { loadMessage ->
                            Log.d("ViewModel", "Загрузка данных успешна: $loadMessage")
                            onSuccess("Вход выполнен и данные загружены")
                        },
                        onFailure = { error ->
                            Log.e("ViewModel", "Ошибка загрузки данных: ${error.message}")
                            // Всё равно считаем вход успешным
                            onSuccess("Вход выполнен")
                        }
                    )
                },
                onFailure = { error -> onError(error.message ?: "Ошибка") }
            )
        }
    }
}