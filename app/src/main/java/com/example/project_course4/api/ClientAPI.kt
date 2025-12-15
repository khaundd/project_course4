package com.example.project_course4.api

import android.util.Log
import com.example.project_course4.Product
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClientAPI {

    private val URL: String = "https://loftily-adequate-urchin.cloudpub.ru/products"
    private val client = HttpClient(CIO) {
        install(ContentNegotiation.Plugin) {
            json()
        }
    }
    suspend fun getProducts(): List<Product> {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.get(URL)
                val products = response.body<List<Product>>()
                products
            } catch (e: Exception) {
                Log.e("api_test", "Ошибка в getProducts: ${e.message}", e)
                emptyList()
            }
        }
    }
}