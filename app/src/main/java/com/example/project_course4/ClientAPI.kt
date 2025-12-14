package com.example.project_course4

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClientAPI {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
    suspend fun getProducts(): List<Product> {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get("http://10.0.2.2:5000/products")
                val products = response.body<List<Product>>()
                products
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в getProducts: ${e.message}", e)
                emptyList()
            }
        }
    }
}