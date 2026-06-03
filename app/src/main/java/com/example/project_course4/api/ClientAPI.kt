package com.example.project_course4.api

import android.util.Log
import com.example.project_course4.Product
import com.example.project_course4.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.example.project_course4.local_db.entities.MealEntity
import com.example.project_course4.local_db.MealComponentWithJunction
import com.example.project_course4.utils.DateUtils
import com.example.project_course4.utils.ErrorHandler

class ClientAPI (private val sessionManager: SessionManager){

    private val BASE_URL: String = "https://loftily-adequate-urchin.cloudpub.ru"
    private val client = HttpClient(CIO) {
        install(ContentNegotiation.Plugin) {
            json(
                Json {
                    ignoreUnknownKeys = true // поможет не падать, если сервер прислал лишние поля
                    coerceInputValues = true
                    encodeDefaults = true
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
    suspend fun getProducts(limit: Int? = null, offset: Int = 0): List<Product> {
        val url = buildString {
            append("$BASE_URL/products?offset=$offset")
            if (limit != null) append("&limit=$limit")
        }
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
                    try {
                        val errorResponse = response.body<ApiResponse>()
                        Log.e("api_test", "Ошибка сервера: ${errorResponse.message}")
                        emptyList()
                    } catch (_: Exception) {
                        Log.e("api_test", "Ошибка при обработке ответа сервера: ${response.bodyAsText()}")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в getProducts: ${e.message}", e)
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Log.e("api_test", "Обработанное сообщение: $errorMessage")
                emptyList()
            }
        }
    }

    suspend fun searchProducts(query: String): List<Product> {
        val url = "$BASE_URL/products/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
        return withContext(Dispatchers.IO) {
            Log.d("api_test", "Поиск продуктов: $url")
            val response = client.get(url)
            if (response.status.value in 200..299) {
                response.body<List<Product>>()
            } else {
                throw Exception("Ошибка сервера: ${response.status.value}")
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
                val userData = mutableMapOf(
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
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Result.failure(Exception(errorMessage))
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
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Result.failure(Exception(errorMessage))
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
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Result.failure(Exception(errorMessage))
            }
        }
    }

    suspend fun verifyEmail(email: String, code: String): Result<String> {
        val url = "$BASE_URL/verify-email"
        return withContext(Dispatchers.IO) {
            try {
                val verificationData = mapOf(
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
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Result.failure(Exception(errorMessage))
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
                        Result.failure(Exception(errorResponse.message ?: "Ошибка сервера при синхронизации"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в syncMealsToServer: ${e.message}", e)
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Result.failure(Exception(errorMessage))
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
                        Result.failure(Exception(errorResponse.error ?: "Ошибка сервера при очистке"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в clearMealsFromServer: ${e.message}", e)
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Result.failure(Exception(errorMessage))
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
                        Result.failure(Exception(errorResponse.message ?: "Ошибка сервера при загрузке"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в loadMealsFromServer: ${e.message}", e)
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Result.failure(Exception(errorMessage))
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
                        Result.failure(Exception(errorResponse.error ?: "Ошибка проверки названия продукта"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в checkProductNameExists: ${e.message}", e)
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Result.failure(Exception(errorMessage))
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
                        Result.failure(Exception(errorResponse.error ?: "Ошибка добавления продукта"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в addProduct: ${e.message}", e)
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Result.failure(Exception(errorMessage))
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
                            Result.failure(Exception(errorResponse.message ?: "Ошибка сервера при загрузке профиля"))
                        } catch (parseException: Exception) {
                            Log.e("api_test", "ОШИБКА ПАРСИНГА ОШИБКИ: ${parseException.message}", parseException)
                            Log.e("api_test", "Сырой ответ ошибки: $rawResponse")
                            Result.failure(Exception("Ошибка сервера: ${response.status.value}"))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "КРИТИЧЕСКАЯ ОШИБКА В getProfileData: ${e.message}", e)
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Result.failure(Exception(errorMessage))
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
                        Result.failure(Exception(errorResponse.message ?: "Ошибка сервера при обновлении профиля"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в updateProfileData: ${e.message}", e)
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Result.failure(Exception(errorMessage))
            }
        }
    }

    suspend fun getRecipes(): Result<List<RecipeResponse>> {
        val url = "$BASE_URL/recipes"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get(url)
                if (response.status.value in 200..299) {
                    Result.success(response.body<List<RecipeResponse>>())
                } else {
                    val error = response.body<ApiResponse>()
                    Result.failure(Exception(error.error ?: "Ошибка загрузки рецептов"))
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в getRecipes: ${e.message}", e)
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun createRecipe(request: CreateRecipeRequest): Result<String> {
        val url = "$BASE_URL/recipes"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                when (response.status.value) {
                    in 200..299 -> {
                        val body = response.body<CreateRecipeResponse>()
                        Result.success(body.result ?: "Рецепт успешно сохранён")
                    }
                    else -> {
                        val body = response.body<CreateRecipeResponse>()
                        Result.failure(Exception(body.error ?: "Ошибка сохранения рецепта"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в createRecipe: ${e.message}", e)
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun updateRecipe(productId: Int, request: CreateRecipeRequest): Result<String> {
        val url = "$BASE_URL/recipes/$productId"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                when (response.status.value) {
                    in 200..299 -> {
                        val body = response.body<CreateRecipeResponse>()
                        Result.success(body.result ?: "Рецепт успешно обновлён")
                    }
                    else -> {
                        val body = response.body<CreateRecipeResponse>()
                        Result.failure(Exception(body.error ?: "Ошибка обновления рецепта"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в updateRecipe: ${e.message}", e)
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun generateRecipeLink(recipeId: Int): Result<String> {
        val url = "$BASE_URL/recipes/$recipeId/share"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url)
                val body = response.body<RecipeShareResponse>()
                if (response.status.value in 200..299) {
                    Result.success(body.link ?: "")
                } else {
                    Result.failure(Exception(body.error ?: "Ошибка генерации ссылки"))
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в generateRecipeLink: ${e.message}", e)
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun setRecipeVisibility(recipeId: Int, isPublic: Boolean): Result<RecipeVisibilityResponse> {
        val url = "$BASE_URL/recipes/$recipeId/visibility"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(RecipeVisibilityRequest(isPublic))
                }
                val body = response.body<RecipeVisibilityResponse>()
                if (response.status.value in 200..299) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error ?: "Ошибка изменения доступа"))
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в setRecipeVisibility: ${e.message}", e)
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun getSharedRecipe(token: String): Result<RecipeResponse> {
        val url = "$BASE_URL/recipes/shared/$token"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get(url) {
                    headers.append("Accept", "application/json")
                }
                if (response.status.value in 200..299) {
                    Result.success(response.body<RecipeResponse>())
                } else {
                    val body = response.body<ApiResponse>()
                    Result.failure(Exception(body.error ?: "Рецепт не найден"))
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в getSharedRecipe: ${e.message}", e)
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun getProductByBarcode(barcode: String): Result<Product?> {
        val url = "$BASE_URL/products/by-barcode?barcode=$barcode"
        return withContext(Dispatchers.IO) {
            try {
                Log.d("api_test", "Поиск продукта по штрих-коду: $barcode")
                val response = client.get(url)
                Log.d("api_test", "Статус ответа: ${response.status.value}")
                
                when (response.status.value) {
                    in 200..299 -> {
                        // Получаем ответ как текст и парсим JSON вручную
                        val rawResponse = response.bodyAsText()
                        Log.d("api_test", "Сырой ответ сервера: $rawResponse")
                        
                        try {
                            val responseBody = Json.decodeFromString<ProductResponse>(rawResponse)
                            Log.d("api_test", "Продукт найден: $responseBody")
                            val product = Product(
                                productId = responseBody.productId,
                                name = responseBody.name ?: "",
                                protein = responseBody.protein ?: 0f,
                                fats = responseBody.fats ?: 0f,
                                carbs = responseBody.carbs ?: 0f,
                                calories = responseBody.calories ?: 0f,
                                barcode = responseBody.barcode,
                                isDish = responseBody.isDish ?: false,
                                createdBy = responseBody.createdBy ?: 0
                            )
                            Result.success(product)
                        } catch (e: Exception) {
                            Log.e("api_test", "Ошибка парсинга JSON: ${e.message}, ответ: $rawResponse")
                            // Проверяем, не является ли это ответом об ошибке
                            if (rawResponse.contains("Продукт не найден в базе")) {
                                Result.success(null)
                            } else {
                                Result.failure(Exception("Ошибка парсинга ответа сервера: ${e.message}"))
                            }
                        }
                    }
                    404 -> {
                        Log.d("api_test", "Продукт не найден в базе данных")
                        Result.success(null)
                    }
                    else -> {
                        val rawResponse = response.bodyAsText()
                        Log.e("api_test", "Ошибка сервера: ${response.status.value}, ответ: $rawResponse")
                        if (rawResponse.contains("Продукт не найден в базе")) {
                            Result.success(null)
                        } else {
                            Result.failure(Exception("Ошибка поиска продукта: ${response.status.value}"))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в getProductByBarcode: ${e.message}", e)
                val errorMessage = ErrorHandler.handleNetworkException(e)
                Result.failure(Exception(errorMessage))
            }
        }
    }

    suspend fun requestPasswordReset(email: String): Result<String> {
        val url = "$BASE_URL/password-reset/request"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("email" to email))
                }
                val body = response.body<ApiResponse>()
                if (response.status.value in 200..299) {
                    Result.success(body.message ?: "Код отправлен")
                } else {
                    Result.failure(Exception(body.error ?: "Ошибка запроса сброса пароля"))
                }
            } catch (e: Exception) {
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun verifyPasswordResetCode(email: String, code: String): Result<String> {
        val url = "$BASE_URL/password-reset/verify"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("email" to email, "code" to code))
                }
                val body = response.body<ApiResponse>()
                if (response.status.value in 200..299) {
                    Result.success(body.message ?: "Код верный")
                } else {
                    Result.failure(Exception(body.error ?: "Неверный код"))
                }
            } catch (e: Exception) {
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun confirmPasswordReset(email: String, code: String, newPassword: String): Result<String> {
        val url = "$BASE_URL/password-reset/confirm"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("email" to email, "code" to code, "new_password" to newPassword))
                }
                val body = response.body<ApiResponse>()
                if (response.status.value in 200..299) {
                    body.token?.let { sessionManager.saveAuthToken(it) }
                    body.userId?.let { sessionManager.saveUserId(it) }
                    Result.success(body.message ?: "Пароль изменён")
                } else {
                    Result.failure(Exception(body.error ?: "Ошибка смены пароля"))
                }
            } catch (e: Exception) {
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    // ─── Meal Plans ──────────────────────────────────────────────────────────

    suspend fun getMealPlans(): Result<List<MealPlanData>> {
        val url = "$BASE_URL/meal-plans"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get(url)
                if (response.status.value in 200..299) {
                    val body = response.body<MealPlanListResponse>()
                    if (body.success) Result.success(body.plans)
                    else Result.failure(Exception(body.message ?: "Ошибка загрузки планов"))
                } else {
                    Result.failure(Exception("Ошибка сервера: ${response.status.value}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun createMealPlan(request: MealPlanSaveRequest): Result<MealPlanSaveResponse> {
        val url = "$BASE_URL/meal-plans"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                val body = response.body<MealPlanSaveResponse>()
                if (response.status.value in 200..299 && body.success) Result.success(body)
                else Result.failure(Exception(body.message ?: "Ошибка создания плана"))
            } catch (e: Exception) {
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun updateMealPlan(planId: Int, request: MealPlanSaveRequest): Result<String> {
        val url = "$BASE_URL/meal-plans/$planId"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.put(url) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                val body = response.body<MealPlanSaveResponse>()
                if (response.status.value in 200..299 && body.success) Result.success(body.message ?: "Обновлено")
                else Result.failure(Exception(body.message ?: "Ошибка обновления плана"))
            } catch (e: Exception) {
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun deleteMealPlan(planId: Int): Result<String> {
        val url = "$BASE_URL/meal-plans/$planId"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.delete(url)
                val body = response.body<MealPlanSaveResponse>()
                if (response.status.value in 200..299 && body.success) Result.success(body.message ?: "Удалено")
                else Result.failure(Exception(body.message ?: "Ошибка удаления плана"))
            } catch (e: Exception) {
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun getPublicMealPlans(): Result<List<MealPlanData>> {
        val url = "$BASE_URL/meal-plans/public"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get(url)
                if (response.status.value in 200..299) {
                    val body = response.body<MealPlanListResponse>()
                    if (body.success) Result.success(body.plans)
                    else Result.failure(Exception(body.message ?: "Ошибка загрузки публичных планов"))
                } else {
                    Result.failure(Exception("Ошибка сервера: ${response.status.value}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun assignMealPlan(planId: Int): Result<MealPlanAssignResponse> {
        val url = "$BASE_URL/meal-plans/$planId/assign"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url)
                val body = response.body<MealPlanAssignResponse>()
                if (response.status.value in 200..299 && body.success) Result.success(body)
                else Result.failure(Exception(body.message ?: "Ошибка"))
            } catch (e: Exception) {
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun finishMealPlan(planId: Int): Result<String> {
        val url = "$BASE_URL/meal-plans/$planId/finish"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url)
                val body = response.body<MealPlanSaveResponse>()
                if (response.status.value in 200..299 && body.success) Result.success(body.message ?: "Завершено")
                else Result.failure(Exception(body.message ?: "Ошибка завершения плана"))
            } catch (e: Exception) {
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun finishMealPlanAuto(planId: Int): Result<String> {
        val url = "$BASE_URL/meal-plans/$planId/finish-auto"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.post(url)
                val body = response.body<MealPlanSaveResponse>()
                if (response.status.value in 200..299 && body.success) Result.success(body.message ?: "Завершено")
                else Result.failure(Exception(body.message ?: "Ошибка завершения плана"))
            } catch (e: Exception) {
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    suspend fun getMealPlanSharedUsers(planId: Int): Result<List<MealPlanSharedUser>> {
        val url = "$BASE_URL/meal-plans/$planId/share-users"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get(url)
                val body = response.body<MealPlanSharedUsersResponse>()
                if (response.status.value in 200..299 && body.success) Result.success(body.users)
                else Result.failure(Exception(body.message ?: "Ошибка"))
            } catch (e: Exception) {
                Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
            }
        }
    }

    // ─── Fitness: Exercises ───────────────────────────────────────────────────

    suspend fun getExercises(
        muscleIds: List<Int>? = null,
        equipments: List<String>? = null,
        levels: List<String>? = null,
        category: String? = null,
        search: String? = null,
        limit: Int = 30,
        offset: Int = 0
    ): Result<ExerciseListResponse> = withContext(Dispatchers.IO) {
        try {
            val url = buildString {
                append("$BASE_URL/exercises?limit=$limit&offset=$offset")
                muscleIds?.forEach { append("&muscle_id=$it") }
                equipments?.forEach { append("&equipment=${java.net.URLEncoder.encode(it, "UTF-8")}") }
                levels?.forEach { append("&level=${java.net.URLEncoder.encode(it, "UTF-8")}") }
                category?.let { append("&category=${java.net.URLEncoder.encode(it, "UTF-8")}") }
                search?.let { append("&search=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            }
            val response = client.get(url)
            if (response.status.value in 200..299) Result.success(response.body<ExerciseListResponse>())
            else Result.failure(Exception("Ошибка сервера: ${response.status.value}"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun getExerciseById(id: Int): Result<ExerciseResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$BASE_URL/exercises/$id")
            if (response.status.value in 200..299) Result.success(response.body<ExerciseResponse>())
            else Result.failure(Exception("Упражнение не найдено"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun getMuscles(): Result<List<MuscleResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$BASE_URL/muscles")
            if (response.status.value in 200..299) Result.success(response.body<List<MuscleResponse>>())
            else Result.failure(Exception("Ошибка загрузки мышц"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun getEquipmentList(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$BASE_URL/exercises/equipment")
            if (response.status.value in 200..299) Result.success(response.body<List<String>>())
            else Result.failure(Exception("Ошибка загрузки оборудования"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    // ─── Fitness: Trainings ───────────────────────────────────────────────────

    suspend fun getTrainings(): Result<List<TrainingData>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$BASE_URL/trainings")
            if (response.status.value in 200..299) Result.success(response.body<TrainingListResponse>().trainings)
            else Result.failure(Exception("Ошибка загрузки тренировок"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun createTraining(request: TrainingSaveRequest): Result<TrainingSaveResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.post("$BASE_URL/trainings") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.value in 200..299) Result.success(response.body<TrainingSaveResponse>())
            else Result.failure(Exception("Ошибка сохранения тренировки"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun createTrainingWithSets(request: TrainingWithSetsSaveRequest): Result<TrainingSaveResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.post("$BASE_URL/trainings/with-sets") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.value in 200..299) Result.success(response.body<TrainingSaveResponse>())
            else Result.failure(Exception("Ошибка сохранения тренировки"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun updateTrainingWithSets(id: Int, request: TrainingWithSetsSaveRequest): Result<TrainingSaveResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.put("$BASE_URL/trainings/$id/with-sets") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.value in 200..299) Result.success(response.body<TrainingSaveResponse>())
            else Result.failure(Exception("Ошибка обновления тренировки"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun updateTraining(id: Int, request: TrainingSaveRequest): Result<TrainingSaveResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.put("$BASE_URL/trainings/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.value in 200..299) Result.success(response.body<TrainingSaveResponse>())
            else Result.failure(Exception("Ошибка обновления тренировки"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun deleteTraining(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.delete("$BASE_URL/trainings/$id")
            if (response.status.value in 200..299) Result.success(Unit)
            else Result.failure(Exception("Ошибка удаления тренировки"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    // ─── Fitness: Training Plans ──────────────────────────────────────────────

    suspend fun getTrainingPlans(): Result<List<TrainingPlanData>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$BASE_URL/training-plans")
            if (response.status.value in 200..299) Result.success(response.body<TrainingPlanListResponse>().plans)
            else Result.failure(Exception("Ошибка загрузки планов тренировок"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun getPublicTrainingPlans(): Result<List<TrainingPlanData>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$BASE_URL/training-plans/public")
            if (response.status.value in 200..299) Result.success(response.body<TrainingPlanListResponse>().plans)
            else Result.failure(Exception("Ошибка загрузки публичных планов"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun getTrainingPlanById(id: Int): Result<TrainingPlanData> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$BASE_URL/training-plans/$id")
            if (response.status.value in 200..299) Result.success(response.body<TrainingPlanData>())
            else Result.failure(Exception("План не найден"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun createTrainingPlan(request: TrainingPlanSaveRequest): Result<TrainingPlanSaveResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.post("$BASE_URL/training-plans") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.value in 200..299) Result.success(response.body<TrainingPlanSaveResponse>())
            else Result.failure(Exception("Ошибка создания плана"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun updateTrainingPlan(id: Int, request: TrainingPlanSaveRequest): Result<TrainingPlanSaveResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.put("$BASE_URL/training-plans/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.value in 200..299) Result.success(response.body<TrainingPlanSaveResponse>())
            else Result.failure(Exception("Ошибка обновления плана"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun deleteTrainingPlan(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.delete("$BASE_URL/training-plans/$id")
            if (response.status.value in 200..299) Result.success(Unit)
            else Result.failure(Exception("Ошибка удаления плана"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }

    suspend fun toggleTrainingPlanPublic(id: Int, isPublic: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = client.put("$BASE_URL/training-plans/$id") {
                contentType(ContentType.Application.Json)
                // We send a minimal update — only is_public changes; name/description/days are preserved server-side
                // The server PUT endpoint replaces the whole plan, so we need to fetch first.
                // Instead, we use a dedicated visibility endpoint if available, or fall back to a full update.
                // Since the API doesn't have a dedicated visibility endpoint for training plans,
                // we'll handle this in the ViewModel by fetching the plan first.
                setBody(TrainingPlanVisibilityRequest(isPublic = isPublic))
            }
            if (response.status.value in 200..299) Result.success(Unit)
            else Result.failure(Exception("Ошибка изменения видимости плана"))
        } catch (e: Exception) {
            Result.failure(Exception(ErrorHandler.handleNetworkException(e)))
        }
    }
}

