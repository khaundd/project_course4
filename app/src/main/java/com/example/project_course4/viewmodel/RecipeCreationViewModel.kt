package com.example.project_course4.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_course4.Product
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.api.CreateRecipeRequest
import com.example.project_course4.api.RecipeIngredient
import com.example.project_course4.composable_elements.screens.recipe.RecipeIngredientItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecipeCreationState(
    val name: String = "",
    val ingredients: List<RecipeIngredientItem> = emptyList(),
    val afterCookingWeight: String = "",
    val portionWeight: String = "100",
    val description: String = "",
    val errorMessage: String? = null,
    val editingProductId: Int? = null  // null = создание, non-null = редактирование
) {
    val totalIngredientsWeight: Int get() = ingredients.sumOf { it.weight }

    val portionsCount: Float
        get() {
            val cooking = afterCookingWeight.toFloatOrNull() ?: 0f
            val portion = portionWeight.toFloatOrNull() ?: 0f
            return if (portion > 0f && cooking > 0f) cooking / portion else 0f
        }
}

class RecipeCreationViewModel(
    private val clientAPI: ClientAPI
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeCreationState())
    val state: StateFlow<RecipeCreationState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Продукт, ожидающий ввода веса (показывается диалог на экране создания рецепта)
    private val _pendingProductForWeight = MutableStateFlow<Product?>(null)
    val pendingProductForWeight: StateFlow<Product?> = _pendingProductForWeight.asStateFlow()

    // Очередь продуктов, выбранных на SelectProductScreen
    private val _pendingQueue = MutableStateFlow<List<Product>>(emptyList())

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name, errorMessage = null)
    }

    fun updateAfterCookingWeight(value: String) {
        _state.value = _state.value.copy(afterCookingWeight = value)
    }

    fun updatePortionWeight(value: String) {
        _state.value = _state.value.copy(portionWeight = value)
    }

    fun updateDescription(value: String) {
        _state.value = _state.value.copy(description = value)
    }

    /** Вызывается перед переходом на SelectProductScreen */
    fun prepareForProductSelection() {
        // ничего не нужно — продукты придут через addSelectedProducts
    }

    /** Вызывается из NavigationApp после возврата с SelectProductScreen */
    fun addSelectedProducts(products: List<Product>) {
        if (products.isEmpty()) return
        // Фильтруем продукты, которые уже есть в списке ингредиентов
        val existingIds = _state.value.ingredients.map { it.product.productId }.toSet()
        val newProducts = products.filter { it.productId !in existingIds }
        if (newProducts.isEmpty()) return
        _pendingQueue.value = newProducts
        showNextPending()
    }

    private fun showNextPending() {
        val queue = _pendingQueue.value
        if (queue.isNotEmpty()) {
            _pendingProductForWeight.value = queue.first()
        }
    }

    fun confirmProductWeight(weight: Int) {
        val product = _pendingProductForWeight.value ?: return
        if (weight > 0) {
            val current = _state.value.ingredients.toMutableList()
            val existing = current.indexOfFirst { it.product.productId == product.productId }
            if (existing != -1) {
                current[existing] = current[existing].copy(weight = current[existing].weight + weight)
            } else {
                current.add(RecipeIngredientItem(product = product, weight = weight))
            }
            _state.value = _state.value.copy(ingredients = current)
        }
        // Убираем из очереди
        val queue = _pendingQueue.value.toMutableList()
        queue.removeFirstOrNull()
        _pendingQueue.value = queue
        _pendingProductForWeight.value = null
        showNextPending()
    }

    fun updateIngredientWeightAt(index: Int, weight: Int) {
        val current = _state.value.ingredients.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(weight = weight)
            _state.value = _state.value.copy(ingredients = current)
        }
    }

    fun removeIngredientAt(index: Int) {
        val current = _state.value.ingredients.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _state.value = _state.value.copy(ingredients = current)
        }
    }

    fun cancelPendingProduct() {
        val queue = _pendingQueue.value.toMutableList()
        queue.removeFirstOrNull()
        _pendingQueue.value = queue
        _pendingProductForWeight.value = null
        showNextPending()
    }

    /** Загружает данные существующего рецепта для редактирования */
    fun loadForEditing(productId: Int, name: String, ingredients: List<RecipeIngredientItem>, afterCookingWeight: Float, description: String) {
        _state.value = RecipeCreationState(
            name = name,
            ingredients = ingredients,
            afterCookingWeight = if (afterCookingWeight > 0f) afterCookingWeight.toInt().toString() else "",
            portionWeight = "100",
            description = description,
            editingProductId = productId
        )
    }

    fun saveRecipe(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(errorMessage = "Введите название рецепта")
            return
        }
        if (s.ingredients.isEmpty()) {
            _state.value = s.copy(errorMessage = "Добавьте хотя бы один продукт")
            return
        }
        val afterWeight = s.afterCookingWeight.toFloatOrNull() ?: 0f

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = CreateRecipeRequest(
                    dishName = s.name,
                    ingredients = s.ingredients.map {
                        RecipeIngredient(productId = it.product.productId, weight = it.weight)
                    },
                    afterCookingWeight = afterWeight,
                    description = s.description
                )
                val result = if (s.editingProductId != null) {
                    clientAPI.updateRecipe(s.editingProductId, request)
                } else {
                    clientAPI.createRecipe(request)
                }
                result.fold(
                    onSuccess = {
                        Log.d("RecipeCreation", "Рецепт сохранён: $it")
                        _state.value = RecipeCreationState()
                        onSuccess()
                    },
                    onFailure = { e ->
                        Log.e("RecipeCreation", "Ошибка: ${e.message}")
                        _state.value = _state.value.copy(errorMessage = e.message)
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}
