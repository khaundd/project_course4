package com.example.project_course4.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Проверяет реальную доступность интернета через ping
     * @param timeout таймаут в миллисекундах
     * @return true если интернет доступен, иначе false
     */
    fun isInternetReachable(timeout: Int = 3000): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("8.8.8.8", 53), timeout)
            socket.close()
            true
        } catch (e: IOException) {
            // Пробуем альтернативный DNS сервер
            try {
                val socket2 = Socket()
                socket2.connect(InetSocketAddress("1.1.1.1", 53), timeout)
                socket2.close()
                true
            } catch (e2: IOException) {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Проверяет доступность интернета через HTTP запрос
     * @param timeout таймаут в миллисекундах
     * @return true если интернет доступен, иначе false
     */
    suspend fun isInternetAvailableHttp(timeout: Int = 3000): Boolean {
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(timeout.toLong()) {
                try {
                    val url = URL("https://www.google.com")
                    val connection = url.openConnection()
                    connection.connectTimeout = 2000
                    connection.readTimeout = 1000
                    connection.getInputStream()
                    true
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }
    }
    
    /**
     * Комплексная проверка доступности интернета
     * @param context контекст приложения
     * @param timeout таймаут для ping проверки
     * @return true если интернет доступен, иначе false
     */
    fun isInternetAvailable(context: Context, timeout: Int = 3000): Boolean {
        // Для UI проверки используем только базовую проверку доступности сети
        // Это позволит избежать ложных срабатываний когда интернет есть
        return isNetworkAvailable(context)
    }
    
    /**
     * Быстрая проверка интернета с меньшим таймаутом
     * @param context контекст приложения
     * @return true если интернет доступен, иначе false
     */
    fun isInternetAvailableFast(context: Context): Boolean {
        return isNetworkAvailable(context) && isInternetReachable(1000)
    }
    
    /**
     * Определяет, является ли исключение сетевой ошибкой
     * @param exception исключение для проверки
     * @return true если это сетевая ошибка, иначе false
     */
    fun isNetworkError(exception: Throwable): Boolean {
        return when (exception) {
            is java.io.IOException -> true
            is java.net.SocketTimeoutException -> true
            is java.net.UnknownHostException -> true
            is java.net.ConnectException -> true
            is java.net.NoRouteToHostException -> true
            is java.net.PortUnreachableException -> true
            is javax.net.ssl.SSLException -> true
            else -> exception.cause?.let { isNetworkError(it) } ?: false
        }
    }
}
