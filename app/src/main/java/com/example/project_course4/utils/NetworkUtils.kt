package com.example.project_course4.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.IOException

object NetworkUtils {
    
    /**
     * Проверяет наличие активного интернет-соединения
     * @param context контекст приложения
     * @return true если есть интернет-соединение, иначе false
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            capabilities?.let {
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } ?: false
        } catch (_: Exception) {
            false
        }
    }
    
    /**
     * Комплексная проверка доступности интернета
     * @param context контекст приложения
     * @return true если интернет доступен, иначе false
     */
    fun isInternetAvailable(context: Context): Boolean {
        return isNetworkAvailable(context)
    }
    
    /**
     * Определяет, является ли исключение сетевой ошибкой
     * @param exception исключение для проверки
     * @return true если это сетевая ошибка, иначе false
     */
    fun isNetworkError(exception: Throwable): Boolean {
        return when (exception) {
            is IOException -> true
            else -> exception.cause?.let { isNetworkError(it) } ?: false
        }
    }
}
