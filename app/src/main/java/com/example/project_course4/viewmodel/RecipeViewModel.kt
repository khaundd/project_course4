package com.example.project_course4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.api.RecipeResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeViewModel(private val clientAPI: ClientAPI) : ViewModel() {

    private val _recipes = MutableStateFlow<List<RecipeResponse>>(emptyList())
    val recipes: StateFlow<List<RecipeResponse>> = _recipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Для экрана просмотра рецепта по ссылке
    private val _sharedRecipe = MutableStateFlow<RecipeResponse?>(null)
    val sharedRecipe: StateFlow<RecipeResponse?> = _sharedRecipe.asStateFlow()

    private val _sharedRecipeLoading = MutableStateFlow(false)
    val sharedRecipeLoading: StateFlow<Boolean> = _sharedRecipeLoading.asStateFlow()

    private val _sharedRecipeError = MutableStateFlow<String?>(null)
    val sharedRecipeError: StateFlow<String?> = _sharedRecipeError.asStateFlow()

    // Результат генерации ссылки (null = не запрашивали, "" = ошибка, иначе — ссылка)
    private val _generatedLink = MutableStateFlow<String?>(null)
    val generatedLink: StateFlow<String?> = _generatedLink.asStateFlow()

    fun loadRecipes() {
        if (_recipes.value.isNotEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            clientAPI.getRecipes().fold(
                onSuccess = { _recipes.value = it },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }

    fun refreshRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            clientAPI.getRecipes().fold(
                onSuccess = { _recipes.value = it },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }

    fun getRecipeByName(name: String): RecipeResponse? =
        _recipes.value.find { it.name == name }

    fun generateLink(recipeId: Int) {
        viewModelScope.launch {
            clientAPI.generateRecipeLink(recipeId).fold(
                onSuccess = { _generatedLink.value = it },
                onFailure = { _generatedLink.value = null; _error.value = it.message }
            )
        }
    }

    fun clearGeneratedLink() {
        _generatedLink.value = null
    }

    fun setVisibility(recipeId: Int, isPublic: Boolean, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            clientAPI.setRecipeVisibility(recipeId, isPublic).fold(
                onSuccess = { response ->
                    // Обновляем рецепт в списке
                    _recipes.value = _recipes.value.map { recipe ->
                        if (recipe.productId == recipeId) {
                            recipe.copy(
                                isPublicRaw = if (response.isPublic) 1 else 0,
                                recipeLink = response.link
                            )
                        } else recipe
                    }
                    onResult(true, null)
                },
                onFailure = { onResult(false, it.message) }
            )
        }
    }

    fun loadSharedRecipe(token: String) {
        viewModelScope.launch {
            _sharedRecipeLoading.value = true
            _sharedRecipeError.value = null
            clientAPI.getSharedRecipe(token).fold(
                onSuccess = { _sharedRecipe.value = it },
                onFailure = { _sharedRecipeError.value = it.message }
            )
            _sharedRecipeLoading.value = false
        }
    }
}

