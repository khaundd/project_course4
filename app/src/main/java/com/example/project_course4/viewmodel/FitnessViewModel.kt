package com.example.project_course4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.api.ExerciseResponse
import com.example.project_course4.api.MuscleResponse
import com.example.project_course4.api.TrainingData
import com.example.project_course4.api.TrainingExerciseData
import com.example.project_course4.api.TrainingExerciseWithSets
import com.example.project_course4.api.TrainingPlanData
import com.example.project_course4.api.TrainingPlanDayData
import com.example.project_course4.api.TrainingPlanExerciseData
import com.example.project_course4.api.TrainingPlanSaveRequest
import com.example.project_course4.api.TrainingSaveRequest
import com.example.project_course4.api.TrainingSetData
import com.example.project_course4.api.TrainingWithSetsSaveRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

// ─── Editor state ─────────────────────────────────────────────────────────────

enum class ExerciseFieldType { SETS, REPS, WEIGHT, TIME }

fun defaultFieldsForCategory(category: String?): Set<ExerciseFieldType> {
    val cat = category?.lowercase() ?: ""
    return when {
        cat.contains("кардио") -> setOf(ExerciseFieldType.TIME)
        cat.contains("растяжк") || cat.contains("плиометрик") ->
            setOf(ExerciseFieldType.SETS, ExerciseFieldType.REPS)
        else -> setOf(ExerciseFieldType.SETS, ExerciseFieldType.REPS, ExerciseFieldType.WEIGHT)
    }
}

data class TrainingEditorSet(
    val setNumber: Int,
    val weightKg: String = "",
    val reps: String = "",
    val durationSec: String = ""
)

data class TrainingEditorExercise(
    val exerciseId: Int,
    val exerciseNameRu: String,
    val category: String? = null,
    // Агрегированные поля (используются когда нет детальных подходов)
    val sets: String = "",
    val reps: String = "",
    val weight: String = "",
    val exerciseTime: String = "",  // "HH:mm" string
    val activeFields: Set<ExerciseFieldType> = emptySet(),
    // Детальные подходы (если есть — редактируем их, иначе агрегированные)
    val detailedSets: List<TrainingEditorSet> = emptyList()
) {
    val hasDetailedSets: Boolean get() = detailedSets.isNotEmpty()
}

data class TrainingEditorState(
    val trainingId: Int = 0,
    val name: String = "",
    val description: String = "",
    val notes: String = "",
    val date: LocalDate = LocalDate.now(),
    val exercises: List<TrainingEditorExercise> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

data class TrainingPlanEditorExercise(
    val exerciseId: Int,
    val exerciseNameRu: String,
    val sets: String = "",
    val reps: String = "",
    val weight: String = "",
    val exerciseTime: String = "",
    val isTimeBased: Boolean = false
)

data class TrainingPlanEditorDay(
    val dayNumber: Int,
    val dayId: Int = 0,
    val dayName: String = "",
    val notes: String = "",
    val exercises: List<TrainingPlanEditorExercise> = emptyList()
)

data class TrainingPlanEditorState(
    val planId: Int = 0,
    val name: String = "",
    val description: String = "",
    val isPublic: Boolean = false,
    val days: List<TrainingPlanEditorDay> = listOf(TrainingPlanEditorDay(dayNumber = 1)),
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class FitnessViewModel(private val api: ClientAPI, private val currentUserId: Int = -1) : ViewModel() {

    // ─── Exercises catalog ────────────────────────────────────────────────────

    private val _exercises = MutableStateFlow<List<ExerciseResponse>>(emptyList())
    val exercises: StateFlow<List<ExerciseResponse>> = _exercises.asStateFlow()

    private val _exercisesTotal = MutableStateFlow(0)
    val exercisesTotal: StateFlow<Int> = _exercisesTotal.asStateFlow()

    private val _muscles = MutableStateFlow<List<MuscleResponse>>(emptyList())
    val muscles: StateFlow<List<MuscleResponse>> = _muscles.asStateFlow()

    private val _isLoadingExercises = MutableStateFlow(false)
    val isLoadingExercises: StateFlow<Boolean> = _isLoadingExercises.asStateFlow()

    private val _selectedExercise = MutableStateFlow<ExerciseResponse?>(null)
    val selectedExercise: StateFlow<ExerciseResponse?> = _selectedExercise.asStateFlow()

    // Filters
    private val _filterMuscleIds = MutableStateFlow<List<Int>>(emptyList())
    val filterMuscleIds: StateFlow<List<Int>> = _filterMuscleIds.asStateFlow()

    private val _filterEquipments = MutableStateFlow<List<String>>(emptyList())
    val filterEquipments: StateFlow<List<String>> = _filterEquipments.asStateFlow()

    private val _filterLevels = MutableStateFlow<List<String>>(emptyList())
    val filterLevels: StateFlow<List<String>> = _filterLevels.asStateFlow()

    private val _filterCategory = MutableStateFlow<String?>(null)
    val filterCategory: StateFlow<String?> = _filterCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var exerciseOffset = 0
    private val exercisePageSize = 30
    private var searchDebounceJob: Job? = null

    private val _hasMoreExercises = MutableStateFlow(true)
    val hasMoreExercises: StateFlow<Boolean> = _hasMoreExercises.asStateFlow()

    fun loadMuscles() {
        viewModelScope.launch {
            api.getMuscles().onSuccess { _muscles.value = it }
        }
    }

    fun setFilterMuscles(ids: List<Int>) { _filterMuscleIds.value = ids }
    fun setFilterEquipments(eqs: List<String>) { _filterEquipments.value = eqs }
    fun setFilterLevels(levels: List<String>) { _filterLevels.value = levels }
    fun setFilterCategory(cat: String?) { _filterCategory.value = cat; resetAndLoadExercises() }
    fun setSearchQuery(q: String) {
        _searchQuery.value = q
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(500L)
            resetAndLoadExercises()
        }
    }

    fun applyFilters(muscleIds: List<Int>, levels: List<String>, equipments: List<String>) {
        _filterMuscleIds.value = muscleIds
        _filterLevels.value = levels
        _filterEquipments.value = equipments
        resetAndLoadExercises()
    }

    private val _equipmentList = MutableStateFlow<List<String>>(emptyList())
    val equipmentList: StateFlow<List<String>> = _equipmentList.asStateFlow()

    fun loadEquipmentList() {
        if (_equipmentList.value.isNotEmpty()) return
        viewModelScope.launch {
            api.getEquipmentList().onSuccess { _equipmentList.value = it }
        }
    }    fun resetAndLoadExercises() {
        exerciseOffset = 0
        _exercises.value = emptyList()
        _hasMoreExercises.value = true
        loadExercises()
    }

    fun loadExercises() {
        if (_isLoadingExercises.value || !_hasMoreExercises.value) return
        viewModelScope.launch {
            _isLoadingExercises.value = true
            api.getExercises(
                muscleIds = _filterMuscleIds.value.ifEmpty { null },
                equipments = _filterEquipments.value.ifEmpty { null },
                levels = _filterLevels.value.ifEmpty { null },
                category = _filterCategory.value,
                search = _searchQuery.value.ifBlank { null },
                limit = exercisePageSize,
                offset = exerciseOffset
            ).onSuccess { resp ->
                _exercises.value = _exercises.value + resp.exercises
                _exercisesTotal.value = resp.total
                exerciseOffset += resp.exercises.size
                _hasMoreExercises.value = resp.exercises.size == exercisePageSize
            }
            _isLoadingExercises.value = false
        }
    }

    fun selectExercise(exercise: ExerciseResponse) {
        _selectedExercise.value = exercise
    }

    private val _isLoadingExerciseDetail = MutableStateFlow(false)
    val isLoadingExerciseDetail: StateFlow<Boolean> = _isLoadingExerciseDetail.asStateFlow()

    fun loadExerciseDetail(id: Int) {
        viewModelScope.launch {
            _isLoadingExerciseDetail.value = true
            _selectedExercise.value = null
            api.getExerciseById(id).onSuccess { _selectedExercise.value = it }
            _isLoadingExerciseDetail.value = false
        }
    }

    // ─── Trainings ────────────────────────────────────────────────────────────

    private val _trainings = MutableStateFlow<List<TrainingData>>(emptyList())
    val trainings: StateFlow<List<TrainingData>> = _trainings.asStateFlow()

    private val _isLoadingTrainings = MutableStateFlow(false)
    val isLoadingTrainings: StateFlow<Boolean> = _isLoadingTrainings.asStateFlow()

    private val _isDeletingTraining = MutableStateFlow(false)
    val isDeletingTraining: StateFlow<Boolean> = _isDeletingTraining.asStateFlow()

    private val _trainingEditor = MutableStateFlow(TrainingEditorState())
    val trainingEditor: StateFlow<TrainingEditorState> = _trainingEditor.asStateFlow()

    // Pending exercise selection for training editor
    private var _pendingTrainingExercises = MutableStateFlow<List<ExerciseResponse>>(emptyList())
    val pendingTrainingExercises: StateFlow<List<ExerciseResponse>> = _pendingTrainingExercises.asStateFlow()

    fun loadTrainings() {
        viewModelScope.launch {
            _isLoadingTrainings.value = true
            api.getTrainings().onSuccess { _trainings.value = it }
            _isLoadingTrainings.value = false
        }
    }

    fun startNewTraining() {
        _trainingEditor.value = TrainingEditorState(date = LocalDate.now())
    }

    fun startEditTraining(training: TrainingData) {
        _trainingEditor.value = TrainingEditorState(
            trainingId = training.id,
            name = training.name,
            description = training.description ?: "",
            notes = training.notes ?: "",
            date = training.date?.let { runCatching { LocalDate.parse(it) }.getOrDefault(LocalDate.now()) } ?: LocalDate.now(),
            exercises = training.exercises.map { ex ->
                val fields = buildSet {
                    if (ex.sets != null) add(ExerciseFieldType.SETS)
                    if (ex.reps != null) add(ExerciseFieldType.REPS)
                    if (ex.weight != null) add(ExerciseFieldType.WEIGHT)
                    if (ex.exerciseTime != null) add(ExerciseFieldType.TIME)
                    if (isEmpty()) addAll(setOf(ExerciseFieldType.SETS, ExerciseFieldType.REPS, ExerciseFieldType.WEIGHT))
                }
                TrainingEditorExercise(
                    exerciseId = ex.exerciseId,
                    exerciseNameRu = ex.exerciseNameRu ?: ex.exerciseName ?: "",
                    sets = ex.sets?.toString() ?: "",
                    reps = ex.reps?.toString() ?: "",
                    weight = ex.weight?.toString() ?: "",
                    exerciseTime = ex.exerciseTime?.let { t ->
                        val m = t / 60; val s = t % 60
                        "%02d:%02d".format(m, s)
                    } ?: "",
                    activeFields = fields,
                    detailedSets = ex.detailedSets.map { s ->
                        TrainingEditorSet(
                            setNumber = s.setNumber,
                            weightKg = s.weightKg?.let { w ->
                                if (w == w.toLong().toFloat()) w.toLong().toString() else w.toString()
                            } ?: "",
                            reps = s.reps?.toString() ?: "",
                            durationSec = s.durationSec?.toString() ?: ""
                        )
                    }
                )
            }
        )
    }

    fun updateTrainingName(name: String) { _trainingEditor.value = _trainingEditor.value.copy(name = name) }
    fun updateTrainingDescription(desc: String) { _trainingEditor.value = _trainingEditor.value.copy(description = desc) }
    fun updateTrainingNotes(notes: String) { _trainingEditor.value = _trainingEditor.value.copy(notes = notes) }
    fun updateTrainingDate(date: LocalDate) { _trainingEditor.value = _trainingEditor.value.copy(date = date) }

    fun addExercisesToTraining(exercises: List<ExerciseResponse>) {
        val current = _trainingEditor.value.exercises.toMutableList()
        exercises.forEach { ex ->
            if (current.none { it.exerciseId == ex.id }) {
                current.add(TrainingEditorExercise(
                    exerciseId = ex.id,
                    exerciseNameRu = ex.nameRu ?: ex.name,
                    category = ex.category,
                    activeFields = defaultFieldsForCategory(ex.category)
                ))
            }
        }
        _trainingEditor.value = _trainingEditor.value.copy(exercises = current)
    }

    fun removeExerciseFromTraining(index: Int) {
        val list = _trainingEditor.value.exercises.toMutableList()
        list.removeAt(index)
        _trainingEditor.value = _trainingEditor.value.copy(exercises = list)
    }

    fun updateTrainingExercise(index: Int, ex: TrainingEditorExercise) {
        val list = _trainingEditor.value.exercises.toMutableList()
        list[index] = ex
        _trainingEditor.value = _trainingEditor.value.copy(exercises = list)
    }

    fun saveTraining() {
        val state = _trainingEditor.value
        if (state.name.isBlank()) {
            _trainingEditor.value = state.copy(error = "Введите название тренировки")
            return
        }
        viewModelScope.launch {
            _trainingEditor.value = _trainingEditor.value.copy(isSaving = true, error = null)

            // Если хотя бы одно упражнение имеет детальные подходы — сохраняем через with-sets
            val hasDetailedSets = state.exercises.any { it.hasDetailedSets }

            if (hasDetailedSets) {
                val request = TrainingWithSetsSaveRequest(
                    name = state.name.trim(),
                    date = state.date.toString(),
                    description = state.description.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    exercises = state.exercises.mapNotNull { ex ->
                        if (ex.detailedSets.isEmpty()) return@mapNotNull null
                        TrainingExerciseWithSets(
                            exerciseId = ex.exerciseId,
                            sets = ex.detailedSets.mapIndexed { idx, s ->
                                TrainingSetData(
                                    setNumber = idx + 1,
                                    weightKg = s.weightKg.replace(',', '.').toFloatOrNull(),
                                    reps = s.reps.toIntOrNull(),
                                    durationSec = s.durationSec.toIntOrNull()
                                )
                            }
                        )
                    }
                )
                val result = if (state.trainingId == 0) api.createTrainingWithSets(request)
                             else api.updateTrainingWithSets(state.trainingId, request)
                result.fold(
                    onSuccess = {
                        _trainingEditor.value = _trainingEditor.value.copy(isSaving = false, saveSuccess = true)
                        loadTrainings()
                    },
                    onFailure = {
                        _trainingEditor.value = _trainingEditor.value.copy(isSaving = false, error = it.message)
                    }
                )
            } else {
                val request = TrainingSaveRequest(
                    name = state.name.trim(),
                    date = state.date.toString(),
                    description = state.description.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    exercises = state.exercises.map { ex ->
                        val hasTime = ExerciseFieldType.TIME in ex.activeFields
                        val timeSeconds = if (hasTime) {
                            val parts = ex.exerciseTime.split(":")
                            // Формат mm:ss → минуты*60 + секунды
                            if (parts.size == 2) (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
                            else ex.exerciseTime.toIntOrNull()
                        } else null
                        TrainingExerciseData(
                            exerciseId = ex.exerciseId,
                            sets = if (ExerciseFieldType.SETS in ex.activeFields) (ex.sets.toIntOrNull() ?: 0) else null,
                            reps = if (ExerciseFieldType.REPS in ex.activeFields) (ex.reps.toIntOrNull() ?: 0) else null,
                            weight = if (ExerciseFieldType.WEIGHT in ex.activeFields)
                                (ex.weight.replace(',', '.').toFloatOrNull() ?: 0f) else null,
                            exerciseTime = timeSeconds
                        )
                    }
                )
                val result = if (state.trainingId == 0) api.createTraining(request)
                             else api.updateTraining(state.trainingId, request)
                result.fold(
                    onSuccess = {
                        _trainingEditor.value = _trainingEditor.value.copy(isSaving = false, saveSuccess = true)
                        loadTrainings()
                    },
                    onFailure = {
                        _trainingEditor.value = _trainingEditor.value.copy(isSaving = false, error = it.message)
                    }
                )
            }
        }
    }

    fun deleteTraining(id: Int, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isDeletingTraining.value = true
            api.deleteTraining(id).onSuccess {
                _trainings.value = _trainings.value.filter { it.id != id }
            }
            _isDeletingTraining.value = false
            onDone?.invoke()
        }
    }

    /**
     * Создаёт запись в дневнике тренировок из дня тренировочного плана.
     * Загружает план по [planId], берёт день с номером [dayNumber] и сохраняет как тренировку.
     * Если [dayNumber] == 0 — вставляет все упражнения плана (объединяет все дни).
     */
    fun createTrainingFromPlanDay(planId: Int, dayNumber: Int, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            api.getTrainingPlanById(planId).onSuccess { plan ->
                val (trainingName, trainingDesc, exercises) = if (dayNumber == 0 || plan.days.isEmpty()) {
                    // Весь план — объединяем все упражнения всех дней
                    Triple(
                        plan.name,
                        plan.description?.ifBlank { null },
                        plan.days.flatMap { it.exercises }.distinctBy { it.exerciseId }
                    )
                } else {
                    val day = plan.days.find { it.dayNumber == dayNumber } ?: run {
                        onDone(false); return@onSuccess
                    }
                    Triple(
                        day.dayName?.takeIf { it.isNotBlank() } ?: plan.name,
                        day.notes?.ifBlank { null },
                        day.exercises
                    )
                }
                val request = TrainingSaveRequest(
                    name = trainingName,
                    date = LocalDate.now().toString(),
                    description = trainingDesc,
                    notes = null,
                    exercises = exercises.map { ex ->
                        TrainingExerciseData(
                            exerciseId = ex.exerciseId,
                            sets = ex.sets,
                            reps = ex.reps,
                            weight = ex.weight,
                            exerciseTime = ex.exerciseTime
                        )
                    }
                )
                api.createTraining(request).fold(
                    onSuccess = { loadTrainings(); onDone(true) },
                    onFailure = { onDone(false) }
                )
            }.onFailure { onDone(false) }
        }
    }

    fun resetTrainingEditorSuccess() { _trainingEditor.value = _trainingEditor.value.copy(saveSuccess = false) }
    fun clearTrainingEditorError() { _trainingEditor.value = _trainingEditor.value.copy(error = null) }

    // ─── Training Plans ───────────────────────────────────────────────────────

    private val _trainingPlans = MutableStateFlow<List<TrainingPlanData>>(emptyList())
    val trainingPlans: StateFlow<List<TrainingPlanData>> = _trainingPlans.asStateFlow()

    private val _publicTrainingPlans = MutableStateFlow<List<TrainingPlanData>>(emptyList())
    val publicTrainingPlans: StateFlow<List<TrainingPlanData>> = _publicTrainingPlans.asStateFlow()

    private val _isLoadingPlans = MutableStateFlow(false)
    val isLoadingPlans: StateFlow<Boolean> = _isLoadingPlans.asStateFlow()

    private val _planEditor = MutableStateFlow(TrainingPlanEditorState())
    val planEditor: StateFlow<TrainingPlanEditorState> = _planEditor.asStateFlow()

    // Pending exercise selection for plan editor: dayIndex
    private var _pendingPlanDayIndex: Int = -1

    // Tracks in-flight toggle jobs per plan to cancel on rapid taps
    private val _toggleJobs = mutableMapOf<Int, kotlinx.coroutines.Job>()

    fun loadTrainingPlans() {
        viewModelScope.launch {
            _isLoadingPlans.value = true
            api.getTrainingPlans().onSuccess { _trainingPlans.value = it }
            _isLoadingPlans.value = false
        }
    }

    fun loadPublicTrainingPlans() {
        viewModelScope.launch {
            _isLoadingPlans.value = true
            api.getPublicTrainingPlans().onSuccess { all ->
                _publicTrainingPlans.value = all
            }
            _isLoadingPlans.value = false
        }
    }

    fun startNewTrainingPlan() {
        _planEditor.value = TrainingPlanEditorState()
    }

    fun startEditTrainingPlan(plan: TrainingPlanData) {
        _planEditor.value = TrainingPlanEditorState(
            planId = plan.id,
            name = plan.name,
            description = plan.description ?: "",
            isPublic = plan.isPublic,
            days = plan.days.map { d ->
                TrainingPlanEditorDay(
                    dayNumber = d.dayNumber,
                    dayId = d.id,
                    dayName = d.dayName ?: "",
                    notes = d.notes ?: "",
                    exercises = d.exercises.map { ex ->
                        TrainingPlanEditorExercise(
                            exerciseId = ex.exerciseId,
                            exerciseNameRu = ex.exerciseNameRu ?: ex.exerciseName ?: "",
                            sets = ex.sets?.toString() ?: "",
                            reps = ex.reps?.toString() ?: "",
                            weight = ex.weight?.toString() ?: "",
                            exerciseTime = ex.exerciseTime?.toString() ?: "",
                            isTimeBased = ex.exerciseTime != null && ex.sets == null
                        )
                    }
                )
            }.ifEmpty { listOf(TrainingPlanEditorDay(dayNumber = 1)) }
        )
    }

    fun updatePlanName(name: String) { _planEditor.value = _planEditor.value.copy(name = name) }
    fun updatePlanDescription(desc: String) { _planEditor.value = _planEditor.value.copy(description = desc) }
    fun updatePlanPublic(v: Boolean) { _planEditor.value = _planEditor.value.copy(isPublic = v) }

    fun addPlanDay() {
        val days = _planEditor.value.days
        val next = (days.maxOfOrNull { it.dayNumber } ?: 0) + 1
        _planEditor.value = _planEditor.value.copy(days = days + TrainingPlanEditorDay(dayNumber = next))
    }

    fun removePlanDay(index: Int) {
        val days = _planEditor.value.days.toMutableList()
        if (days.size > 1) {
            days.removeAt(index)
            _planEditor.value = _planEditor.value.copy(
                days = days.mapIndexed { i, d -> d.copy(dayNumber = i + 1) }
            )
        }
    }

    fun setPendingPlanDayIndex(index: Int) { _pendingPlanDayIndex = index }

    fun addExercisesToPlanDay(dayIndex: Int, exercises: List<ExerciseResponse>) {
        val days = _planEditor.value.days.toMutableList()
        val day = days[dayIndex]
        val current = day.exercises.toMutableList()
        exercises.forEach { ex ->
            if (current.none { it.exerciseId == ex.id }) {
                current.add(TrainingPlanEditorExercise(
                    exerciseId = ex.id,
                    exerciseNameRu = ex.nameRu ?: ex.name,
                    isTimeBased = ex.category?.contains("кардио", ignoreCase = true) == true
                ))
            }
        }
        days[dayIndex] = day.copy(exercises = current)
        _planEditor.value = _planEditor.value.copy(days = days)
    }

    fun removeExerciseFromPlanDay(dayIndex: Int, exIndex: Int) {
        val days = _planEditor.value.days.toMutableList()
        val exList = days[dayIndex].exercises.toMutableList()
        exList.removeAt(exIndex)
        days[dayIndex] = days[dayIndex].copy(exercises = exList)
        _planEditor.value = _planEditor.value.copy(days = days)
    }

    fun updatePlanDayExercise(dayIndex: Int, exIndex: Int, ex: TrainingPlanEditorExercise) {
        val days = _planEditor.value.days.toMutableList()
        val exList = days[dayIndex].exercises.toMutableList()
        exList[exIndex] = ex
        days[dayIndex] = days[dayIndex].copy(exercises = exList)
        _planEditor.value = _planEditor.value.copy(days = days)
    }

    fun saveTrainingPlan() {
        val state = _planEditor.value
        if (state.name.isBlank()) {
            _planEditor.value = state.copy(error = "Введите название плана")
            return
        }
        viewModelScope.launch {
            _planEditor.value = _planEditor.value.copy(isSaving = true, error = null)
            val request = TrainingPlanSaveRequest(
                name = state.name.trim(),
                description = state.description.trim(),
                isPublic = state.isPublic,
                days = state.days.map { d ->
                    TrainingPlanDayData(
                        id = d.dayId,
                        dayNumber = d.dayNumber,
                        dayName = d.dayName.ifBlank { null },
                        notes = d.notes.ifBlank { null },
                        exercises = d.exercises.map { ex ->
                            TrainingPlanExerciseData(
                                exerciseId = ex.exerciseId,
                                sets = ex.sets.toIntOrNull(),
                                reps = ex.reps.toIntOrNull(),
                                weight = ex.weight.toFloatOrNull(),
                                exerciseTime = ex.exerciseTime.toIntOrNull()
                            )
                        }
                    )
                }
            )
            val result = if (state.planId == 0) api.createTrainingPlan(request).map { it }
                         else api.updateTrainingPlan(state.planId, request).map { it }
            result.fold(
                onSuccess = {
                    _planEditor.value = _planEditor.value.copy(isSaving = false, saveSuccess = true)
                    loadTrainingPlans()
                },
                onFailure = {
                    _planEditor.value = _planEditor.value.copy(isSaving = false, error = it.message)
                }
            )
        }
    }

    fun deleteTrainingPlan(id: Int) {
        viewModelScope.launch {
            api.deleteTrainingPlan(id).onSuccess {
                _trainingPlans.value = _trainingPlans.value.filter { it.id != id }
            }
        }
    }

    private val _selectedPlan = MutableStateFlow<TrainingPlanData?>(null)
    val selectedPlan: StateFlow<TrainingPlanData?> = _selectedPlan.asStateFlow()

    private val _isLoadingPlanDetail = MutableStateFlow(false)
    val isLoadingPlanDetail: StateFlow<Boolean> = _isLoadingPlanDetail.asStateFlow()

    fun loadTrainingPlanById(id: Int) {
        viewModelScope.launch {
            _isLoadingPlanDetail.value = true
            api.getTrainingPlanById(id).onSuccess { _selectedPlan.value = it }
            _isLoadingPlanDetail.value = false
        }
    }

    fun loadTrainingPlanForEdit(id: Int, onReady: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isLoadingPlanDetail.value = true
            api.getTrainingPlanById(id).onSuccess { plan ->
                startEditTrainingPlan(plan)
            }
            _isLoadingPlanDetail.value = false
            onReady?.invoke()
        }
    }

    fun resetPlanEditorSuccess() { _planEditor.value = _planEditor.value.copy(saveSuccess = false) }
    fun clearPlanEditorError() { _planEditor.value = _planEditor.value.copy(error = null) }

    fun updatePlanDayName(dayIndex: Int, name: String) {
        val days = _planEditor.value.days.toMutableList()
        days[dayIndex] = days[dayIndex].copy(dayName = name)
        _planEditor.value = _planEditor.value.copy(days = days)
    }

    fun updatePlanDayNotes(dayIndex: Int, notes: String) {
        val days = _planEditor.value.days.toMutableList()
        days[dayIndex] = days[dayIndex].copy(notes = notes)
        _planEditor.value = _planEditor.value.copy(days = days)
    }

    fun copyPlanDay(dayIndex: Int) {
        val days = _planEditor.value.days.toMutableList()
        val source = days.getOrNull(dayIndex) ?: return
        val nextNum = (days.maxOfOrNull { it.dayNumber } ?: 0) + 1
        val copy = source.copy(dayNumber = nextNum, dayId = 0)
        days.add(copy)
        _planEditor.value = _planEditor.value.copy(days = days)
    }

    fun toggleTrainingPlanPublic(planId: Int) {
        val current = _trainingPlans.value.find { it.id == planId } ?: return
        val newPublic = !current.isPublic
        // Optimistic update — UI reacts immediately
        _trainingPlans.value = _trainingPlans.value.map {
            if (it.id == planId) it.copy(isPublicRaw = if (newPublic) 1 else 0) else it
        }
        // Cancel any previous in-flight request for this plan
        _toggleJobs[planId]?.cancel()
        _toggleJobs[planId] = viewModelScope.launch {
            api.getTrainingPlanById(planId).onSuccess { plan ->
                val request = TrainingPlanSaveRequest(
                    name = plan.name,
                    description = plan.description ?: "",
                    isPublic = newPublic,
                    days = plan.days
                )
                api.updateTrainingPlan(planId, request).onFailure {
                    // Rollback on error
                    _trainingPlans.value = _trainingPlans.value.map {
                        if (it.id == planId) it.copy(isPublicRaw = if (current.isPublic) 1 else 0) else it
                    }
                }
            }.onFailure {
                // Rollback on error
                _trainingPlans.value = _trainingPlans.value.map {
                    if (it.id == planId) it.copy(isPublicRaw = if (current.isPublic) 1 else 0) else it
                }
            }
        }
    }
}
