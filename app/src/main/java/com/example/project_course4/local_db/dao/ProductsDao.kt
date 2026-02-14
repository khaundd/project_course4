package com.example.project_course4.local_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.project_course4.Product
import com.example.project_course4.local_db.entities.Products
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Products>)

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: Long): Products?

    @Query("SELECT * FROM products WHERE createdBy = :userId OR isSavedLocally = 1")
    fun getUserProducts(userId: Int): Flow<List<Products>>
}