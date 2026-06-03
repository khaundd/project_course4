package com.example.project_course4.composable_elements.screens.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.project_course4.Screen
import com.example.project_course4.api.TrainingData
import com.example.project_course4.viewmodel.ActiveExercise
import com.example.project_course4.viewmodel.ActiveFieldType
import com.example.project_course4.viewmodel.ActiveSet
import com.example.project_course4.viewmodel.ActiveWorkoutViewModel

private val Green = Color(0xFF4CAF50)
private val LightGreen = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    navController: NavController,
    viewModel: ActiveWorkoutViewModel,
    // Для добавления упражнений используем уже существующий ExerciseCatalogScreen в режиме выбора
    onAddExercise: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val historyTrainings by viewModel.historyTrainings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showRestTimerDialog by remember { mutableStateOf(false) }
    var editingRestExerciseIndex by remember { mutableIntStateOf(-1) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showEmptyWorkoutDialog by remember { mutableStateOf(false) }

    // Переход на экран итогов после завершения
    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            navController.navigate(Screen.WorkoutSummary.route) {
                popUpTo(Screen.ActiveWorkout.route) { inclusive = true }
            }
        }
    }

    // Пустая тренировка — автоматически удалена, возвращаемся назад
    LaunchedEffect(state.isDiscarded) {
        if (state.isDiscarded) {
            viewModel.resetState()
            navController.popBackStack()
        }
    }

    // Останавливаем таймер при сворачивании/закрытии приложения, возобновляем при возврате
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.pauseWorkoutTimer()
                Lifecycle.Event.ON_RESUME -> viewModel.resumeWorkoutTimer()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Диалог: тренировка пустая
    if (showEmptyWorkoutDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyWorkoutDialog = false },
            title = { Text("Нет выполненных подходов") },
            text = { Text("Завершите хотя бы один подход, чтобы сохранить тренировку.") },
            confirmButton = {
                TextButton(onClick = { showEmptyWorkoutDialog = false }) { Text("Понятно") }
            }
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Завершить тренировку?") },
            text = { Text("Будут сохранены только выполненные подходы.") },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        val hasCompleted = state.exercises.any { ex -> ex.sets.any { it.isCompleted } }
                        if (!hasCompleted) {
                            showEmptyWorkoutDialog = true
                        } else {
                            viewModel.finishWorkout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) { Text("Завершить") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Отменить тренировку?") },
            text = { Text("Все данные текущей тренировки будут удалены.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.discardWorkout()
                    navController.popBackStack()
                }) { Text("Удалить", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Диалог настройки таймера отдыха для упражнения
    if (showRestTimerDialog && editingRestExerciseIndex >= 0) {
        val ex = state.exercises.getOrNull(editingRestExerciseIndex)
        if (ex != null) {
            RestTimerDialog(
                currentSeconds = ex.restTimeSec,
                onConfirm = { secs ->
                    viewModel.updateExerciseRestTime(editingRestExerciseIndex, secs)
                    showRestTimerDialog = false
                },
                onDismiss = { showRestTimerDialog = false }
            )
        }
    }

    // Диалог переименования тренировки
    if (showRenameDialog) {
        var nameInput by remember { mutableStateOf(state.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Название тренировки") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    label = { Text("Название") },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateName(nameInput.trim().ifBlank { "Тренировка" })
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable { showRenameDialog = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                state.name.ifBlank { "Тренировка" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Переименовать",
                                modifier = Modifier.size(14.dp),
                                tint = Color.Gray
                            )
                        }
                        Text("Активная тренировка", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showDiscardDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Отмена")
                    }
                },
                actions = {
                    Button(
                        onClick = { showFinishDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCDD2)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Завершить", color = Color.Red, fontSize = 13.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Таймеры
            TimerRow(
                elapsedSec = state.elapsedSec,
                restRemaining = state.restRemainingSec,
                isRestRunning = state.isRestTimerRunning,
                onStopRest = { viewModel.stopRestTimer() }
            )

            // Вкладки
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Green
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Упражнения") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("История") }
                )
            }

            when (selectedTab) {
                0 -> ExercisesTab(
                    state_exercises = state.exercises,
                    currentIndex = state.currentExerciseIndex,
                    isSaving = state.isSaving,
                    onGoTo = { viewModel.goToExercise(it) },
                    onAddSet = { viewModel.addSet(it) },
                    onUpdateSet = { exIdx, setIdx, set -> viewModel.updateSet(exIdx, setIdx, set) },
                    onCompleteSet = { exIdx, setIdx -> viewModel.completeSet(exIdx, setIdx) },
                    onSkipExercise = { exIdx ->
                        val ex = state.exercises.getOrNull(exIdx) ?: return@ExercisesTab
                        ex.sets.forEachIndexed { sIdx, _ -> viewModel.skipSet(exIdx, sIdx) }
                        if (exIdx < state.exercises.size - 1) viewModel.goToExercise(exIdx + 1)
                    },
                    onNextExercise = { exIdx ->
                        if (exIdx < state.exercises.size - 1) viewModel.goToExercise(exIdx + 1)
                    },
                    onEditRestTimer = { exIdx ->
                        editingRestExerciseIndex = exIdx
                        showRestTimerDialog = true
                    },
                    onRemoveExercise = { viewModel.removeExercise(it) },
                    onUpdateFields = { exIdx, fields -> viewModel.updateExerciseFields(exIdx, fields) },
                    onAddExercise = onAddExercise
                )
                1 -> HistoryTab(
                    exercises = state.exercises,
                    historyTrainings = historyTrainings
                )
            }
        }
    }
}

// ─── Таймеры ─────────────────────────────────────────────────────────────────

@Composable
private fun TimerRow(
    elapsedSec: Long,
    restRemaining: Int,
    isRestRunning: Boolean,
    onStopRest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("ВРЕМЯ", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(
                formatElapsed(elapsedSec),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (isRestRunning) {
            Column(horizontalAlignment = Alignment.End) {
                Text("ОТДЫХ", fontSize = 11.sp, color = Green, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatElapsed(restRemaining.toLong()),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onStopRest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Стоп", tint = Green, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
    HorizontalDivider()
}

// ─── Вкладка упражнений ───────────────────────────────────────────────────────

@Composable
private fun ExercisesTab(
    state_exercises: List<ActiveExercise>,
    currentIndex: Int,
    isSaving: Boolean,
    onGoTo: (Int) -> Unit,
    onAddSet: (Int) -> Unit,
    onUpdateSet: (Int, Int, ActiveSet) -> Unit,
    onCompleteSet: (Int, Int) -> Unit,
    onSkipExercise: (Int) -> Unit,
    onNextExercise: (Int) -> Unit,
    onEditRestTimer: (Int) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onUpdateFields: (Int, Set<ActiveFieldType>) -> Unit,
    onAddExercise: () -> Unit
) {
    if (isSaving) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Green)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        itemsIndexed(state_exercises) { exIdx, exercise ->
            val isActive = exIdx == currentIndex
            val isDone = exercise.sets.all { it.isCompleted || it.isSkipped }
            ExerciseCard(
                exercise = exercise,
                exerciseIndex = exIdx,
                isActive = isActive,
                isDone = isDone,
                isLast = exIdx == state_exercises.size - 1,
                onActivate = { onGoTo(exIdx) },
                onAddSet = { onAddSet(exIdx) },
                onUpdateSet = { sIdx, set -> onUpdateSet(exIdx, sIdx, set) },
                onCompleteSet = { sIdx -> onCompleteSet(exIdx, sIdx) },
                onSkip = { onSkipExercise(exIdx) },
                onNext = { onNextExercise(exIdx) },
                onEditRestTimer = { onEditRestTimer(exIdx) },
                onRemove = { onRemoveExercise(exIdx) },
                onUpdateFields = { fields -> onUpdateFields(exIdx, fields) }
            )
        }

        item {
            TextButton(
                onClick = onAddExercise,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Green)
                Spacer(Modifier.width(4.dp))
                Text("Добавить упражнение", color = Green)
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ActiveExercise,
    exerciseIndex: Int,
    isActive: Boolean,
    isDone: Boolean,
    isLast: Boolean,
    onActivate: () -> Unit,
    onAddSet: () -> Unit,
    onUpdateSet: (Int, ActiveSet) -> Unit,
    onCompleteSet: (Int) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onEditRestTimer: () -> Unit,
    onRemove: () -> Unit,
    onUpdateFields: (Set<ActiveFieldType>) -> Unit
) {
    val borderColor = when {
        isActive -> Green
        isDone -> Color(0xFFBDBDBD)
        else -> Color.Transparent
    }
    val bgColor = when {
        isActive -> LightGreen
        else -> MaterialTheme.colorScheme.surface
    }

    var showFieldPicker by remember { mutableStateOf(false) }
    val fields = exercise.activeFields
    val hasWeight = ActiveFieldType.WEIGHT in fields
    val hasReps = ActiveFieldType.REPS in fields
    val hasDuration = ActiveFieldType.DURATION in fields

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(
                width = if (isActive) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isActive) { onActivate() },
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isActive) 0.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Заголовок упражнения
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDone) Color(0xFFBDBDBD) else if (isActive) Green else Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("${exerciseIndex + 1}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    exercise.exerciseNameRu,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isActive) {
                    val label = when {
                        isDone -> "выполнено"
                        exerciseIndex > 0 -> "далее"
                        else -> ""
                    }
                    if (label.isNotEmpty()) {
                        Text(label, fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Подходы (только для активного)
            if (isActive) {
                Spacer(Modifier.height(10.dp))

                // Заголовок таблицы + кнопка настройки метрик
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Подход", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(56.dp))
                    if (hasWeight) Text("кг", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                    if (hasReps) Text("повт", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                    if (hasDuration) Text("сек", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                    Spacer(Modifier.weight(1f))
                    // Кнопка настройки метрик
                    Box {
                        IconButton(onClick = { showFieldPicker = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Settings, contentDescription = "Метрики", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        if (showFieldPicker) {
                            DropdownMenu(expanded = true, onDismissRequest = { showFieldPicker = false }) {
                                listOf(
                                    ActiveFieldType.WEIGHT to "Вес (кг)",
                                    ActiveFieldType.REPS to "Повторения",
                                    ActiveFieldType.DURATION to "Время (сек)"
                                ).forEach { (field, label) ->
                                    val isChecked = field in fields
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            val updated = fields.toMutableSet()
                                            if (isChecked && updated.size > 1) updated.remove(field)
                                            else updated.add(field)
                                            onUpdateFields(updated)
                                        },
                                        trailingIcon = {
                                            if (isChecked) Icon(Icons.Default.Check, contentDescription = null,
                                                tint = Green, modifier = Modifier.size(18.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                exercise.sets.forEachIndexed { sIdx, set ->
                    SetRow(
                        set = set,
                        setIndex = sIdx,
                        hasWeight = hasWeight,
                        hasReps = hasReps,
                        hasDuration = hasDuration,
                        onUpdate = { onUpdateSet(sIdx, it) },
                        onComplete = { onCompleteSet(sIdx) }
                    )
                }

                TextButton(
                    onClick = onAddSet,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Green, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ добавить подход", color = Green, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditRestTimer() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Отдых: ${formatSeconds(exercise.restTimeSec)}", fontSize = 13.sp, color = Color.Gray)
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp).padding(start = 2.dp))
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text("Пропустить")
                    }
                    if (!isLast) {
                        Button(
                            onClick = onNext,
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Green)
                        ) { Text("Следующее →") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetRow(
    set: ActiveSet,
    setIndex: Int,
    hasWeight: Boolean,
    hasReps: Boolean,
    hasDuration: Boolean,
    onUpdate: (ActiveSet) -> Unit,
    onComplete: () -> Unit
) {
    val bgColor = when {
        set.isCompleted -> Color(0xFFE8F5E9)
        set.isSkipped -> Color(0xFFF5F5F5)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Подх. ${set.setNumber}",
            fontSize = 13.sp,
            modifier = Modifier.width(56.dp),
            color = if (set.isSkipped) Color.Gray else Color.Unspecified
        )

        if (hasWeight) {
            SetTextField(
                value = set.weightKg,
                placeholder = "кг",
                enabled = !set.isCompleted && !set.isSkipped,
                modifier = Modifier.width(72.dp)
            ) { onUpdate(set.copy(weightKg = it)) }
        }

        if (hasReps) {
            SetTextField(
                value = set.reps,
                placeholder = "повт",
                enabled = !set.isCompleted && !set.isSkipped,
                modifier = Modifier.width(72.dp)
            ) { onUpdate(set.copy(reps = it)) }
        }

        if (hasDuration) {
            SetTextField(
                value = set.durationSec,
                placeholder = "сек",
                enabled = !set.isCompleted && !set.isSkipped,
                modifier = Modifier.width(72.dp)
            ) { onUpdate(set.copy(durationSec = it)) }
        }

        Spacer(Modifier.weight(1f))

        IconButton(
            onClick = onComplete,
            enabled = !set.isCompleted && !set.isSkipped,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Выполнено",
                tint = if (set.isCompleted) Green else Color(0xFFBDBDBD),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SetTextField(
    value: String,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ),
        modifier = modifier,
        decorationBox = { innerTextField ->
            val borderColor = if (enabled) Color(0xFFE0E0E0) else Color(0xFFEEEEEE)
            Box(
                modifier = Modifier
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                innerTextField()
            }
        }
    )
}

// ─── Вкладка истории ──────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(
    exercises: List<ActiveExercise>,
    historyTrainings: List<TrainingData>
) {
    if (exercises.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет упражнений", color = Color.Gray)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(exercises) { _, exercise ->
            val history = historyTrainings
                .sortedByDescending { it.date ?: "" }
                .mapNotNull { training ->
                    val ex = training.exercises.firstOrNull { it.exerciseId == exercise.exerciseId }
                    if (ex != null) Pair(training, ex) else null
                }
                .take(5)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(exercise.exerciseNameRu, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    if (history.isEmpty()) {
                        Text("Нет истории", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        history.forEach { (training, ex) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    training.date ?: "—",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                                val detail = buildString {
                                    if (ex.sets != null && ex.reps != null) append("${ex.sets}×${ex.reps}")
                                    else if (ex.sets != null) append("${ex.sets} подх.")
                                    ex.weight?.let { if (it > 0) append("  ${it} кг") }
                                }
                                Text(detail, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Диалог таймера отдыха ────────────────────────────────────────────────────

@Composable
private fun RestTimerDialog(
    currentSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(30, 60, 90, 120, 180, 240, 300)
    var selected by remember { mutableIntStateOf(currentSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Время отдыха") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { secs ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = secs }
                            .background(if (selected == secs) LightGreen else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == secs,
                            onClick = { selected = secs },
                            colors = RadioButtonDefaults.colors(selectedColor = Green)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(formatSeconds(secs), fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) { Text("Применить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun formatElapsed(totalSec: Long): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}

private fun formatSeconds(secs: Int): String {
    val m = secs / 60
    val s = secs % 60
    return if (m > 0 && s == 0) "${m} мин" else if (m > 0) "${m} мин ${s} сек" else "${s} сек"
}
