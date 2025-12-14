package com.example.project_course4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Текущий временный выбор (сбрасывается при каждом открытии SelectProductScreen)
    private val _currentSelection = MutableStateFlow<Set<Product>>(emptySet())
    val currentSelection: StateFlow<Set<Product>> = _currentSelection.asStateFlow()

    // Финальный выбор на главном экране
    private val _finalSelection = MutableStateFlow<Set<Product>>(emptySet())
    val finalSelection: StateFlow<Set<Product>> = _finalSelection.asStateFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() {
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

    fun saveCurrentSelection() {
        // Добавляем текущий выбор к финальному
        val updatedSelection = _finalSelection.value.toMutableSet()
        updatedSelection.addAll(_currentSelection.value)
        _finalSelection.value = updatedSelection

        // Очищаем текущий выбор
        _currentSelection.value = emptySet()
    }

    fun clearCurrentSelection() {
        _currentSelection.value = emptySet()
    }
}
