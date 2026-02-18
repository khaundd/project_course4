package com.example.project_course4.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel as AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.project_course4.Meal
import com.example.project_course4.MealNutrition
import com.example.project_course4.Product
import com.example.project_course4.ProductCreationState
import com.example.project_course4.ProductRepository
import com.example.project_course4.SelectedProduct
import com.example.project_course4.AuthViewModel
import com.example.project_course4.local_db.entities.MealEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ProductViewModel(
    private val repository: ProductRepository,
    private val authViewModel: AuthViewModel
) : AndroidViewModel() {
    val products: StateFlow<List<Product>> = repository.getProductsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // состояние для создания нового продукта
    private var _productCreationState = MutableStateFlow(ProductCreationState())
    val productCreationState: StateFlow<ProductCreationState> = _productCreationState.asStateFlow()

    // флаг, показывающий нужно ли показывать экран создания продукта
    private var _shouldShowProductCreation = MutableStateFlow(false)
    val shouldShowProductCreation: StateFlow<Boolean> = _shouldShowProductCreation.asStateFlow()

    // флаг, указывающий, что продукты добавляются из списка
    private var _isAddingFromList = MutableStateFlow(false)
    var isAddingFromList: StateFlow<Boolean> = _isAddingFromList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // текущий временный выбор на экране выбора продуктов
    private var _currentSelection = MutableStateFlow<Set<Product>>(emptySet())
    var currentSelection: StateFlow<Set<Product>> = _currentSelection.asStateFlow()

    // продукты, ожидающие ввода веса
    private var _pendingProducts = MutableStateFlow<List<Product>>(emptyList())
    var pendingProducts: StateFlow<List<Product>> = _pendingProducts.asStateFlow()

    // окончательный выбор на главном экране
    private var _finalSelection = MutableStateFlow<List<SelectedProduct>>(emptyList())
    var finalSelection: StateFlow<List<SelectedProduct>> = _finalSelection.asStateFlow()

    // список приёмов пищи
    private var _meals = MutableStateFlow<List<Meal>>(emptyList())
    var meals: StateFlow<List<Meal>> = _meals.asStateFlow()

    // текущий продукт для ввода веса
    private var _currentProductForWeight = MutableStateFlow<Product?>(null)
    var currentProductForWeight: StateFlow<Product?> = _currentProductForWeight.asStateFlow()

    // флаг, показывающий нужно ли начинать ввод веса
    private var _shouldShowWeightInput = MutableStateFlow(false)
    var shouldShowWeightInput: StateFlow<Boolean> = _shouldShowWeightInput.asStateFlow()

    // идентификатор приёма пищи, в котором происходит редактирование
    private var _editingMealId = MutableStateFlow<Int?>(null)
    var editingMealId: StateFlow<Int?> = _editingMealId.asStateFlow()

    private var tempMealIdCounter = -1

    // Текущая выбранная дата
    private var _selectedDate = MutableStateFlow(LocalDate.now())
    var selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // функция для установки идентификатора приёма пищи
    fun setEditingMealId(mealId: Int?) {
        _editingMealId.value = mealId
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        loadMealsForDate(date)
    }

    init {
        viewModelScope.launch {
            repository.fetchInitialProducts()
            loadMealsForDate(_selectedDate.value)
            
            // Подписываемся на события обновления данных от AuthViewModel
            authViewModel.dataUpdateEvent.collect {
                Log.d("ProductViewModel", "Получено событие обновления данных, перезагружаем")
                loadMealsForDate(_selectedDate.value)
            }
        }
    }

    //загружает приёмы пищи и выбранные продукты из локальной БД (при старте приложения).
    fun loadMealsFromDb() {
        viewModelScope.launch {
            try {
                val (mealsFromDb, selectionFromDb) = repository.loadMealsFromDb()
                if (mealsFromDb.isNotEmpty()) {
                    _meals.value = mealsFromDb.sortedBy { it.time }
                    _finalSelection.value = selectionFromDb
                } else {
                    initializeMeals()
                }
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Ошибка загрузки приёмов из БД: ${e.message}", e)
                initializeMeals()
            }
        }
    }

    fun loadMealsForDate(date: LocalDate) {
        viewModelScope.launch {
            try {
                val (mealsFromDb, selectionFromDb) = repository.loadMealsByDate(date)
                if (mealsFromDb.isNotEmpty()) {
                    _meals.value = mealsFromDb.sortedBy { it.time }
                    _finalSelection.value = selectionFromDb
                } else {
                    initializeMealsForDate(date)
                }
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Ошибка загрузки приёмов за дату $date: ${e.message}", e)
                initializeMealsForDate(date)
            }
        }
    }

    fun updateProductCreationState(newState: ProductCreationState) {
        _productCreationState.value = newState
    }

    fun resetProductCreationState() {
        _productCreationState.value = ProductCreationState()
    }

    fun hideProductCreationScreen() {
        _shouldShowProductCreation.value = false
        resetProductCreationState()
    }

    fun navigateToProductCreation(navController: NavController) {
        resetProductCreationState()
        navController.navigate("productCreation?barcode=")
    }

    fun toggleCurrentSelection(product: Product) {
        val current = _currentSelection.value.toMutableSet()
        if (current.contains(product)) {
            current.remove(product)
        } else {
            current.add(product)
        }
        _currentSelection.value = current
    }

    fun clearCurrentSelection() {
        _currentSelection.value = emptySet()
    }
    
    fun saveCurrentSelection() {
        val selected = _currentSelection.value.toList()
        val mealId = _editingMealId.value ?: "default"
        
        if (selected.isNotEmpty()) {
            // Фильтруем продукты, которых еще нет в приёме пищи
            val productsToAdd = selected.filter { product ->
                _finalSelection.value.none { 
                    it.product == product && it.mealId == mealId 
                }
            }
            
            if (productsToAdd.isNotEmpty()) {
                _pendingProducts.value = productsToAdd
                _shouldShowWeightInput.value = true
                // Устанавливаем флаг, что это добавление из списка
                _isAddingFromList.value = true
            } else {
                // Все выбранные продукты уже есть в приёме пищи
                _shouldShowWeightInput.value = false
            }
        }
        _currentSelection.value = emptySet()
    }

    // добавить выбранные продукты в приём пищи через диалог ввода веса (editingMealId уже установлен).
    fun addSelectionToMealWithWeightInput() {
        val selected = _currentSelection.value.toList()
        if (selected.isEmpty()) return
        
        val mealId = _editingMealId.value ?: "default"
        // фильтруем продукты, которых еще нет в приёме пищи
        val productsToAdd = selected.filter { product ->
            _finalSelection.value.none { 
                it.product == product && it.mealId == mealId 
            }
        }
        
        if (productsToAdd.isNotEmpty()) {
            _pendingProducts.value = productsToAdd
            _shouldShowWeightInput.value = true
            // устанавливаем флаг, что это добавление из списка
            _isAddingFromList.value = true
        } else {
            // все выбранные продукты уже есть в приёме пищи
            _shouldShowWeightInput.value = false
        }
        _currentSelection.value = emptySet()
    }


    fun addProductWithWeight(weight: Int) {
        val currentProduct = _currentProductForWeight.value
        val mealId = _editingMealId.value ?: -1
        
        if (currentProduct != null) {
            val updatedSelection = _finalSelection.value.toMutableList()
            
            // проверяем, есть ли продукт с таким же product и mealId
            val existingProductIndex = updatedSelection.indexOfFirst { 
                it.product == currentProduct && it.mealId == mealId 
            }
            
            if (existingProductIndex != -1) {
                val existing = updatedSelection[existingProductIndex]
                // используем флаг isAddingFromList для определения режима
                if (isAddingFromList.value) {
                    // режим добавления из списка - суммируем вес, сохраняем junctionId
                    updatedSelection[existingProductIndex] = existing.copy(
                        weight = existing.weight + weight
                    )
                } else {
                    // режим редактирования - только меняем вес в существующей записи, junctionId сохраняем
                    updatedSelection[existingProductIndex] = existing.copy(weight = weight)
                }
            } else {
                // если продукт новый, добавляем его
                updatedSelection.add(
                    SelectedProduct(
                        product = currentProduct,
                        weight = weight,
                        mealId = mealId
                    )
                )
            }
            _finalSelection.value = updatedSelection

            val pending = _pendingProducts.value.toMutableList()
            pending.remove(currentProduct)
            _pendingProducts.value = pending

            if (pending.isNotEmpty()) {
                _currentProductForWeight.value = pending.first()
            } else {
                _currentProductForWeight.value = null
                _shouldShowWeightInput.value = false
                _isAddingFromList.value = false
                saveMealToDb(mealId)
            }
        }
    }

    private fun saveMealToDb(mealId: Int) {
        val mealInfo = _meals.value.find { it.id == mealId } ?: return
        val productsInMeal = getProductsForMeal(mealId)

        if (productsInMeal.isNotEmpty()) {
            productsInMeal.map {
                it.product.productId to it.weight.toUShort()
            }
            viewModelScope.launch {
                saveCurrentMeal(mealInfo.id)
            }
        }
    }

    fun skipCurrentProduct() {
        val currentProduct = _currentProductForWeight.value
        if (currentProduct != null) {
            val pending = _pendingProducts.value.toMutableList()
            pending.remove(currentProduct)
            _pendingProducts.value = pending

            if (pending.isNotEmpty()) {
                _currentProductForWeight.value = pending.first()
            } else {
                _currentProductForWeight.value = null
                _shouldShowWeightInput.value = false
            }
        }
    }

    fun checkAndStartWeightInput() {
        val pending = _pendingProducts.value
        if (pending.isNotEmpty() && _currentProductForWeight.value == null) {
            _currentProductForWeight.value = pending.first()
        }
    }

    fun editProductWeightInMeal(product: Product, mealId: Int, currentWeight: Int) {
        _isAddingFromList.value = false
        _currentProductForWeight.value = product
        _pendingProducts.value = listOf(product)
        _shouldShowWeightInput.value = true
        _editingMealId.value = mealId
        checkAndStartWeightInput()
    }

    // удаление продукта
    fun removeProductFromMeal(product: Product, mealId: Int) {
        val selectedToRemove = _finalSelection.value.find {
            it.product.productId == product.productId && it.mealId == mealId
        }

        viewModelScope.launch {
            try {
                // удаление из БД
                selectedToRemove?.junctionId?.let { jId ->
                    repository.deleteProductFromMeal(jId)
                }
                _finalSelection.value = _finalSelection.value.filter { it != selectedToRemove }

                Log.d("DeleteProduct", "Продукт удален, список обновлен")

            } catch (e: Exception) {
                Log.e("DeleteProduct", "Ошибка удаления: ${e.message}")
            }
        }
    }

    fun clearWeightInput() {
        _currentProductForWeight.value = null
        _pendingProducts.value = emptyList()
        _shouldShowWeightInput.value = false
        _editingMealId.value = null
        _isAddingFromList.value = false
    }

    fun initializeMeals() {
        if (_meals.value.isEmpty()) {
            val defaultMeals = listOf(
                Meal(id = tempMealIdCounter--, time = LocalTime.of(6, 0), name = "Завтрак"),
                Meal(id = tempMealIdCounter--, time = LocalTime.of(11, 0), name = "Обед"),
                Meal(id = tempMealIdCounter--, time = LocalTime.of(16, 0), name = "Ужин")
            ).sortedBy { it.time }
            _meals.value = defaultMeals
        }
    }

    fun initializeMealsForDate(date: LocalDate) {
        _meals.value = listOf(
            Meal(id = tempMealIdCounter--, time = LocalTime.of(6, 0), name = "Завтрак"),
            Meal(id = tempMealIdCounter--, time = LocalTime.of(11, 0), name = "Обед"),
            Meal(id = tempMealIdCounter--, time = LocalTime.of(16, 0), name = "Ужин")
        ).sortedBy { it.time }
        _finalSelection.value = emptyList()
    }

    fun addMeal(name: String) {
        val newMeal = Meal(
            id = tempMealIdCounter--, // Временный ID: -1, -2 и т.д.
            time = LocalTime.now(),
            name = name
        )
        _meals.value += newMeal
    }

    fun removeMeal(mealId: Int) {
        Log.d("MealDelete", "Попытка удаления приёма пищи с ID: $mealId")

        viewModelScope.launch {
            try {
                // если ID > 0, значит приём пищи уже есть в БД
                if (mealId > 0) {
                    Log.d("MealDelete", "Удаление из локальной БД по ID: $mealId")
                    repository.deleteMealFromDb(mealId)
                }
                _meals.value = _meals.value.filter { it.id != mealId }
                _finalSelection.value = _finalSelection.value.filter { it.mealId != mealId }

                Log.i("MealDelete", "Приём пищи успешно удален из UI")
            } catch (e: Exception) {
                Log.e("MealDelete", "Ошибка при удалении приёма пищи: ${e.message}")
            }
        }
    }

    fun updateMealTime(mealId: Int, newTime: LocalTime) {
        val updatedMeals = _meals.value.toMutableList()
        val index = updatedMeals.indexOfFirst { it.id == mealId }
        if (index != -1) {
            updatedMeals[index] = updatedMeals[index].copy(time = newTime)
            // сортируем приёмы пищи по времени
            _meals.value = updatedMeals.sortedBy { it.time }
            
            // Обновляем время в БД
            viewModelScope.launch {
                try {
                    repository.updateMealTime(mealId, newTime)
                    Log.d("UpdateMealTime", "Время приёма пищи обновлено в БД: mealId=$mealId, time=$newTime")
                } catch (e: Exception) {
                    Log.e("UpdateMealTime", "Ошибка обновления времени: ${e.message}")
                }
            }
        }
    }

    fun getProductsForMeal(mealId: Int): List<SelectedProduct> {
        return _finalSelection.value.filter { it.mealId == mealId }
    }

    fun getMealNutrition(mealId: Int): MealNutrition {
        val products = getProductsForMeal(mealId)
        val totalCalories = products.sumOf {
            (it.product.calories.toDouble() * it.weight / 100)
        }.toFloat()

        val totalProtein = products.sumOf {
            (it.product.protein.toDouble() * it.weight / 100)
        }.toFloat()

        val totalFats = products.sumOf {
            (it.product.fats.toDouble() * it.weight / 100)
        }.toFloat()

        val totalCarbs = products.sumOf {
            (it.product.carbs.toDouble() * it.weight / 100)
        }.toFloat()
        
        return MealNutrition(totalProtein, totalFats, totalCarbs, totalCalories)
    }
    suspend fun getCaloriesForDate(date: LocalDate): Int {
        return try {
            repository.getCaloriesForDate(date)
        } catch (e: Exception) {
            Log.e("Calories", "Ошибка получения калорий за дату $date: ${e.message}")
            0
        }
    }

    suspend fun saveCurrentMeal(mealIdInUi: Int) {
        try {
            val mealToSave = _meals.value.find { it.id == mealIdInUi } ?: return

            val productsInThisMeal = _finalSelection.value.filter { it.mealId == mealIdInUi }

            val toUpdate = productsInThisMeal.filter { it.junctionId != null }
            val toInsert = productsInThisMeal.filter { it.junctionId == null }

            toUpdate.forEach { selected ->
                selected.junctionId?.let { jId ->
                    repository.updateProductWeight(jId, selected.weight.toUShort())
                    Log.d("SaveMeal", "Обновлен вес для существующего продукта. JunctionId: $jId")
                }
            }

            if (toInsert.isNotEmpty()) {
                val componentsData = toInsert.map {
                    it.product.productId to it.weight.toUShort()
                }

                if (mealIdInUi <= 0) {
                    // если сам прием пищи еще не в БД
                    val currentDate = _selectedDate.value
                    val fullDateTime = mealToSave.time.atDate(currentDate)
                        .atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                    
                    val startOfDay = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val timeOnly = fullDateTime - startOfDay
                    
                    val newMealEntity = MealEntity(
                        name = mealToSave.name,
                        mealTime = timeOnly, // Только время от начала дня
                        mealDate = startOfDay // Начало дня
                    )
                    val (generatedMealId, newJunctionIds) = repository.saveFullMeal(newMealEntity, componentsData)
                    syncStateAfterSave(mealIdInUi, generatedMealId, newJunctionIds)
                } else {
                    // если прием уже в БД, просто добавляем в него новые компоненты
                    val newJunctionIds = repository.addComponentsToExistingMeal(mealIdInUi, componentsData)
                    syncStateAfterSave(mealIdInUi, mealIdInUi, newJunctionIds)
                }
            }

            Log.i("SaveMeal", "Синхронизация завершена успешно")
        } catch (e: Exception) {
            Log.e("SaveMeal", "Ошибка сохранения: ${e.message}")
        }
    }

    private fun syncStateAfterSave(oldMealId: Int, realMealId: Int, newJunctionIds: List<Int>) {
        var junctionIdx = 0

        _meals.value = _meals.value.map {
            if (it.id == oldMealId) it.copy(id = realMealId) else it
        }

        _finalSelection.value = _finalSelection.value.map { selected ->
            if (selected.mealId == oldMealId) {
                if (selected.junctionId == null) {
                    val updated = selected.copy(
                        mealId = realMealId,
                        junctionId = newJunctionIds.getOrNull(junctionIdx)
                    )
                    junctionIdx++
                    updated
                } else {
                    selected.copy(mealId = realMealId)
                }
            } else {
                selected
            }
        }
    }
}