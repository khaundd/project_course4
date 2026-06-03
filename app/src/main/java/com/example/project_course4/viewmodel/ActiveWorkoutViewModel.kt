package com.example.project_course4.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.api.TrainingData
import com.example.project_course4.api.TrainingExerciseWithSets
import com.example.project_course4.api.TrainingSetData
import com.example.project_course4.api.TrainingWithSetsSaveRequest
import com.example.project_course4.local_db.dao.ActiveWorkoutDao
import com.example.project_course4.local_db.entities.ActiveWorkoutEntity
import com.example.project_course4.local_db.entities.ActiveWorkoutSetEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

// ─── UI models ────────────────────────────────────────────────────────────────

enum class ActiveFieldType { WEIGHT, REPS, DURATION }

/** Метрики по умолчанию в зависимости от категории упражнения */
fun defaultActiveFields(category: String?): Set<ActiveFieldType> {
    val cat = category?.lowercase() ?: ""
    return when {
        cat.contains("кардио") -> setOf(ActiveFieldType.DURATION)
        cat.contains("растяжк") || cat.contains("плиометрик") -> setOf(ActiveFieldType.REPS)
        else -> setOf(ActiveFieldType.WEIGHT, ActiveFieldType.REPS)
    }
}

data class ActiveSet(
    val localId: Int = 0,   // id в Room (0 = ещё не сохранён)
    val setNumber: Int,
    val weightKg: String = "",
    val reps: String = "",
    val durationSec: String = "",
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false
)

data class ActiveExercise(
    val exerciseId: Int,
    val exerciseNameRu: String,
    val sets: List<ActiveSet> = listOf(ActiveSet(setNumber = 1)),
    val restTimeSec: Int = 90,  // таймер отдыха для этого упражнения
    val activeFields: Set<ActiveFieldType> = setOf(ActiveFieldType.WEIGHT, ActiveFieldType.REPS)
)

data class ActiveWorkoutState(
    val workoutId: Int = 0,
    val name: String = "",
    val exercises: List<ActiveExercise> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val elapsedSec: Long = 0L,
    val restRemainingSec: Int = 0,   // 0 = таймер не активен
    val isRestTimerRunning: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val isFinished: Boolean = false,
    val isDiscarded: Boolean = false,  // true = empty workout was auto-discarded
    val savedTrainingId: Int = 0,
    val notes: String = ""
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ActiveWorkoutViewModel(
    private val dao: ActiveWorkoutDao,
    private val api: ClientAPI
) : ViewModel() {

    private val _state = MutableStateFlow(ActiveWorkoutState())
    val state: StateFlow<ActiveWorkoutState> = _state.asStateFlow()

    // История тренировок для вкладки "История"
    private val _historyTrainings = MutableStateFlow<List<TrainingData>>(emptyList())
    val historyTrainings: StateFlow<List<TrainingData>> = _historyTrainings.asStateFlow()

    private var elapsedTimerJob: Job? = null
    private var restTimerJob: Job? = null

    // ─── Запуск тренировки ────────────────────────────────────────────────────

    fun startEmptyWorkout(name: String = "Тренировка") {
        viewModelScope.launch {
            val entity = ActiveWorkoutEntity(
                name = name,
                startedAt = System.currentTimeMillis()
            )
            val id = dao.insertWorkout(entity).toInt()
            _state.value = ActiveWorkoutState(workoutId = id, name = name)
            startElapsedTimer()
            loadHistory()
        }
    }

    fun startFromTemplate(training: TrainingData) {
        viewModelScope.launch {
            val entity = ActiveWorkoutEntity(
                name = training.name,
                startedAt = System.currentTimeMillis(),
                sourceTrainingId = training.id
            )
            val id = dao.insertWorkout(entity).toInt()

            // Группируем упражнения из шаблона
            val exercises = training.exercises.map { ex ->
                val setsCount = ex.sets?.takeIf { it > 0 } ?: 3
                // Определяем метрики из данных упражнения шаблона
                val fields = buildSet<ActiveFieldType> {
                    if (ex.weight != null && ex.weight > 0) add(ActiveFieldType.WEIGHT)
                    if (ex.reps != null && ex.reps > 0) add(ActiveFieldType.REPS)
                    if (ex.exerciseTime != null && ex.exerciseTime > 0) add(ActiveFieldType.DURATION)
                    if (isEmpty()) addAll(setOf(ActiveFieldType.WEIGHT, ActiveFieldType.REPS))
                }
                ActiveExercise(
                    exerciseId = ex.exerciseId,
                    exerciseNameRu = ex.exerciseNameRu ?: ex.exerciseName ?: "Упражнение",
                    activeFields = fields,
                    sets = (1..setsCount).map { n ->
                        ActiveSet(
                            setNumber = n,
                            weightKg = ex.weight?.let { if (it > 0) it.toString() else "" } ?: "",
                            reps = ex.reps?.let { if (it > 0) it.toString() else "" } ?: "",
                            durationSec = ex.exerciseTime?.let { if (it > 0) it.toString() else "" } ?: ""
                        )
                    }
                )
            }

            _state.value = ActiveWorkoutState(
                workoutId = id,
                name = training.name,
                exercises = exercises
            )
            startElapsedTimer()
            loadHistory()
        }
    }

    // Восстановление незавершённой тренировки при перезапуске приложения.
    // Только загружает состояние из БД — таймер НЕ запускается здесь,
    // он стартует когда пользователь открывает экран активной тренировки (ON_RESUME).
    fun resumeIfActive() {
        viewModelScope.launch {
            val workout = dao.getActiveWorkout() ?: return@launch
            val sets = dao.getSetsForWorkoutOnce(workout.id)

            val exerciseMap = sets.groupBy { it.exerciseId }
            val exercises = exerciseMap.map { (exId, exSets) ->
                ActiveExercise(
                    exerciseId = exId,
                    exerciseNameRu = exSets.first().exerciseNameRu,
                    restTimeSec = exSets.first().restTimeSec,
                    sets = exSets.sortedBy { it.setNumber }.map { s ->
                        ActiveSet(
                            localId = s.id,
                            setNumber = s.setNumber,
                            weightKg = s.weightKg?.toString() ?: "",
                            reps = s.reps?.toString() ?: "",
                            durationSec = s.durationSec?.toString() ?: "",
                            isCompleted = s.completedAt != null,
                            isSkipped = s.isSkipped
                        )
                    }
                )
            }

            _state.value = ActiveWorkoutState(
                workoutId = workout.id,
                name = workout.name,
                exercises = exercises,
                elapsedSec = workout.pausedElapsedSec
            )
            // startElapsedTimer() — не вызываем здесь, таймер запустится через
            // resumeWorkoutTimer() когда откроется ActiveWorkoutScreen (Lifecycle.Event.ON_RESUME)
            loadHistory()
        }
    }
    // ─── Упражнения ───────────────────────────────────────────────────────────

    fun addExercise(exerciseId: Int, exerciseNameRu: String, category: String? = null) {
        val current = _state.value
        if (current.exercises.any { it.exerciseId == exerciseId }) return
        val fields = defaultActiveFields(category)
        val updated = current.exercises + ActiveExercise(
            exerciseId = exerciseId,
            exerciseNameRu = exerciseNameRu,
            activeFields = fields
        )
        _state.value = current.copy(exercises = updated)
    }

    fun removeExercise(exerciseIndex: Int) {
        val current = _state.value
        val ex = current.exercises.getOrNull(exerciseIndex) ?: return
        viewModelScope.launch {
            dao.deleteSetsForExercise(current.workoutId, ex.exerciseId)
        }
        val updated = current.exercises.toMutableList().also { it.removeAt(exerciseIndex) }
        val newIndex = current.currentExerciseIndex.coerceAtMost((updated.size - 1).coerceAtLeast(0))
        _state.value = current.copy(exercises = updated, currentExerciseIndex = newIndex)
    }

    fun goToExercise(index: Int) {
        _state.value = _state.value.copy(currentExerciseIndex = index)
    }

    fun updateExerciseRestTime(exerciseIndex: Int, seconds: Int) {
        val list = _state.value.exercises.toMutableList()
        val ex = list.getOrNull(exerciseIndex) ?: return
        list[exerciseIndex] = ex.copy(restTimeSec = seconds)
        _state.value = _state.value.copy(exercises = list)
    }

    fun updateExerciseFields(exerciseIndex: Int, fields: Set<ActiveFieldType>) {
        val list = _state.value.exercises.toMutableList()
        val ex = list.getOrNull(exerciseIndex) ?: return
        list[exerciseIndex] = ex.copy(activeFields = fields)
        _state.value = _state.value.copy(exercises = list)
    }

    // ─── Подходы ──────────────────────────────────────────────────────────────

    fun addSet(exerciseIndex: Int) {
        val list = _state.value.exercises.toMutableList()
        val ex = list.getOrNull(exerciseIndex) ?: return
        val nextNum = (ex.sets.maxOfOrNull { it.setNumber } ?: 0) + 1
        // Копируем вес/повторения из последнего подхода
        val last = ex.sets.lastOrNull()
        list[exerciseIndex] = ex.copy(
            sets = ex.sets + ActiveSet(
                setNumber = nextNum,
                weightKg = last?.weightKg ?: "",
                reps = last?.reps ?: ""
            )
        )
        _state.value = _state.value.copy(exercises = list)
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, set: ActiveSet) {
        val list = _state.value.exercises.toMutableList()
        val ex = list.getOrNull(exerciseIndex) ?: return
        val sets = ex.sets.toMutableList()
        sets[setIndex] = set
        list[exerciseIndex] = ex.copy(sets = sets)
        _state.value = _state.value.copy(exercises = list)
    }

    fun completeSet(exerciseIndex: Int, setIndex: Int) {
        val current = _state.value
        val list = current.exercises.toMutableList()
        val ex = list.getOrNull(exerciseIndex) ?: return
        val sets = ex.sets.toMutableList()
        val set = sets.getOrNull(setIndex) ?: return
        val completedSet = set.copy(isCompleted = true, isSkipped = false)
        sets[setIndex] = completedSet
        list[exerciseIndex] = ex.copy(sets = sets)
        _state.value = current.copy(exercises = list)

        // Сохраняем в Room
        persistSet(current.workoutId, ex, completedSet)

        // Запускаем таймер отдыха
        startRestTimer(ex.restTimeSec)
    }

    fun skipSet(exerciseIndex: Int, setIndex: Int) {
        val current = _state.value
        val list = current.exercises.toMutableList()
        val ex = list.getOrNull(exerciseIndex) ?: return
        val sets = ex.sets.toMutableList()
        val set = sets.getOrNull(setIndex) ?: return
        val skippedSet = set.copy(isSkipped = true, isCompleted = false)
        sets[setIndex] = skippedSet
        list[exerciseIndex] = ex.copy(sets = sets)
        _state.value = current.copy(exercises = list)
        persistSet(current.workoutId, ex, skippedSet)
    }

    private fun persistSet(workoutId: Int, ex: ActiveExercise, set: ActiveSet) {
        viewModelScope.launch {
            val entity = ActiveWorkoutSetEntity(
                id = set.localId,
                workoutId = workoutId,
                exerciseId = ex.exerciseId,
                exerciseNameRu = ex.exerciseNameRu,
                setNumber = set.setNumber,
                weightKg = set.weightKg.replace(',', '.').toFloatOrNull(),
                reps = set.reps.toIntOrNull(),
                durationSec = set.durationSec.toIntOrNull(),
                restTimeSec = ex.restTimeSec,
                isSkipped = set.isSkipped,
                completedAt = if (set.isCompleted) System.currentTimeMillis() else null
            )
            if (set.localId == 0) {
                val newId = dao.insertSet(entity).toInt()
                // Обновляем localId в state
                val current = _state.value
                val exList = current.exercises.toMutableList()
                val exIdx = exList.indexOfFirst { it.exerciseId == ex.exerciseId }
                if (exIdx >= 0) {
                    val setList = exList[exIdx].sets.toMutableList()
                    val sIdx = setList.indexOfFirst { it.setNumber == set.setNumber }
                    if (sIdx >= 0) setList[sIdx] = setList[sIdx].copy(localId = newId)
                    exList[exIdx] = exList[exIdx].copy(sets = setList)
                    _state.value = current.copy(exercises = exList)
                }
            } else {
                dao.updateSet(entity)
            }
        }
    }

    // ─── Таймеры ──────────────────────────────────────────────────────────────

    /** Вызывается когда приложение уходит в фон — останавливаем таймер тренировки */
    fun pauseWorkoutTimer() {
        elapsedTimerJob?.cancel()
        elapsedTimerJob = null
        restTimerJob?.cancel()
        restTimerJob = null
        _state.value = _state.value.copy(isRestTimerRunning = false, restRemainingSec = 0)
        // Сохраняем накопленное время в БД, чтобы при перезапуске приложения
        // таймер продолжил с того же места, а не считал время пока приложение было закрыто
        val workoutId = _state.value.workoutId
        val elapsed = _state.value.elapsedSec
        if (workoutId != 0) {
            viewModelScope.launch {
                val entity = dao.getWorkoutById(workoutId) ?: return@launch
                dao.updateWorkout(entity.copy(pausedElapsedSec = elapsed))
            }
        }
    }

    /** Вызывается когда приложение возвращается на передний план — возобновляем таймер */
    fun resumeWorkoutTimer() {
        if (_state.value.workoutId != 0 && !_state.value.isFinished && !_state.value.isDiscarded) {
            startElapsedTimer()
        }
    }

    private fun startElapsedTimer() {
        elapsedTimerJob?.cancel()
        elapsedTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _state.value = _state.value.copy(elapsedSec = _state.value.elapsedSec + 1)
            }
        }
    }

    fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _state.value = _state.value.copy(restRemainingSec = seconds, isRestTimerRunning = true)
        restTimerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _state.value = _state.value.copy(restRemainingSec = remaining)
            }
            _state.value = _state.value.copy(isRestTimerRunning = false, restRemainingSec = 0)
        }
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        _state.value = _state.value.copy(isRestTimerRunning = false, restRemainingSec = 0)
    }

    // ─── Название / заметки ───────────────────────────────────────────────────

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name)
        // Persist name to Room so it survives app restart
        val workoutId = _state.value.workoutId
        if (workoutId != 0) {
            viewModelScope.launch {
                val entity = dao.getWorkoutById(workoutId) ?: return@launch
                dao.updateWorkout(entity.copy(name = name))
            }
        }
    }
    fun updateNotes(notes: String) { _state.value = _state.value.copy(notes = notes) }

    // ─── Завершение тренировки ────────────────────────────────────────────────

    fun finishWorkout() {
        val current = _state.value
        if (current.workoutId == 0) return

        // Не сохраняем пустые тренировки (нет ни одного выполненного подхода)
        val hasCompletedSets = current.exercises.any { ex -> ex.sets.any { it.isCompleted } }
        if (!hasCompletedSets) {
            elapsedTimerJob?.cancel()
            restTimerJob?.cancel()
            viewModelScope.launch {
                if (current.workoutId != 0) dao.deleteWorkout(current.workoutId)
            }
            _state.value = ActiveWorkoutState(isDiscarded = true)
            return
        }

        elapsedTimerJob?.cancel()
        restTimerJob?.cancel()

        viewModelScope.launch {
            _state.value = current.copy(isSaving = true, saveError = null)

            // Обновляем запись в Room
            val entity = dao.getWorkoutById(current.workoutId) ?: return@launch
            dao.updateWorkout(entity.copy(finishedAt = System.currentTimeMillis(), notes = current.notes))

            // Формируем запрос к API — детализация по подходам
            val exercises = current.exercises.mapNotNull { ex ->
                val completedSets = ex.sets.filter { it.isCompleted }
                if (completedSets.isEmpty()) return@mapNotNull null
                TrainingExerciseWithSets(
                    exerciseId = ex.exerciseId,
                    sets = completedSets.map { set ->
                        TrainingSetData(
                            setNumber = set.setNumber,
                            weightKg = set.weightKg.replace(',', '.').toFloatOrNull(),
                            reps = set.reps.toIntOrNull(),
                            durationSec = set.durationSec.toIntOrNull()
                        )
                    }
                )
            }

            val request = TrainingWithSetsSaveRequest(
                name = current.name.ifBlank { "Тренировка" },
                date = LocalDate.now().toString(),
                notes = current.notes.ifBlank { null },
                exercises = exercises
            )

            api.createTrainingWithSets(request).fold(
                onSuccess = { resp ->
                    _state.value = _state.value.copy(
                        isSaving = false,
                        isFinished = true,
                        savedTrainingId = resp.id
                    )
                },
                onFailure = { e ->
                    // Тренировка сохранена локально, но API упал — всё равно показываем итоги
                    _state.value = _state.value.copy(
                        isSaving = false,
                        isFinished = true,
                        saveError = e.message
                    )
                }
            )
        }
    }

    fun discardWorkout() {
        val id = _state.value.workoutId
        elapsedTimerJob?.cancel()
        restTimerJob?.cancel()
        viewModelScope.launch {
            if (id != 0) dao.deleteWorkout(id)
        }
        _state.value = ActiveWorkoutState()
    }

    /**
     * Удаляет уже сохранённую тренировку с сервера (вызывается с экрана итогов).
     * [onDone] вызывается после завершения (успех или ошибка).
     */
    fun deleteSavedWorkout(onDone: () -> Unit) {
        val savedId = _state.value.savedTrainingId
        val localId = _state.value.workoutId
        viewModelScope.launch {
            if (savedId != 0) {
                api.deleteTraining(savedId)
            }
            if (localId != 0) dao.deleteWorkout(localId)
            _state.value = ActiveWorkoutState()
            onDone()
        }
    }

    fun resetState() {
        _state.value = ActiveWorkoutState()
    }

    // ─── История ──────────────────────────────────────────────────────────────

    private fun loadHistory() {
        viewModelScope.launch {
            api.getTrainings().onSuccess { _historyTrainings.value = it }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Суммарное количество выполненных подходов */
    fun totalCompletedSets(): Int =
        _state.value.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }

    override fun onCleared() {
        super.onCleared()
        elapsedTimerJob?.cancel()
        restTimerJob?.cancel()
    }
}
