package com.example.project_course4

import android.util.Log
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.local_db.dao.MealDao
import com.example.project_course4.local_db.dao.ProductsDao
import com.example.project_course4.local_db.entities.MealEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ProductRepository(
    private val productDao: ProductsDao,
    private val clientAPI: ClientAPI,
    private val sessionManager: SessionManager,
    private val mealDao: MealDao
) {
    // наблюдаем за базой данных и конвертируем Entity в UI-модели
    fun getProductsFlow(): Flow<List<Product>> {
        val userId = sessionManager.fetchUserId()
        return productDao.getUserProducts(userId).map { list ->
            list.map { it.toUiModel() }
        }
    }

    suspend fun fetchInitialProducts() {
        try {
            val currentUserId = sessionManager.fetchUserId()
            val response = clientAPI.getProducts(limit = 50)
            val entities = response.map { it.toEntity(isSavedLocally = true, currentUserId = currentUserId) }
            productDao.insertProducts(entities)
        } catch (e: Exception) {
            Log.e("Repository", "Ошибка синхронизации продуктов", e)
        }
    }

    suspend fun saveFullMeal(mealEntity: MealEntity, components: List<Pair<Int, UShort>>): Pair<Int, List<Int>> {
        return mealDao.insertFullMeal(mealEntity, components)
    }

    suspend fun deleteMealFromDb(mealId: Int) {
        Log.d("Repositorys", "Удаление приема пищи из БД по ID: $mealId")
        mealDao.deleteMealById(mealId)
    }

    suspend fun deleteProductFromMeal(junctionId: Int) {
        Log.d("Repositorys", "Вызов MealDao для удаления JunctionId: $junctionId")
        mealDao.deleteJunctionById(junctionId)
    }

    suspend fun addComponentsToExistingMeal(mealId: Int, components: List<Pair<Int, UShort>>) : List<Int> {
        return mealDao.addComponentsToMeal(mealId, components)
    }

    suspend fun updateProductWeight(junctionId: Int, newWeight: UShort) {
        mealDao.updateWeight(junctionId, newWeight)
    }

    suspend fun updateMealTime(mealId: Int, newTime: LocalTime) {
        val timeInMillis = newTime.atDate(LocalDate.now())
            .atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        mealDao.updateMealTime(mealId, timeInMillis)
    }

    suspend fun loadMealsByDate(date: LocalDate): Pair<List<Meal>, List<SelectedProduct>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val mealEntities = mealDao.getMealsByDate(startOfDay)
        val components = mealDao.getAllMealComponentsWithJunction()
        
        if (mealEntities.isEmpty()) {
            return emptyList<Meal>() to emptyList()
        }
        
        val productIds = components.map { it.productId }.distinct()
        val productsMap = if (productIds.isEmpty()) emptyMap()
        else productDao.getProductsByIds(productIds).associateBy { it.productId }

        val meals = mealEntities.map { entity ->
            val time = Instant.ofEpochMilli(entity.mealTime)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
            Meal(id = entity.mealId, time = time, name = entity.name)
        }

        val selectedProducts = components.mapNotNull { comp ->
            val productEntity = productsMap[comp.productId] ?: return@mapNotNull null
            SelectedProduct(
                product = productEntity.toUiModel(),
                weight = comp.weight,
                mealId = comp.mealId,
                junctionId = comp.junctionId
            )
        }
        return meals to selectedProducts
    }

    suspend fun getCaloriesForDate(date: LocalDate): Int {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val mealEntities = mealDao.getMealsByDate(startOfDay)
        val components = mealDao.getAllMealComponentsWithJunction()
        
        if (mealEntities.isEmpty()) {
            return 0
        }
        
        val mealIds = mealEntities.map { it.mealId }.toSet()
        val productIds = components.map { it.productId }.distinct()
        val productsMap = if (productIds.isEmpty()) emptyMap()
        else productDao.getProductsByIds(productIds).associateBy { it.productId }

        return components
            .filter { it.mealId in mealIds }
            .mapNotNull { comp ->
                val productEntity = productsMap[comp.productId] ?: return@mapNotNull null
                val product = productEntity.toUiModel()
                (product.calories * comp.weight / 100).toInt()
            }
            .sum()
    }

    /**
     * Загружает все приёмы пищи и их компоненты из локальной БД.
     * Возвращает пару: список приёмов [Meal] и список выбранных продуктов [SelectedProduct].
     */
    suspend fun loadMealsFromDb(): Pair<List<Meal>, List<SelectedProduct>> {
        val mealEntities = mealDao.getAllMeals()
        val components = mealDao.getAllMealComponentsWithJunction()
        if (mealEntities.isEmpty()) {
            return emptyList<Meal>() to emptyList()
        }
        val productIds = components.map { it.productId }.distinct()
        val productsMap = if (productIds.isEmpty()) emptyMap()
        else productDao.getProductsByIds(productIds).associateBy { it.productId }

        val meals = mealEntities.map { entity ->
            val time = Instant.ofEpochMilli(entity.mealTime)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
            Meal(id = entity.mealId, time = time, name = entity.name)
        }

        val selectedProducts = components.mapNotNull { comp ->
            val productEntity = productsMap[comp.productId] ?: return@mapNotNull null
            SelectedProduct(
                product = productEntity.toUiModel(),
                weight = comp.weight,
                mealId = comp.mealId,
                junctionId = comp.junctionId
            )
        }
        return meals to selectedProducts
    }
}