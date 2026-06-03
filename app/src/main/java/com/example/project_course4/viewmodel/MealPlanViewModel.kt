package com.example.project_course4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.api.MealPlanComponentData
import com.example.project_course4.api.MealPlanData
import com.example.project_course4.api.MealPlanDayData
import com.example.project_course4.api.MealPlanMealData
import com.example.project_course4.api.MealPlanSaveRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─── Editor state ─────────────────────────────────────────────────────────────

data class MealPlanEditorMeal(
    val name: String = "",
    val mealTime: String = "",
    val components: List<MealPlanComponentData> = emptyList()
)

data class MealPlanEditorDay(
    val dayNumber: Int,
    val dayOfWeek: Int? = null,
    val meals: List<MealPlanEditorMeal> = emptyList(),
    val notes: String = ""
)

data class MealPlanEditorState(
    val planId: Int = 0,          // 0 = новый план
    val name: String = "",
    val description: String = "",
    val isPublic: Boolean = false,
    val targetCalories: String = "2000",
    val proteinPct: String = "30",
    val fatsPct: String = "30",
    val carbsPct: String = "40",
    val days: List<MealPlanEditorDay> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class MealPlanViewModel(private val api: ClientAPI) : ViewModel() {

    private val _plans = MutableStateFlow<List<MealPlanData>>(emptyList())
    val plans: StateFlow<List<MealPlanData>> = _plans.asStateFlow()

    private val _publicPlans = MutableStateFlow<List<MealPlanData>>(emptyList())
    val publicPlans: StateFlow<List<MealPlanData>> = _publicPlans.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _editor = MutableStateFlow(MealPlanEditorState())
    val editor: StateFlow<MealPlanEditorState> = _editor.asStateFlow()

    // Отслеживаем in-flight jobs для замочка — отменяем при быстрых повторных кликах
    private val _toggleJobs = mutableMapOf<Int, Job>()

    // ─── Load ──────────────────────────────────────────────────────────────

    fun loadPlans() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            api.getMealPlans().fold(
                onSuccess = { _plans.value = it },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }

    fun loadPublicPlans() {
        viewModelScope.launch {
            _isLoading.value = true
            api.getPublicMealPlans().fold(
                onSuccess = { _publicPlans.value = it },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }

    // ─── Delete ────────────────────────────────────────────────────────────

    fun deletePlan(planId: Int) {
        viewModelScope.launch {
            api.deleteMealPlan(planId).fold(
                onSuccess = { _plans.value = _plans.value.filter { it.planId != planId } },
                onFailure = { _error.value = it.message }
            )
        }
    }

    // ─── Toggle public ─────────────────────────────────────────────────────

    fun togglePlanPublic(planId: Int) {
        val current = _plans.value.find { it.planId == planId } ?: return
        val newPublic = !current.isPublic
        // Оптимистичное обновление — UI реагирует сразу
        _plans.value = _plans.value.map {
            if (it.planId == planId) it.copy(isPublicRaw = if (newPublic) 1 else 0) else it
        }
        // Отменяем предыдущий запрос для этого плана (дебаунс)
        _toggleJobs[planId]?.cancel()
        _toggleJobs[planId] = viewModelScope.launch {
            delay(600L) // ждём 600мс — если будет ещё клик, этот job отменится
            val request = MealPlanSaveRequest(
                name = current.name,
                description = current.description,
                isPublic = newPublic,
                targetCalories = current.targetCalories,
                proteinPct = current.proteinPct,
                fatsPct = current.fatsPct,
                carbsPct = current.carbsPct,
                days = current.days
            )
            api.updateMealPlan(planId, request).onFailure {
                // Откат при ошибке
                _plans.value = _plans.value.map { p ->
                    if (p.planId == planId) p.copy(isPublicRaw = if (current.isPublic) 1 else 0) else p
                }
            }
        }
    }

    // ─── Editor helpers ────────────────────────────────────────────────────

    fun startNewPlan() {
        _editor.value = MealPlanEditorState(
            days = listOf(MealPlanEditorDay(dayNumber = 1))
        )
    }

    fun startEditPlan(plan: MealPlanData) {
        _editor.value = MealPlanEditorState(
            planId = plan.planId,
            name = plan.name,
            description = plan.description,
            isPublic = plan.isPublic,
            targetCalories = plan.targetCalories.toInt().toString(),
            proteinPct = plan.proteinPct.toInt().toString(),
            fatsPct = plan.fatsPct.toInt().toString(),
            carbsPct = plan.carbsPct.toInt().toString(),
            days = plan.days.map { d ->
                MealPlanEditorDay(
                    dayNumber = d.dayNumber,
                    dayOfWeek = d.dayOfWeek,
                    notes = d.notes ?: "",
                    meals = d.meals.map { m ->
                        MealPlanEditorMeal(
                            name = m.name,
                            mealTime = m.mealTime,
                            components = m.components
                        )
                    }
                )
            }
        )
    }

    fun updateEditorName(name: String) { _editor.value = _editor.value.copy(name = name) }
    fun updateEditorDescription(desc: String) { _editor.value = _editor.value.copy(description = desc) }
    fun updateEditorPublic(isPublic: Boolean) { _editor.value = _editor.value.copy(isPublic = isPublic) }
    fun updateEditorTargetCalories(v: String) { _editor.value = _editor.value.copy(targetCalories = v) }
    fun updateEditorProteinPct(v: String) { _editor.value = _editor.value.copy(proteinPct = v) }
    fun updateEditorFatsPct(v: String) { _editor.value = _editor.value.copy(fatsPct = v) }
    fun updateEditorCarbsPct(v: String) { _editor.value = _editor.value.copy(carbsPct = v) }

    fun addDay() {
        val days = _editor.value.days
        val nextNum = (days.maxOfOrNull { it.dayNumber } ?: 0) + 1
        _editor.value = _editor.value.copy(days = days + MealPlanEditorDay(dayNumber = nextNum))
    }

    fun removeDay(dayIndex: Int) {
        val days = _editor.value.days.toMutableList()
        if (days.size > 1) {
            days.removeAt(dayIndex)
            // Перенумеровываем дни по порядку
            val renumbered = days.mapIndexed { i, d -> d.copy(dayNumber = i + 1) }
            _editor.value = _editor.value.copy(days = renumbered)
        }
    }

    fun updateDayNotes(dayIndex: Int, notes: String) {
        val days = _editor.value.days.toMutableList()
        days[dayIndex] = days[dayIndex].copy(notes = notes)
        _editor.value = _editor.value.copy(days = days)
    }

    fun addMealToDay(dayIndex: Int) {
        val days = _editor.value.days.toMutableList()
        val day = days[dayIndex]
        days[dayIndex] = day.copy(meals = day.meals + MealPlanEditorMeal())
        _editor.value = _editor.value.copy(days = days)
    }

    fun removeMealFromDay(dayIndex: Int, mealIndex: Int) {
        val days = _editor.value.days.toMutableList()
        val day = days[dayIndex]
        val meals = day.meals.toMutableList()
        meals.removeAt(mealIndex)
        days[dayIndex] = day.copy(meals = meals)
        _editor.value = _editor.value.copy(days = days)
    }

    fun updateMealName(dayIndex: Int, mealIndex: Int, name: String) {
        updateMeal(dayIndex, mealIndex) { it.copy(name = name) }
    }

    fun updateMealTime(dayIndex: Int, mealIndex: Int, time: String) {
        updateMeal(dayIndex, mealIndex) { it.copy(mealTime = time) }
    }

    fun addComponentToMeal(dayIndex: Int, mealIndex: Int, productId: Int, weight: Int) {
        updateMeal(dayIndex, mealIndex) { meal ->
            meal.copy(components = meal.components + MealPlanComponentData(productId, weight))
        }
    }

    fun removeComponentFromMeal(dayIndex: Int, mealIndex: Int, componentIndex: Int) {
        updateMeal(dayIndex, mealIndex) { meal ->
            val comps = meal.components.toMutableList()
            comps.removeAt(componentIndex)
            meal.copy(components = comps)
        }
    }

    fun updateComponentWeight(dayIndex: Int, mealIndex: Int, componentIndex: Int, weight: Int) {
        updateMeal(dayIndex, mealIndex) { meal ->
            val comps = meal.components.toMutableList()
            comps[componentIndex] = comps[componentIndex].copy(weight = weight)
            meal.copy(components = comps)
        }
    }

    private fun updateMeal(dayIndex: Int, mealIndex: Int, transform: (MealPlanEditorMeal) -> MealPlanEditorMeal) {
        val days = _editor.value.days.toMutableList()
        val day = days[dayIndex]
        val meals = day.meals.toMutableList()
        meals[mealIndex] = transform(meals[mealIndex])
        days[dayIndex] = day.copy(meals = meals)
        _editor.value = _editor.value.copy(days = days)
    }

    // ─── Save ──────────────────────────────────────────────────────────────

    fun savePlan() {
        val state = _editor.value
        if (state.name.isBlank()) {
            _editor.value = state.copy(error = "Введите название плана")
            return
        }
        val calories = state.targetCalories.toFloatOrNull()
        if (calories == null || calories <= 0) {
            _editor.value = state.copy(error = "Введите корректную целевую калорийность")
            return
        }
        val p = state.proteinPct.toFloatOrNull() ?: 0f
        val f = state.fatsPct.toFloatOrNull() ?: 0f
        val c = state.carbsPct.toFloatOrNull() ?: 0f
        if (Math.abs(p + f + c - 100f) > 0.5f) {
            _editor.value = state.copy(error = "Сумма процентов БЖУ должна быть 100%")
            return
        }
        viewModelScope.launch {
            _editor.value = _editor.value.copy(isSaving = true, error = null)
            val request = MealPlanSaveRequest(
                name = state.name.trim(),
                description = state.description.trim(),
                isPublic = state.isPublic,
                targetCalories = calories,
                proteinPct = p,
                fatsPct = f,
                carbsPct = c,
                days = state.days.map { d ->
                    MealPlanDayData(
                        dayNumber = d.dayNumber,
                        dayOfWeek = d.dayOfWeek,
                        notes = d.notes.ifBlank { null },
                        meals = d.meals.map { m ->
                            MealPlanMealData(
                                name = m.name,
                                mealTime = m.mealTime.ifBlank { "12:00" },
                                components = m.components
                            )
                        }
                    )
                }
            )
            val result = if (state.planId == 0) {
                api.createMealPlan(request).map { it.message ?: "Сохранено" }
            } else {
                api.updateMealPlan(state.planId, request)
            }
            result.fold(
                onSuccess = {
                    _editor.value = _editor.value.copy(isSaving = false, saveSuccess = true)
                    loadPlans()
                },
                onFailure = {
                    _editor.value = _editor.value.copy(isSaving = false, error = it.message)
                }
            )
        }
    }

    fun resetSaveSuccess() { _editor.value = _editor.value.copy(saveSuccess = false) }
    fun clearError() { _error.value = null }

    // ─── Pending product selection (from SelectProductScreen) ──────────────

    private var _pendingDayIndex: Int = -1
    private var _pendingMealIndex: Int = -1

    // Продукты, ожидающие ввода веса
    private val _pendingProductsForWeight = MutableStateFlow<List<com.example.project_course4.Product>>(emptyList())
    val pendingProductsForWeight: StateFlow<List<com.example.project_course4.Product>> = _pendingProductsForWeight.asStateFlow()

    fun setPendingMealTarget(dayIndex: Int, mealIndex: Int) {
        _pendingDayIndex = dayIndex
        _pendingMealIndex = mealIndex
    }

    fun setPendingProductsForWeight(products: List<com.example.project_course4.Product>) {
        val existingIds = if (_pendingDayIndex >= 0 && _pendingMealIndex >= 0) {
            _editor.value.days.getOrNull(_pendingDayIndex)
                ?.meals?.getOrNull(_pendingMealIndex)
                ?.components?.map { it.productId }?.toSet() ?: emptySet()
        } else emptySet()
        _pendingProductsForWeight.value = products.filter { it.productId !in existingIds }
    }

    fun confirmProductWeight(product: com.example.project_course4.Product, weight: Int) {
        if (_pendingDayIndex < 0 || _pendingMealIndex < 0) return
        addComponentToMeal(_pendingDayIndex, _pendingMealIndex, product.productId, weight)
        val remaining = _pendingProductsForWeight.value.drop(1)
        _pendingProductsForWeight.value = remaining
        if (remaining.isEmpty()) {
            _pendingDayIndex = -1
            _pendingMealIndex = -1
        }
    }

    fun skipPendingProduct() {
        val remaining = _pendingProductsForWeight.value.drop(1)
        _pendingProductsForWeight.value = remaining
        if (remaining.isEmpty()) {
            _pendingDayIndex = -1
            _pendingMealIndex = -1
        }
    }

    fun addProductsFromSelection(products: List<com.example.project_course4.Product>, weight: Int = 100) {
        if (_pendingDayIndex < 0 || _pendingMealIndex < 0) return
        products.forEach { product ->
            addComponentToMeal(_pendingDayIndex, _pendingMealIndex, product.productId, weight)
        }
        _pendingDayIndex = -1
        _pendingMealIndex = -1
    }
}
