package com.example.project_course4.utils

import android.util.Log
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

/**
 * Централизованный обработчик ошибок сервера
 */
object ErrorHandler {
    
    /**
     * Обрабатывает исключения при сетевых запросах и возвращает понятное сообщение для пользователя
     */
    fun handleNetworkException(throwable: Throwable): String {
        Log.e("ErrorHandler", "Обработка сетевой ошибки: ${throwable.message}", throwable)
        
        val errorMessage = throwable.message ?: ""
        
        return when {
            // Проверка на 502 Bad Gateway
            errorMessage.contains("502 Bad Gateway", ignoreCase = true) -> {
                "Сервер недоступен, повторите попытку позднее"
            }
            
            // Проверка на 503 Service Unavailable
            errorMessage.contains("503 Service Unavailable", ignoreCase = true) -> {
                "Сервер недоступен, повторите попытку позднее"
            }
            
            // Проверка на таймауты
            errorMessage.contains("timeout", ignoreCase = true) ||
            errorMessage.contains("Timeout", ignoreCase = true) -> {
                "Превышено время ожидания ответа сервера"
            }
            
            // Проверка на отсутствие подключения к интернету
            errorMessage.contains("UnknownHost", ignoreCase = true) ||
            errorMessage.contains("Network is unreachable", ignoreCase = true) ||
            errorMessage.contains("ConnectException", ignoreCase = true) -> {
                "Отсутствует подключение к интернету"
            }
            
            // Проверка на ошибки парсинга (когда сервер возвращает HTML вместо JSON)
            errorMessage.contains("ByteBufferChannel", ignoreCase = true) ||
            errorMessage.contains("Expected response body", ignoreCase = true) ||
            errorMessage.contains("text/html", ignoreCase = true) -> {
                "Сервер недоступен, повторите попытку позднее"
            }
            
            // Общие ошибки соединения
            errorMessage.contains("SocketException", ignoreCase = true) ||
            errorMessage.contains("SSLException", ignoreCase = true) -> {
                "Ошибка соединения с сервером"
            }
            
            else -> {
                "Произошла ошибка при запросе к серверу"
            }
        }
    }
    
    /**
     * Обрабатывает HTTP коды ошибок
     */
    fun handleHttpError(statusCode: Int, responseBody: String? = null): String {
        Log.e("ErrorHandler", "Обработка HTTP ошибки: $statusCode, тело: $responseBody")
        
        return when (statusCode) {
            400 -> "Неверный формат запроса"
            401 -> "Требуется авторизация"
            403 -> "Доступ запрещен"
            404 -> "Запрашиваемый ресурс не найден"
            429 -> "Слишком много запросов, попробуйте позже"
            500 -> "Внутренняя ошибка сервера"
            502 -> "Сервер недоступен, повторите попытку позднее"
            503 -> "Сервер недоступен, повторите попытку позднее"
            504 -> "Шлюз не отвечает, повторите попытку позднее"
            else -> "Ошибка сервера: $statusCode"
        }
    }
    
    /**
     * Проверяет, является ли ошибка ошибкой недоступности сервера
     */
    fun isServerUnavailableError(throwable: Throwable): Boolean {
        val errorMessage = throwable.message ?: ""
        return errorMessage.contains("502 Bad Gateway", ignoreCase = true) ||
               errorMessage.contains("503 Service Unavailable", ignoreCase = true) ||
               errorMessage.contains("ByteBufferChannel", ignoreCase = true) ||
               errorMessage.contains("Expected response body", ignoreCase = true) ||
               errorMessage.contains("text/html", ignoreCase = true)
    }
}
