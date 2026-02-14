package com.example.project_course4

import android.util.Log
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.local_db.dao.ProductsDao
import com.example.project_course4.local_db.entities.Products
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val productDao: ProductsDao,
    private val clientAPI: ClientAPI,
    private val sessionManager: SessionManager
) {
    // Наблюдаем за базой данных и конвертируем Entity в UI-модели
    fun getProductsFlow(): Flow<List<Product>> {
        val userId = sessionManager.fetchUserId()
        return productDao.getUserProducts(userId).map { list ->
            list.map { it.toUiModel() }
        }
    }

    suspend fun fetchInitialProducts() {
        try {
            val currentUserId = sessionManager.fetchUserId()
            // Предполагается, что вы обновили ClientAPI для поддержки limit
            val response = clientAPI.getProducts(limit = 50)
            val entities = response.map { it.toEntity(isSavedLocally = true, currentUserId = currentUserId) }
            productDao.insertProducts(entities)
        } catch (e: Exception) {
            Log.e("Repository", "Ошибка синхронизации продуктов", e)
        }
    }
}