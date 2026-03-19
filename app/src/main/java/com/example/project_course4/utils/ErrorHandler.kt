package com.example.project_course4.utils

import android.util.Log

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
    
}
