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

    // Временный выбор на экране выбора
    private val _tempSelection = MutableStateFlow<Set<Product>>(emptySet())
    val tempSelection: StateFlow<Set<Product>> = _tempSelection.asStateFlow()

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

    fun toggleTempSelection(product: Product) {
        val current = _tempSelection.value.toMutableSet()
        if (current.contains(product)) {
            current.remove(product)
        } else {
            current.add(product)
        }
        _tempSelection.value = current
    }

    fun saveSelection() {
        _finalSelection.value = _tempSelection.value
        clearTempSelection()
    }

    fun clearTempSelection() {
        _tempSelection.value = emptySet()
    }

    fun getFinalSelectionList(): List<Product> {
        return _finalSelection.value.toList()
    }
}