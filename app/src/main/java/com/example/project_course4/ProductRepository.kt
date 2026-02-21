package com.example.project_course4

import android.util.Log
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.local_db.dao.MealDao
import com.example.project_course4.local_db.dao.ProductsDao
import com.example.project_course4.local_db.entities.MealEntity
import com.example.project_course4.local_db.entities.Products
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
        // newTime - это LocalTime, нужно конвертировать в смещение от начала дня в миллисекундах
        val timeInMillis = newTime.toNanoOfDay() / 1_000_000
        mealDao.updateMealTime(mealId, timeInMillis)
    }

    suspend fun loadMealsByDate(date: LocalDate): Pair<List<Meal>, List<SelectedProduct>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        Log.d("ProductRepository", "Загрузка приемов пищи для даты: $date, startOfDay=$startOfDay")
        val mealEntities = mealDao.getMealsByDate(startOfDay)
        val components = mealDao.getAllMealComponentsWithJunction()
        Log.d("ProductRepository", "Найдено в БД: ${mealEntities.size} приемов пищи, ${components.size} компонентов ВСЕГО")
        Log.d("ProductRepository", "Компоненты: ${components.map { "mealId=${it.mealId}, productId=${it.productId}, weight=${it.weight}" }}")
        
        if (mealEntities.isEmpty()) {
            Log.d("ProductRepository", "Приемы пищи не найдены для даты $date")
            return emptyList<Meal>() to emptyList()
        }
        
        Log.d("ProductRepository", "Найдено ${mealEntities.size} приемов пищи для даты $date")
        
        val productIds = components.map { it.productId }.distinct()
        val productsMap = if (productIds.isEmpty()) emptyMap()
        else productDao.getProductsByIds(productIds).associateBy { it.productId }

        val meals = mealEntities.map { entity ->
            // entity.mealTime - это смещение от начала дня в миллисекундах
            // entity.mealDate - это начало дня в миллисекундах
            val fullDateTime = entity.mealDate + entity.mealTime
            val time = Instant.ofEpochMilli(fullDateTime)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
            Meal(id = entity.mealId, time = time, name = entity.name)
        }

        val selectedProducts = components.mapNotNull { comp ->
            val productEntity = productsMap[comp.productId] ?: return@mapNotNull null
            SelectedProduct(
                product = productEntity.toUiModel(),
                weight = comp.weight.toInt(),
                mealId = comp.mealId,
                junctionId = comp.junctionId
            )
        }
        Log.d("ProductRepository", "Итого компонентов для возврата: ${selectedProducts.size}")
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
                (product.calories * comp.weight.toInt() / 100).toInt()
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
            // entity.mealTime - это смещение от начала дня в миллисекундах
            // entity.mealDate - это начало дня в миллисекундах
            val fullDateTime = entity.mealDate + entity.mealTime
            val time = Instant.ofEpochMilli(fullDateTime)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
            Meal(id = entity.mealId, time = time, name = entity.name)
        }

        val selectedProducts = components.mapNotNull { comp ->
            val productEntity = productsMap[comp.productId] ?: return@mapNotNull null
            SelectedProduct(
                product = productEntity.toUiModel(),
                weight = comp.weight.toInt(),
                mealId = comp.mealId,
                junctionId = comp.junctionId
            )
        }
        return meals to selectedProducts
    }

    suspend fun checkProductNameExists(name: String): Result<Boolean> {
        return try {
            val result = clientAPI.checkProductNameExists(name)
            result.fold(
                onSuccess = { exists -> Result.success(exists) },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Log.e("ProductRepository", "Ошибка проверки названия продукта: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun addProductToServerAndLocal(product: Product): Result<Product> {
        return try {
            // Сначала добавляем на сервер
            val createRequest = com.example.project_course4.api.ProductCreateRequest(
                name = product.name,
                protein = product.protein,
                fats = product.fats,
                carbs = product.carbs,
                calories = product.calories,
                barcode = product.barcode ?: ""
            )
            
            val serverResult = clientAPI.addProduct(createRequest)
            serverResult.fold(
                onSuccess = { serverProduct ->
                    Log.d("ProductRepository", "Продукт с сервера: name='${serverProduct.name}', protein=${serverProduct.protein}")
                    // Сохраняем в локальную БД
                    val currentUserId = sessionManager.fetchUserId()
                    val entity = serverProduct.toEntity(isSavedLocally = true, currentUserId = currentUserId)
                    Log.d("ProductRepository", "Entity для БД: productName='${entity.productName}', protein=${entity.protein}")
                    productDao.insertProducts(entity)
                    Log.d("ProductRepository", "Продукт успешно добавлен на сервер и в локальную БД")
                    Result.success(serverProduct)
                },
                onFailure = { error ->
                    Log.e("ProductRepository", "Ошибка добавления продукта на сервер: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e("ProductRepository", "Ошибка добавления продукта: ${e.message}", e)
            Result.failure(e)
        }
    }
}
