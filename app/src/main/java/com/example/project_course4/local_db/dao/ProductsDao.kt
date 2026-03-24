package com.example.project_course4.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.project_course4.local_db.entities.Products
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProducts(products: List<Products>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProducts(products: Products)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: Products)

    @Query("SELECT * FROM products WHERE barcode = :barcode AND (createdBy = :userId OR isSavedLocally = 1) LIMIT 1")
    suspend fun getProductByBarcode(barcode: String, userId: Int): Products?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND (createdBy = :userId OR isSavedLocally = 1)")
    suspend fun getAllUserProductsByBarcode(barcode: String, userId: Int): List<Products>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: Long): Products?

    @Query("SELECT * FROM products WHERE createdBy = :userId OR isSavedLocally = 1")
    fun getUserProducts(userId: Int): Flow<List<Products>>

    @Query("SELECT * FROM products WHERE productId IN (:ids)")
    suspend fun getProductsByIds(ids: List<Int>): List<Products>

    // Пагинация: загрузить страницу продуктов (не пользовательских)
    @Query("SELECT * FROM products WHERE isSavedLocally = 1 ORDER BY productId ASC LIMIT :limit OFFSET :offset")
    suspend fun getProductsPaged(limit: Int, offset: Int): List<Products>

    // Недавно использованные продукты (по времени последнего использования)
    @Query("SELECT * FROM products WHERE lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT :limit")
    suspend fun getRecentlyUsedProducts(limit: Int): List<Products>

    // Обновить время последнего использования
    @Query("UPDATE products SET lastUsedAt = :timestamp WHERE productId = :productId")
    suspend fun updateLastUsedAt(productId: Int, timestamp: Long)

    // Поиск по имени среди локальных продуктов
    @Query("SELECT * FROM products WHERE productName LIKE '%' || :query || '%'")
    suspend fun searchLocalProducts(query: String): List<Products>

    // Поиск среди пользовательских продуктов и всех локально сохранённых
    @Query("SELECT * FROM products WHERE productName LIKE '%' || :query || '%' AND (createdBy = :userId OR isSavedLocally = 1)")
    suspend fun searchUserProducts(query: String, userId: Int): List<Products>
}