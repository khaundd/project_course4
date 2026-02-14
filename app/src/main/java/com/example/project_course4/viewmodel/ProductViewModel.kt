package com.example.project_course4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.project_course4.Meal
import com.example.project_course4.MealNutrition
import com.example.project_course4.Product
import com.example.project_course4.ProductCreationState
import com.example.project_course4.ProductRepository
import com.example.project_course4.SelectedProduct
import com.example.project_course4.SessionManager
import com.example.project_course4.api.ClientAPI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class ProductViewModel(
    private val repository: ProductRepository
) : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = repository.getProductsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Состояние для создания нового продукта
    private var _productCreationState = MutableStateFlow(ProductCreationState())
    val productCreationState: StateFlow<ProductCreationState> = _productCreationState.asStateFlow()

    // Флаг, показывающий нужно ли показывать экран создания продукта
    private var _shouldShowProductCreation = MutableStateFlow(false)
    val shouldShowProductCreation: StateFlow<Boolean> = _shouldShowProductCreation.asStateFlow()

    // Флаг, указывающий, что продукты добавляются из списка
    private var _isAddingFromList = MutableStateFlow(false)
    var isAddingFromList: StateFlow<Boolean> = _isAddingFromList.asStateFlow()

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
        viewModelScope.launch {
            repository.fetchInitialProducts()
        }
    }

    // Функция для обновления состояния создания продукта
    fun updateProductCreationState(newState: ProductCreationState) {
        _productCreationState.value = newState
    }

    // Функция для сброса состояния создания продукта
    fun resetProductCreationState() {
        _productCreationState.value = ProductCreationState()
    }

    // Функция для показа экрана создания продукта
    fun showProductCreationScreen() {
        resetProductCreationState()
        _shouldShowProductCreation.value = true
    }

    // Функция для скрытия экрана создания продукта
    fun hideProductCreationScreen() {
        _shouldShowProductCreation.value = false
        resetProductCreationState()
    }

    // Функция для навигации на экран создания продукта
    fun navigateToProductCreation(navController: NavController) {
        resetProductCreationState()
        navController.navigate("product_creation")
    }

    // Функция для возврата с экрана создания продукта
    fun navigateBackFromProductCreation(navController: NavController) {
        navController.popBackStack()
        resetProductCreationState()
    }

//    fun loadProducts() {
//        viewModelScope.launch {
//            _isLoading.value = true
//            try {
//                val loadedProducts = clientAPI.getProducts()
//                _products.value = loadedProducts
//            } catch (e: Exception) {
//                _products.value = emptyList()
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }

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

    // Добавить выбранные продукты в приём пищи через диалог ввода веса (editingMealId уже установлен).
    fun addSelectionToMealWithWeightInput() {
        val selected = _currentSelection.value.toList()
        if (selected.isEmpty()) return
        
        val mealId = _editingMealId.value ?: "default"
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
                // Используем флаг isAddingFromList для определения режима
                if (isAddingFromList.value) {
                    // Режим добавления из списка - суммируем вес
                    val existingWeight = updatedSelection[existingProductIndex].weight
                    updatedSelection[existingProductIndex] = SelectedProduct(
                        product = currentProduct,
                        weight = existingWeight + weight,
                        mealId = mealId
                    )
                } else {
                    // Режим редактирования - заменяем вес
                    updatedSelection[existingProductIndex] = SelectedProduct(
                        product = currentProduct,
                        weight = weight,
                        mealId = mealId
                    )
                }
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
                // Сбрасываем флаг после завершения ввода
                _isAddingFromList.value = false
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

    // Функция для редактирования веса продукта в приёме пищи
    fun editProductWeightInMeal(product: Product, mealId: String, currentWeight: Int) {
        _currentProductForWeight.value = product
        _pendingProducts.value = listOf(product)
        _shouldShowWeightInput.value = true
        _editingMealId.value = mealId
        checkAndStartWeightInput()
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
        _isAddingFromList.value = false
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
    fun getCaloriesForDate(date: LocalDate): Int {
        // В будущем здесь будет запрос к БД или API по конкретной дате
        // Пока возвращаем случайное число для теста или 0
        return if (date == LocalDate.now()) 1500 else 0
    }

//    fun initialSync(currentUserId: Int) {
//        viewModelScope.launch {
//            // 1. Тянем 50 записей с сервера
//            val remoteProducts = clientAPI.getInitialProducts()
//            // 2. Превращаем их в локальные сущности и помечаем isSavedLocally = true
//            val localEntities = remoteProducts.map { it.toEntity(isSavedLocally = true) }
//            // 3. Сохраняем в Room
//            db.productDao().insertAll(localEntities)
//        }
//    }
}