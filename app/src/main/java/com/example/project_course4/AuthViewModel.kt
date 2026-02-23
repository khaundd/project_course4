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
                        // Сохраняем email при успешной регистрации
                        sessionManager.saveEmail(email)
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
            Log.d("AuthViewModel", "Начало очистки данных с сервера")
            val result = clientAPI.clearMealsFromServer()
            result.fold(
                onSuccess = { message ->
                    Log.d("AuthViewModel", "Данные на сервере успешно очищены: $message")
                    Result.success(message)
                },
                onFailure = { error -> 
                    Log.e("AuthViewModel", "Ошибка очистки данных с сервера: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Исключение при очистке данных с сервера: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun syncMealsToServer(): Result<String> {
        return try {
            Log.d("AuthViewModel", "Начало синхронизации данных на сервер")
            val meals = database.mealDao().getAllMeals()
            val components = database.mealDao().getAllMealComponentsWithJunction()
            
            Log.d("AuthViewModel", "Найдено ${meals.size} приёмов пищи и ${components.size} компонентов для синхронизации")
            
            if (meals.isEmpty()) {
                Log.d("AuthViewModel", "Нет данных для синхронизации")
                Result.success("Нет данных для синхронизации")
            } else {
                val result = clientAPI.syncMealsToServer(meals, components)
                result.fold(
                    onSuccess = { message ->
                        Log.d("AuthViewModel", "Синхронизация успешна: $message")
                        // После успешной синхронизации удаляем локальные данные
                        Log.d("AuthViewModel", "Удаление локальных данных после синхронизации")
                        database.mealDao().fullResetMeals()
                        Result.success(message)
                    },
                    onFailure = { error -> Result.failure(error) }
                )
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Ошибка при синхронизации данных на сервер: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun loadMealsFromServer(): Result<String> {
        return try {
            Log.d("AuthViewModel", "Начало загрузки данных с сервера")
            
            // Сначала очищаем локальную БД перед загрузкой новых данных
            Log.d("AuthViewModel", "Очистка локальной БД перед загрузкой данных с сервера")
            database.mealDao().fullResetMeals()
            Log.d("AuthViewModel", "Локальная БД очищена перед загрузкой данных с сервера")
            
            // Очищаем UI состояние, отправляя событие об обновлении
            _dataUpdateEvent.emit(Unit)
            Log.d("AuthViewModel", "Отправлено событие очистки UI состояния")
            
            val result = clientAPI.loadMealsFromServer()
            result.fold(
                onSuccess = { mealsData ->
                    Log.d("AuthViewModel", "Получено ${mealsData.size} приёмов пищи с сервера")
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
                    Log.d("AuthViewModel", "Данные загружены в БД, обновляем UI")
                    _dataUpdateEvent.emit(Unit) // Уведомляем об обновлении
                    Result.success("Данные успешно загружены")
                },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Ошибка при загрузке данных с сервера: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun logout(
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Начало процесса выхода из аккаунта")
                
                // 1. Сначала очищаем данные на сервере
                Log.d("AuthViewModel", "Шаг 1: Очистка данных на сервере")
                val clearResult = clearMealsFromServer()
                clearResult.fold(
                    onSuccess = { clearMessage ->
                        Log.d("AuthViewModel", "Очистка на сервере успешна: $clearMessage")
                        
                        // 2. Затем синхронизируем локальные данные на сервер
                        Log.d("AuthViewModel", "Шаг 2: Синхронизация локальных данных на сервер")
                        val syncResult = syncMealsToServer()
                        syncResult.fold(
                            onSuccess = { syncMessage ->
                                Log.d("AuthViewModel", "Синхронизация успешна: $syncMessage")
                                
                                // 3. Очищаем локальную БД только после успешной синхронизации
                                Log.d("AuthViewModel", "Шаг 3: Очистка локальной БД")
                                database.mealDao().fullResetMeals()
                                Log.d("AuthViewModel", "Локальная БД очищена после синхронизации")
                                
                                // 4. Очищаем UI состояние перед выходом
                                _dataUpdateEvent.emit(Unit)
                                Log.d("AuthViewModel", "Отправлено событие очистки UI состояния при выходе")
                                
                                // 5. Выполняем обычный выход
                                Log.d("AuthViewModel", "Шаг 5: Выполнение выхода из системы")
                                val logoutResult = clientAPI.logout()
                                logoutResult.fold(
                                    onSuccess = { message ->
                                        sessionManager.clearData()
                                        sessionManager.clearEmail() // Очищаем email при выходе
                                        Log.d("AuthViewModel", "Выход выполнен успешно: $message")
                                        onSuccess(message)
                                    },
                                    onFailure = { error ->
                                        // При ошибке logout не очищаем токен, так как данные уже синхронизированы
                                        Log.e("AuthViewModel", "Ошибка при выходе: ${error.message}")
                                        onError("Сервер недоступен, повторите попытку позднее")
                                    }
                                )
                            },
                            onFailure = { error ->
                                Log.e("AuthViewModel", "Ошибка синхронизации: ${error.message}")
                                // При ошибке синхронизации прерываем процесс и не очищаем локальные данные
                                onError("Сервер недоступен, повторите попытку позднее")
                            }
                        )
                    },
                    onFailure = { error ->
                        Log.e("AuthViewModel", "Ошибка очистки сервера: ${error.message}")
                        // При ошибке очистки сервера прерываем процесс
                        onError("Сервер недоступен, повторите попытку позднее")
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Исключение при выходе: ${e.message}", e)
                // При исключении не очищаем БД и токен
                onError("Сервер недоступен, повторите попытку позднее")
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
            Log.d("AuthViewModel", "Начало входа в аккаунт: email=$email")
            val result = clientAPI.login(email, password)
            result.fold(
                onSuccess = { token ->
                    Log.d("AuthViewModel", "Вход успешен, сохранение токена")
                    sessionManager.saveAuthToken(token)
                    // Сохраняем email при успешном входе
                    sessionManager.saveEmail(email)
                    
                    // Загружаем данные с сервера после успешного входа
                    Log.d("AuthViewModel", "Начало загрузки данных с сервера после входа")
                    val loadResult = loadMealsFromServer()
                    loadResult.fold(
                        onSuccess = { message ->
                            Log.d("AuthViewModel", "Данные успешно загружены после входа: $message")
                            onSuccess("Вход выполнен и данные загружены")
                        },
                        onFailure = { error ->
                            Log.e("AuthViewModel", "Ошибка загрузки данных после входа: ${error.message}")
                            // Даже если загрузка данных не удалась, вход считается успешным
                            onSuccess("Вход выполнен (ошибка загрузки данных: ${error.message})")
                        }
                    )
                },
                onFailure = { error -> 
                    Log.e("AuthViewModel", "Ошибка входа: ${error.message}")
                    onError(error.message ?: "Ошибка") 
                }
            )
        }
    }
}