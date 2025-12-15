package com.example.project_course4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_course4.api.ClientAPI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Текущий временный выбор на экране выбора продуктов
    private val _currentSelection = MutableStateFlow<Set<Product>>(emptySet())
    val currentSelection: StateFlow<Set<Product>> = _currentSelection.asStateFlow()

    // Продукты, ожидающие ввода веса
    private val _pendingProducts = MutableStateFlow<List<Product>>(emptyList())
    val pendingProducts: StateFlow<List<Product>> = _pendingProducts.asStateFlow()

    // Окончательный выбор на главном экране
    private val _finalSelection = MutableStateFlow<List<SelectedProduct>>(emptyList())
    val finalSelection: StateFlow<List<SelectedProduct>> = _finalSelection.asStateFlow()

    // Текущий продукт для ввода веса
    private val _currentProductForWeight = MutableStateFlow<Product?>(null)
    val currentProductForWeight: StateFlow<Product?> = _currentProductForWeight.asStateFlow()

    // Флаг, показывающий нужно ли начинать ввод веса
    private val _shouldShowWeightInput = MutableStateFlow(false)
    val shouldShowWeightInput: StateFlow<Boolean> = _shouldShowWeightInput.asStateFlow()

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

    fun saveCurrentSelection() {
        val selected = _currentSelection.value.toList()
        if (selected.isNotEmpty()) {
            _pendingProducts.value = selected
            _shouldShowWeightInput.value = true
        }
        _currentSelection.value = emptySet()
    }

    fun clearCurrentSelection() {
        _currentSelection.value = emptySet()
    }


    fun addProductWithWeight(weight: Int) {
        val currentProduct = _currentProductForWeight.value
        if (currentProduct != null) {
            val selectedProduct = SelectedProduct(currentProduct, weight)
            val updatedSelection = _finalSelection.value.toMutableList()
            updatedSelection.add(selectedProduct)
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

    fun clearWeightInput() {
        _currentProductForWeight.value = null
        _pendingProducts.value = emptyList()
        _shouldShowWeightInput.value = false
    }
}