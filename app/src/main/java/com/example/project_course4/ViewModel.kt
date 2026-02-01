package com.example.project_course4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.Meal
import com.example.project_course4.MealNutrition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.UUID

class ProductViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Текущий временный выбор на экране выбора продуктов
    private var _currentSelection = MutableStateFlow<Set<Product>>(emptySet())
    var currentSelection: StateFlow<Set<Product>> = _currentSelection.asStateFlow()

    // Продукты, ожидающие ввода веса
    private var _pendingProducts = MutableStateFlow<List<Product>>(emptyList())
    var pendingProducts: StateFlow<List<Product>> = _pendingProducts.asStateFlow()

    // Окончательный выбор на главном экране
    private var _finalSelection = MutableStateFlow<List<SelectedProduct>>(emptyList())
    var finalSelection: StateFlow<List<SelectedProduct>> = _finalSelection.asStateFlow()

    // Список приёмов пищи
    private var _meals = MutableStateFlow<List<Meal>>(emptyList())
    var meals: StateFlow<List<Meal>> = _meals.asStateFlow()

    // Текущий продукт для ввода веса
    private var _currentProductForWeight = MutableStateFlow<Product?>(null)
    var currentProductForWeight: StateFlow<Product?> = _currentProductForWeight.asStateFlow()

    // Флаг, показывающий нужно ли начинать ввод веса
    private var _shouldShowWeightInput = MutableStateFlow(false)
    var shouldShowWeightInput: StateFlow<Boolean> = _shouldShowWeightInput.asStateFlow()

    // Идентификатор приёма пищи, в котором происходит редактирование
    private var _editingMealId = MutableStateFlow<String?>(null)
    var editingMealId: StateFlow<String?> = _editingMealId.asStateFlow()

    // Функция для установки идентификатора приёма пищи
    fun setEditingMealId(mealId: String?) {
        _editingMealId.value = mealId
    }

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apiClient = ClientAPI()
                val loadedProducts = apiClient.getProducts()
                _products.value = loadedProducts
            } catch (e: Exception) {
                _products.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
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

    fun addPendingProduct(product: Product) {
        val currentPending = _pendingProducts.value.toMutableList()
        if (!currentPending.contains(product)) {
            currentPending.add(product)
            _pendingProducts.value = currentPending
        }
    }

    fun clearCurrentSelection() {
        _currentSelection.value = emptySet()
    }
    
    fun saveCurrentSelection() {
        val selected = _currentSelection.value.toList()
        if (selected.isNotEmpty()) {
            _pendingProducts.value = selected
            _shouldShowWeightInput.value = true
        }
        _currentSelection.value = emptySet()
    }


    fun addProductWithWeight(weight: Int) {
        val currentProduct = _currentProductForWeight.value
        val mealId = _editingMealId.value ?: "default"
        
        if (currentProduct != null) {
            val updatedSelection = _finalSelection.value.toMutableList()
            
            // Проверяем, есть ли продукт с таким же product и mealId
            val existingProductIndex = updatedSelection.indexOfFirst { 
                it.product == currentProduct && it.mealId == mealId 
            }
            
            if (existingProductIndex != -1) {
                // Если продукт уже существует, заменяем его вес
                updatedSelection[existingProductIndex] = SelectedProduct(
                    product = currentProduct,
                    weight = weight,
                    mealId = mealId
                )
            } else {
                // Если продукт новый, добавляем его
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

    // Функция для редактирования веса продукта
    fun editProductWeight(product: Product, currentWeight: Int) {
        // Сохраняем продукт в списке, но помечаем его для редактирования
        _currentProductForWeight.value = product
        _pendingProducts.value = listOf(product)
        _shouldShowWeightInput.value = true
        checkAndStartWeightInput()
        
        // Устанавливаем флаг, что редактируем продукт из общего списка
        _editingMealId.value = "default"
    }

    // Функция для удаления продукта из списка (все приёмы пищи)
    fun removeProduct(product: Product) {
        val updatedSelection = _finalSelection.value.toMutableList()
        updatedSelection.removeAll { it.product == product }
        _finalSelection.value = updatedSelection
    }
    
    // Функция для удаления продукта из конкретного приёма пищи
    fun removeProductFromMeal(product: Product, mealId: String) {
        val updatedSelection = _finalSelection.value.toMutableList()
        updatedSelection.removeIf { it.product == product && it.mealId == mealId }
        _finalSelection.value = updatedSelection
    }

    fun clearWeightInput() {
        _currentProductForWeight.value = null
        _pendingProducts.value = emptyList()
        _shouldShowWeightInput.value = false
        _editingMealId.value = null
    }

    // Инициализация приёмов пищи
    fun initializeMeals() {
        if (_meals.value.isEmpty()) {
            val defaultMeals = listOf(
                Meal(id = UUID.randomUUID().toString(), time = LocalTime.of(6, 0), name = "Завтрак"),
                Meal(id = UUID.randomUUID().toString(), time = LocalTime.of(12, 0), name = "Обед"),
                Meal(id = UUID.randomUUID().toString(), time = LocalTime.of(18, 0), name = "Ужин")
            ).sortedBy { it.time }
            _meals.value = defaultMeals
        }
    }

    // Добавление нового приёма пищи
    fun addMeal() {
        val newMeal = Meal(
            id = UUID.randomUUID().toString(),
            time = LocalTime.now(),
            name = "Новый приём пищи"
        )
        val updatedMeals = (_meals.value + newMeal).sortedBy { it.time }
        _meals.value = updatedMeals
    }

    // Удаление приёма пищи
    fun removeMeal(mealId: String) {
        val updatedMeals = _meals.value.toMutableList()
        updatedMeals.removeIf { it.id == mealId }
        _meals.value = updatedMeals
        
        // Удаляем также все продукты, связанные с этим приёмом пищи
        val updatedSelection = _finalSelection.value.toMutableList()
        updatedSelection.removeIf { it.mealId == mealId }
        _finalSelection.value = updatedSelection
    }

    // Обновление времени приёма пищи
    fun updateMealTime(mealId: String, newTime: LocalTime) {
        val updatedMeals = _meals.value.toMutableList()
        val index = updatedMeals.indexOfFirst { it.id == mealId }
        if (index != -1) {
            updatedMeals[index] = updatedMeals[index].copy(time = newTime)
            // Сортируем приёмы пищи по времени
            _meals.value = updatedMeals.sortedBy { it.time }
        }
    }

    // Добавление продукта в приём пищи
    fun addProductToMeal(product: Product, weight: Int, mealId: String) {
        val updatedSelection = _finalSelection.value.toMutableList()
        val existingProductIndex = updatedSelection.indexOfFirst { it.product == product && it.mealId == mealId }
        if (existingProductIndex != -1) {
            // Если продукт уже существует в этом приёме пищи, заменяем его вес
            updatedSelection[existingProductIndex] = SelectedProduct(
                product,
                weight,
                mealId
            )
        } else {
            // Если продукт новый, добавляем его
            updatedSelection.add(SelectedProduct(product, weight, mealId))
        }
        _finalSelection.value = updatedSelection
    }

    // Редактирование веса продукта в приёме пищи
    fun editProductWeightInMeal(product: Product, mealId: String, currentWeight: Int) {
        _currentProductForWeight.value = product
        _pendingProducts.value = listOf(product)
        _shouldShowWeightInput.value = true
        _editingMealId.value = mealId
        checkAndStartWeightInput()
    }

    // Получение продуктов для конкретного приёма пищи
    fun getProductsForMeal(mealId: String): List<SelectedProduct> {
        return _finalSelection.value.filter { it.mealId == mealId }
    }

    // Получение общих БЖУ и калорийности для приёма пищи
    fun getMealNutrition(mealId: String): MealNutrition {
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
}