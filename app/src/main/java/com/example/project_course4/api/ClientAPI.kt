package com.example.project_course4.api

import android.util.Log
import com.example.project_course4.Product
import com.example.project_course4.SessionManager
import com.example.project_course4.api.ProductCreateRequest
import com.example.project_course4.api.ProductCreateResponse
import com.example.project_course4.api.ProductResponse
import com.example.project_course4.api.ProfileData
import com.example.project_course4.api.ProfileResponse
import com.example.project_course4.api.ProfileUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.example.project_course4.local_db.entities.MealEntity
import com.example.project_course4.local_db.entities.MealComponent
import com.example.project_course4.local_db.dao.MealDao
import com.example.project_course4.local_db.MealComponentWithJunction
import com.example.project_course4.utils.DateUtils

class ClientAPI (private val sessionManager: SessionManager){

    private val BASE_URL: String = "https://loftily-adequate-urchin.cloudpub.ru"
    private val client = HttpClient(CIO) {
        install(ContentNegotiation.Plugin) {
            json(
                Json {
                    ignoreUnknownKeys = true // поможет не падать, если сервер прислал лишние поля
                    coerceInputValues = true
                }
            )
        }

        // Этот блок автоматически добавляет заголовок к каждому запросу
        defaultRequest {
            val token = sessionManager.fetchAuthToken()
            if (!token.isNullOrEmpty()) {
                headers.append("Authorization", "Bearer $token")
                Log.d("api_test", "Добавлен заголовок Authorization: Bearer [TOKEN]")
            } else {
                Log.w("api_test", "Токен отсутствует, заголовок Authorization не добавлен")
            }
        }
    }
    suspend fun getProducts(limit: Int? = null): List<Product> {
        val url = if (limit != null) "$BASE_URL/products?limit=$limit" else "$BASE_URL/products"
        return withContext(Dispatchers.IO) {
            try {
                Log.d("api_test", "Запрос продуктов: $url")
                val response = client.get(url)
                Log.d("api_test", "Статус ответа: ${response.status.value}")
                
                if (response.status.value in 200..299) {
                    val products = response.body<List<Product>>()
                    Log.d("api_test", "Получено ${products.size} продуктов")
                    products
                } else {
                    // Для ошибок пытаемся прочитать как ApiResponse
                    try {
                        val errorResponse = response.body<ApiResponse>()
                        Log.e("api_test", "Ошибка сервера: ${errorResponse?.message}")
                        emptyList()
                    } catch (e: Exception) {
                        Log.e("api_test", "Ошибка при обработке ответа сервера: ${response.bodyAsText()}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в getProducts: ${e.message}", e)
                emptyList()
            }
        }
    }

    suspend fun register(
        username: String,
        password: String,
        email: String,
        height: Float,
        bodyweight: Float,
        age: Int,
        goal: String? = null,
        gender: String? = null
    ): Result<String> {
        val url = "$BASE_URL/register"
        return withContext(Dispatchers.IO) {
            try {
                val userData = mutableMapOf<String, String>(
                    "username" to username,
                    "password" to password,
                    "email" to email,
                    "height" to height.toString(),
                    "bodyweight" to bodyweight.toString(),
                    "age" to age.toString()
                )
                
                // Добавляем goal и gender если они предоставлены
                goal?.let { userData["goal"] = it }
                gender?.let { userData["gender"] = it }
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(userData)
                }
                
                when (response.status.value) {
                    in 200..299 -> {
                        val responseBody = response.body<ApiResponse>()
                        Log.d("api_test", "Полный ответ сервера при регистрации: $responseBody")
                        Log.d("api_test", "Токен в ответе: ${responseBody.token}")
                        Log.d("api_test", "UserId в ответе: ${responseBody.userId}")
                        
                        // Если сервер вернул токен при регистрации, сохраняем его
                        if (responseBody.token != null) {
                            val userId = responseBody.userId ?: -1
                            if (userId != -1) {
                                sessionManager.saveUserId(userId)
                            }
                            sessionManager.saveAuthToken(responseBody.token)
                            Log.d("api_test", "Токен сохранен после успешной регистрации: ${responseBody.token}")
                        } else {
                            Log.w("api_test", "Сервер не вернул токен при регистрации")
                        }
                        
                        Result.success(responseBody.message ?: "Регистрация почти завершена. Проверьте email для подтверждения.")
                    }
                    else -> {
                        val errorResponse = response.body<ApiResponse>()
                        Result.failure(Exception(errorResponse.error ?: "Неизвестная ошибка при регистрации"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в register: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun login(email: String, password: String): Result<String> {
        val url = "$BASE_URL/login"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("email" to email, "password" to password))
                }
                val rawResponse = response.bodyAsText()
                Log.d("api_test", "Ответ сервера: $rawResponse")
                val body = response.body<ApiResponse>()
                if (response.status.value in 200..299 && body.token != null) {
                    Log.d("api_test", "body: $body")
                    val userId = body.userId ?: -1

                    if (userId != -1) {
                        sessionManager.saveUserId(userId)
                        Result.success(body.token)// Возвращаем токен в случае успеха
                    } else {
                        Result.failure(Exception("Сервер не вернул ID пользователя"))
                    }
                } else {
                    Result.failure(Exception(body.error ?: "Ошибка входа"))
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в login: ${e.message}", e)
                Result.failure(Exception("Сервер недоступен, повторите попытку позднее"))
            }
        }
    }

    suspend fun logout(): Result<String> {
        val url = "$BASE_URL/logout"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                }
                // Независимо от ответа сервера, очищаем локальный токен
                sessionManager.clearData()

                if (response.status.value in 200..299) {
                    Result.success("Выход выполнен успешно")
                } else {
                    Result.failure(Exception("Сервер вернул ошибку при выходе"))
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в logout: ${e.message}", e)
                sessionManager.clearData()
                Result.failure(e)
            }
        }
    }

    suspend fun verifyEmail(email: String, code: String): Result<String> {
        val url = "$BASE_URL/verify-email"
        return withContext(Dispatchers.IO) {
            try {
                val verificationData = mapOf<String, String>(
                    "email" to email,
                    "code" to code
                )
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(verificationData)
                }
                
                when (response.status.value) {
                    in 200..299 -> {
                        val responseBody = response.body<ApiResponse>()
                        Log.d("api_test", "Полный ответ сервера при верификации: $responseBody")
                        Log.d("api_test", "Токен в ответе: ${responseBody.token}")
                        Log.d("api_test", "UserId в ответе: ${responseBody.userId}")
                        
                        // Если сервер вернул токен после подтверждения email, сохраняем его
                        if (responseBody.token != null) {
                            val userId = responseBody.userId ?: -1
                            if (userId != -1) {
                                sessionManager.saveUserId(userId)
                            }
                            sessionManager.saveAuthToken(responseBody.token)
                            Log.d("api_test", "Токен сохранен после подтверждения email: ${responseBody.token}")
                        } else {
                            Log.w("api_test", "Сервер не вернул токен при верификации email")
                        }
                        
                        Result.success(responseBody.message ?: "Email успешно подтвержден. Регистрация завершена.")
                    }
                    else -> {
                        val errorResponse = response.body<ApiResponse>()
                        Log.d("api_test", "Ошибка подтверждения email: ${'$'}{errorResponse.error}")
                        Result.failure(Exception(errorResponse.error ?: "Неизвестная ошибка при подтверждении email"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в verifyEmail: ${'$'}{e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun syncMealsToServer(meals: List<MealEntity>, mealComponents: List<MealComponentWithJunction>): Result<String> {
        val url = "$BASE_URL/meals/sync"
        return withContext(Dispatchers.IO) {
            try {
                val mealsData = meals.map { meal ->
                    val components = mealComponents
                        .filter { it.mealId == meal.mealId }
                        .map { component ->
                            MealComponentData(
                                productId = component.productId,
                                weight = component.weight.toInt()
                            )
                        }
                    
                    MealData(
                        name = meal.name,
                        mealTime = DateUtils.combineDateTimeForServer(meal.mealTime, meal.mealDate),
                        components = components
                    )
                }
                
                val syncRequest = MealSyncRequest(mealsData)
                
                Log.d("api_test", "Отправляемые данные на сервер: $syncRequest")
                
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(syncRequest)
                }
                
                Log.d("api_test", "Статус ответа: ${response.status.value}")
                
                when (response.status.value) {
                    in 200..299 -> {
                        val responseBody = response.body<MealSyncResponse>()
                        Log.d("api_test", "Ответ сервера: $responseBody")
                        if (responseBody.success) {
                            Result.success(responseBody.message ?: "Синхронизация успешна")
                        } else {
                            Result.failure(Exception(responseBody.message ?: "Ошибка синхронизации"))
                        }
                    }
                    else -> {
                        val errorResponse = response.body<MealSyncResponse>()
                        Log.e("api_test", "Ошибка сервера: ${response.status.value}, ответ: $errorResponse")
                        Result.failure(Exception(errorResponse?.message ?: "Ошибка сервера при синхронизации"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в syncMealsToServer: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun clearMealsFromServer(): Result<String> {
        val url = "$BASE_URL/meals/clear"
        return withContext(Dispatchers.IO) {
            try {
                Log.d("api_test", "Очистка данных на сервере...")
                
                val response = client.delete(url)
                
                Log.d("api_test", "Статус ответа при очистке: ${response.status.value}")
                
                when (response.status.value) {
                    in 200..299 -> {
                        val responseBody = response.body<ApiResponse>()
                        Log.d("api_test", "Ответ сервера при очистке: $responseBody")
                        Result.success(responseBody.message ?: "Данные успешно очищены")
                    }
                    else -> {
                        val errorResponse = response.body<ApiResponse>()
                        Log.e("api_test", "Ошибка сервера при очистке: ${response.status.value}, ответ: $errorResponse")
                        Result.failure(Exception(errorResponse?.error ?: "Ошибка сервера при очистке"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в clearMealsFromServer: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun loadMealsFromServer(): Result<List<MealData>> {
        val url = "$BASE_URL/meals"
        return withContext(Dispatchers.IO) {
            try {
                Log.d("api_test", "Загрузка приемов пищи с сервера...")
                
                val response = client.get(url)
                
                Log.d("api_test", "Статус ответа при загрузке: ${response.status.value}")
                
                when (response.status.value) {
                    in 200..299 -> {
                        val responseBody = response.body<MealLoadResponse>()
                        Log.d("api_test", "Ответ сервера при загрузке: $responseBody")
                        if (responseBody.success && responseBody.meals != null) {
                            Result.success(responseBody.meals)
                        } else {
                            Result.failure(Exception(responseBody.message ?: "Ошибка загрузки данных"))
                        }
                    }
                    else -> {
                        val errorResponse = response.body<MealLoadResponse>()
                        Log.e("api_test", "Ошибка сервера при загрузке: ${response.status.value}, ответ: $errorResponse")
                        Result.failure(Exception(errorResponse?.message ?: "Ошибка сервера при загрузке"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в loadMealsFromServer: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun checkProductNameExists(name: String): Result<Boolean> {
        val url = "$BASE_URL/products/check-name"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("name" to name))
                }
                
                when (response.status.value) {
                    in 200..299 -> {
                        val responseBody = response.body<NameCheckResponse>()
                        Result.success(responseBody.exists)
                    }
                    else -> {
                        val errorResponse = response.body<ApiResponse>()
                        Result.failure(Exception(errorResponse?.error ?: "Ошибка проверки названия продукта"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в checkProductNameExists: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun addProduct(product: ProductCreateRequest): Result<Product> {
        val url = "$BASE_URL/products"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(product)
                }
                
                when (response.status.value) {
                    in 200..299 -> {
                        val responseBody = response.body<ProductCreateResponse>()
                        Log.d("api_test", "Ответ сервера при добавлении продукта: $responseBody")
                        if (responseBody.product != null) {
                            val productResponse = responseBody.product
                            Log.d("api_test", "ProductResponse от сервера: name=${productResponse.name}, protein=${productResponse.protein}")
                            val convertedProduct = Product(
                                productId = productResponse.productId,
                                name = productResponse.name ?: "",
                                protein = productResponse.protein ?: 0f,
                                fats = productResponse.fats ?: 0f,
                                carbs = productResponse.carbs ?: 0f,
                                calories = productResponse.calories ?: 0f,
                                barcode = productResponse.barcode,
                                isDish = productResponse.isDish ?: false,
                                createdBy = productResponse.createdBy
                            )
                            Log.d("api_test", "ConvertedProduct после создания: name=${convertedProduct.name}, protein=${convertedProduct.protein}")
                            Result.success(convertedProduct)
                        } else {
                            Result.failure(Exception("Продукт не был создан"))
                        }
                    }
                    else -> {
                        val errorResponse = response.body<ApiResponse>()
                        Result.failure(Exception(errorResponse?.error ?: "Ошибка добавления продукта"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в addProduct: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getProfileData(): Result<ProfileData> {
        val url = "$BASE_URL/profile"
        return withContext(Dispatchers.IO) {
            try {
                Log.d("api_test", "=== ЗАПРОС ПРОФИЛЯ НАЧАЛО ===")
                Log.d("api_test", "URL запроса: $url")
                
                val token = sessionManager.fetchAuthToken()
                Log.d("api_test", "Токен авторизации: ${if (token.isNullOrEmpty()) "ОТСУТСТВУЕТ" else "ПРИСУТСТВУЕТ (длина: ${token.length})"}")
                
                val response = client.get(url)
                
                Log.d("api_test", "Статус ответа: ${response.status.value} ${response.status.description}")
                
                // Получаем сырой ответ для отладки
                val rawResponse = response.bodyAsText()
                Log.d("api_test", "СЫРОЙ ОТВЕТ СЕРВЕРА: $rawResponse")
                
                when (response.status.value) {
                    in 200..299 -> {
                        try {
                            val responseBody = response.body<ProfileResponse>()
                            Log.d("api_test", "ОБРАБОТАННЫЙ ОТВЕТ: $responseBody")
                            Log.d("api_test", "Success: ${responseBody.success}")
                            Log.d("api_test", "Message: ${responseBody.message}")
                            Log.d("api_test", "Profile: ${responseBody.profile}")
                            
                            if (responseBody.profile != null) {
                                val profile = responseBody.profile
                                Log.d("api_test", "ДЕТАЛЬНЫЕ ДАННЫЕ ПРОФИЛЯ:")
                                Log.d("api_test", "  - Height: ${profile.height}")
                                Log.d("api_test", "  - Bodyweight: ${profile.bodyweight}")
                                Log.d("api_test", "  - Age: ${profile.age}")
                                Log.d("api_test", "  - Goal: ${profile.goal}")
                                Log.d("api_test", "  - Gender: ${profile.gender}")
                            }
                            
                            if (responseBody.success && responseBody.profile != null) {
                                Log.d("api_test", "=== ЗАПРОС ПРОФИЛЯ УСПЕШНО ЗАВЕРШЕН ===")
                                Result.success(responseBody.profile)
                            } else {
                                Log.e("api_test", "=== ЗАПРОС ПРОФИЛЯ ЗАВЕРШЕН С ОШИБКОЙ ===")
                                Log.e("api_test", "Причина: ${responseBody.message ?: "Неизвестная ошибка"}")
                                Result.failure(Exception(responseBody.message ?: "Ошибка загрузки данных профиля"))
                            }
                        } catch (parseException: Exception) {
                            Log.e("api_test", "ОШИБКА ПАРСИНГА ОТВЕТА: ${parseException.message}", parseException)
                            Log.e("api_test", "Сырой ответ, который не удалось распарсить: $rawResponse")
                            Result.failure(Exception("Ошибка парсинга ответа сервера: ${parseException.message}"))
                        }
                    }
                    else -> {
                        try {
                            val errorResponse = response.body<ProfileResponse>()
                            Log.e("api_test", "ОШИБКА СЕРВЕРА:")
                            Log.e("api_test", "  - Статус: ${response.status.value}")
                            Log.e("api_test", "  - Success: ${errorResponse.success}")
                            Log.e("api_test", "  - Message: ${errorResponse.message}")
                            Log.e("api_test", "  - Profile: ${errorResponse.profile}")
                            Result.failure(Exception(errorResponse?.message ?: "Ошибка сервера при загрузке профиля"))
                        } catch (parseException: Exception) {
                            Log.e("api_test", "ОШИБКА ПАРСИНГА ОШИБКИ: ${parseException.message}", parseException)
                            Log.e("api_test", "Сырой ответ ошибки: $rawResponse")
                            Result.failure(Exception("Ошибка сервера: ${response.status.value}"))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "КРИТИЧЕСКАЯ ОШИБКА В getProfileData: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun updateProfileData(
        height: Float,
        bodyweight: Float,
        age: Int,
        goal: String? = null,
        gender: String? = null
    ): Result<String> {
        val url = "$BASE_URL/profile"
        return withContext(Dispatchers.IO) {
            try {
                Log.d("api_test", "Обновление данных профиля на сервере...")
                
                val profileRequest = ProfileUpdateRequest(
                    height = height,
                    bodyweight = bodyweight,
                    age = age,
                    goal = goal,
                    gender = gender
                )
                
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(profileRequest)
                }
                
                Log.d("api_test", "Статус ответа при обновлении профиля: ${response.status.value}")
                
                when (response.status.value) {
                    in 200..299 -> {
                        val responseBody = response.body<ProfileResponse>()
                        Log.d("api_test", "Ответ сервера при обновлении профиля: $responseBody")
                        if (responseBody.success) {
                            Result.success(responseBody.message ?: "Данные профиля успешно обновлены")
                        } else {
                            Result.failure(Exception(responseBody.message ?: "Ошибка обновления данных профиля"))
                        }
                    }
                    else -> {
                        val errorResponse = response.body<ProfileResponse>()
                        Log.e("api_test", "Ошибка сервера при обновлении профиля: ${response.status.value}, ответ: $errorResponse")
                        Result.failure(Exception(errorResponse?.message ?: "Ошибка сервера при обновлении профиля"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в updateProfileData: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}
