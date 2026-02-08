package com.example.project_course4.api

import android.util.Log
import com.example.project_course4.Product
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class ClientAPI {

    private val BASE_URL: String = "https://loftily-adequate-urchin.cloudpub.ru"
    private val client = HttpClient(CIO) {
        install(ContentNegotiation.Plugin) {
            json()
        }
    }
    
    // Существующая функция получения продуктов
    suspend fun getProducts(): List<Product> {
        val url = "$BASE_URL/products"
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get(url)
                val products = response.body<List<Product>>()
                products
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в getProducts: ${'$'}{e.message}", e)
                emptyList()
            }
        }
    }
    
    // Функция регистрации пользователя
    suspend fun register(
        username: String,
        password: String,
        email: String,
        height: Float,
        bodyweight: Float,
        age: Int
    ): Result<String> {
        val url = "$BASE_URL/register"
        return withContext(Dispatchers.IO) {
            try {
                // Преобразуем все значения в строки для сериализации
                val userData = mapOf<String, String>(
                    "username" to username,
                    "password" to password,
                    "email" to email,
                    "height" to height.toString(),
                    "bodyweight" to bodyweight.toString(),
                    "age" to age.toString()
                )
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(userData)
                }
                
                when (response.status.value) {
                    in 200..299 -> {
                        val responseBody = response.body<ApiResponse>()
                        Result.success(responseBody.message ?: "Регистрация почти завершена. Проверьте email для подтверждения.")
                    }
                    else -> {
                        val errorResponse = response.body<ApiResponse>()
                        Result.failure(Exception(errorResponse.error ?: "Неизвестная ошибка при регистрации"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в register: ${'$'}{e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    // Функция авторизации пользователя
    suspend fun login(email: String, password: String): Result<String> {
        val url = "$BASE_URL/login"
        return withContext(Dispatchers.IO) {
            try {
                val loginData = mapOf(
                    "email" to email,
                    "password" to password
                )
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(loginData)
                }
                
                when (response.status.value) {
                    in 200..299 -> {
                        val responseBody = response.body<ApiResponse>()
                        Log.d("api_test", "Успешный вход: ${'$'}{responseBody.message}")
                        Result.success(responseBody.message ?: "Вход выполнен успешно")
                    }
                    else -> {
                        val errorResponse = response.body<ApiResponse>()
                        Log.d("api_test", "Ошибка авторизации: ${'$'}{errorResponse.error}")
                        Result.failure(Exception(errorResponse.error ?: "Неизвестная ошибка при авторизации"))
                    }
                }
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в login: ${'$'}{e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    // Функция подтверждения email
    suspend fun verifyEmail(email: String, code: String): Result<String> {
        val url = "$BASE_URL/verify-email"
        return withContext(Dispatchers.IO) {
            try {
                // Преобразуем значения в строки для сериализации
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
                        Log.d("api_test", "Успешное подтверждение email: ${'$'}{responseBody.message}")
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
}

// Класс для десериализации ответов API
@Serializable
private data class ApiResponse(
    val message: String? = null,
    val error: String? = null
)