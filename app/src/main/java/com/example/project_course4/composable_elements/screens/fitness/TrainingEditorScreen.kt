package com.example.project_course4.composable_elements.screens.fitness

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_course4.Screen
import androidx.navigation.NavController
import com.example.project_course4.composable_elements.CustomButton
import com.example.project_course4.composable_elements.TransparentTextField
import com.example.project_course4.composable_elements.dialogs.TimePickerDialog
import com.example.project_course4.viewmodel.ExerciseFieldType
import com.example.project_course4.viewmodel.FitnessViewModel
import com.example.project_course4.viewmodel.TrainingEditorExercise
import com.example.project_course4.viewmodel.TrainingEditorSet
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingEditorScreen(
    navController: NavController,
    viewModel: FitnessViewModel
) {
    val state by viewModel.trainingEditor.collectAsState()
    var isNavigatingBack by remember { mutableStateOf(false) }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            viewModel.resetTrainingEditorSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.name.ifBlank { "" },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isNavigatingBack) {
                                isNavigatingBack = true
                                val navigated = navController.navigateUp()
                                if (!navigated) {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        },
                        enabled = !isNavigatingBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp)
                        )
                    } else {
                        IconButton(onClick = { viewModel.saveTraining() }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Сохранить",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Название тренировки
            item {
                TransparentTextField(
                    value = state.name,
                    onValueChange = { viewModel.updateTrainingName(it) },
                    placeholder = "Название тренировки...",
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.error != null && state.name.isBlank(),
                    errorMessage = if (state.name.isBlank()) state.error else null
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
            }

            // Описание
            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.updateTrainingDescription(it) },
                    placeholder = { Text("Описание тренировки...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text("Описание") }
                )
            }

            // Заметки
            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.updateTrainingNotes(it) },
                    placeholder = { Text("Заметки...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text("Заметки") }
                )
            }

            // Выбор даты
            item {
                DatePickerField(
                    date = state.date,
                    onDateSelected = { viewModel.updateTrainingDate(it) }
                )
            }

            // Заголовок "Упражнения"
            item {
                Text(
                    text = "Упражнения",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Ошибка (не связанная с именем)
            state.error?.takeIf { state.name.isNotBlank() }?.let { err ->
                item {
                    Text(err, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }

            if (state.exercises.isEmpty()) {
                item {
                    Text(
                        text = "Добавьте упражнения в тренировку",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            itemsIndexed(state.exercises) { index, ex ->
                TrainingExerciseCard(
                    exercise = ex,
                    onUpdate = { viewModel.updateTrainingExercise(index, it) },
                    onDelete = { viewModel.removeExerciseFromTraining(index) }
                )
            }

            // Кнопка "Добавить упражнение"
            item {
                CustomButton(
                    text = "+ Добавить упражнение",
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    cornerRadius = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    onClick = { navController.navigate("exerciseCatalogSelect") }
                )
            }
        }
    }
}

@Composable
private fun DatePickerField(
    date: java.time.LocalDate,
    onDateSelected: (java.time.LocalDate) -> Unit
) {
    val context = LocalContext.current
    val dateText = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

    OutlinedTextField(
        value = dateText,
        onValueChange = {},
        label = { Text("Дата тренировки") },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = {
                val cal = Calendar.getInstance().apply {
                    set(date.year, date.monthValue - 1, date.dayOfMonth)
                }
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onDateSelected(java.time.LocalDate.of(year, month + 1, day))
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Выбрать дату",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        supportingText = { Text("ДД.ММ.ГГГГ") },
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun TrainingExerciseCard(
    exercise: TrainingEditorExercise,
    onUpdate: (TrainingEditorExercise) -> Unit,
    onDelete: () -> Unit
) {
    var showFieldPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Заголовок карточки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.exerciseNameRu,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить",
                        tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            if (exercise.hasDetailedSets) {
                // ── Режим детальных подходов ──────────────────────────────────
                val focusManager = LocalFocusManager.current

                // Заголовок таблицы
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Подход", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(44.dp))
                    Text("Повт.", fontSize = 11.sp, color = Color.Gray,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Вес (кг)", fontSize = 11.sp, color = Color.Gray,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Spacer(Modifier.width(32.dp)) // место под кнопку удаления
                }

                exercise.detailedSets.forEachIndexed { idx, set ->
                    val bgColor = if (idx % 2 == 0) Color.Transparent
                                  else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Номер подхода
                        Text(
                            "${idx + 1}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(44.dp),
                            textAlign = TextAlign.Center
                        )
                        // Повторения
                        OutlinedTextField(
                            value = set.reps,
                            onValueChange = { v ->
                                if (v.all { it.isDigit() }) {
                                    val updated = exercise.detailedSets.toMutableList()
                                    updated[idx] = set.copy(reps = v)
                                    onUpdate(exercise.copy(detailedSets = updated))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center, fontSize = 13.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                            )
                        )
                        // Вес
                        OutlinedTextField(
                            value = set.weightKg,
                            onValueChange = { v ->
                                val normalized = v.replace(',', '.')
                                if (normalized.matches(Regex("\\d*\\.?\\d*"))) {
                                    val updated = exercise.detailedSets.toMutableList()
                                    updated[idx] = set.copy(weightKg = v)
                                    onUpdate(exercise.copy(detailedSets = updated))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center, fontSize = 13.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                            )
                        )
                        // Удалить подход (только если больше одного)
                        IconButton(
                            onClick = {
                                if (exercise.detailedSets.size > 1) {
                                    val updated = exercise.detailedSets.toMutableList()
                                    updated.removeAt(idx)
                                    onUpdate(exercise.copy(detailedSets = updated))
                                }
                            },
                            modifier = Modifier.size(32.dp),
                            enabled = exercise.detailedSets.size > 1
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить подход",
                                tint = if (exercise.detailedSets.size > 1) Color.Red.copy(alpha = 0.7f)
                                       else Color.Gray.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Кнопки: добавить подход + переключиться обратно в агрегированный режим
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val last = exercise.detailedSets.lastOrNull()
                            val updated = exercise.detailedSets + TrainingEditorSet(
                                setNumber = exercise.detailedSets.size + 1,
                                weightKg = last?.weightKg ?: "",
                                reps = last?.reps ?: ""
                            )
                            onUpdate(exercise.copy(detailedSets = updated))
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Добавить подход", fontSize = 13.sp)
                    }
                    TextButton(onClick = {
                        // Переключиться обратно в агрегированный режим
                        val setsCount = exercise.detailedSets.size
                        val avgReps = exercise.detailedSets.mapNotNull { it.reps.toIntOrNull() }
                            .let { if (it.isEmpty()) "" else (it.sum() / it.size).toString() }
                        val maxWeight = exercise.detailedSets.mapNotNull { it.weightKg.replace(',', '.').toFloatOrNull() }
                            .maxOrNull()?.let {
                                if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString()
                            } ?: ""
                        onUpdate(exercise.copy(
                            detailedSets = emptyList(),
                            sets = setsCount.toString(),
                            reps = avgReps,
                            weight = maxWeight
                        ))
                    }) {
                        Text("Свернуть", fontSize = 12.sp, color = Color.Gray)
                    }
                }

            } else {
                // ── Режим агрегированных полей ────────────────────────────────
                val fields = exercise.activeFields
                val hasTime = ExerciseFieldType.TIME in fields
                val hasSets = ExerciseFieldType.SETS in fields
                val hasReps = ExerciseFieldType.REPS in fields
                val hasWeight = ExerciseFieldType.WEIGHT in fields

                if (hasTime) {
                    TimeField(
                        label = "Время",
                        value = exercise.exerciseTime,
                        onValueChange = { onUpdate(exercise.copy(exerciseTime = it)) }
                    )
                }

                if (hasSets || hasReps || hasWeight) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hasSets) {
                            OutlinedTextField(
                                value = exercise.sets,
                                onValueChange = { v ->
                                    if (v.all { it.isDigit() }) onUpdate(exercise.copy(sets = v))
                                },
                                label = { Text("Подходы", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        if (hasReps) {
                            OutlinedTextField(
                                value = exercise.reps,
                                onValueChange = { v ->
                                    if (v.all { it.isDigit() }) onUpdate(exercise.copy(reps = v))
                                },
                                label = { Text("Повторы", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        if (hasWeight) {
                            OutlinedTextField(
                                value = exercise.weight,
                                onValueChange = { v ->
                                    val normalized = v.replace(',', '.')
                                    if (normalized.matches(Regex("\\d*\\.?\\d*")))
                                        onUpdate(exercise.copy(weight = v))
                                },
                                label = { Text("Вес (кг)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка "Детальные подходы" — только если активны ровно подходы+повторы+вес (без времени)
                    if (hasSets && hasReps && hasWeight && !hasTime) {
                        TextButton(onClick = {
                            val setsCount = exercise.sets.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            val initialSets = (1..setsCount).map { n ->
                                TrainingEditorSet(
                                    setNumber = n,
                                    reps = exercise.reps,
                                    weightKg = exercise.weight
                                )
                            }
                            onUpdate(exercise.copy(detailedSets = initialSets))
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Детальные подходы", fontSize = 13.sp)
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }

                    // Кнопка "Настроить"
                    Box {
                        TextButton(onClick = { showFieldPicker = true }) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Настроить", fontSize = 13.sp)
                        }
                        if (showFieldPicker) {
                            FieldPickerDropdown(
                                activeFields = exercise.activeFields,
                                onDismiss = { showFieldPicker = false },
                                onToggle = { field ->
                                    val current = exercise.activeFields.toMutableSet()
                                    if (field in current && current.size > 1) current.remove(field)
                                    else current.add(field)
                                    onUpdate(exercise.copy(activeFields = current))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    val displayTime = if (value.isBlank()) "" else value

    OutlinedTextField(
        value = displayTime,
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        placeholder = { Text("00:00") },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Выбрать время",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp)
    )

    if (showPicker) {
        val parts = value.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            initialTime = LocalTime.of(h, m),
            onTimeSelected = { time ->
                onValueChange(String.format(Locale.getDefault(), "%02d:%02d", time.hour, time.minute))
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun FieldPickerDropdown(
    activeFields: Set<ExerciseFieldType>,
    onDismiss: () -> Unit,
    onToggle: (ExerciseFieldType) -> Unit
) {
    val allFields = listOf(
        ExerciseFieldType.SETS to "Подходы",
        ExerciseFieldType.REPS to "Повторы",
        ExerciseFieldType.WEIGHT to "Вес (кг)",
        ExerciseFieldType.TIME to "Время"
    )

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        allFields.forEach { (field, label) ->
            val isActive = field in activeFields
            DropdownMenuItem(
                text = { Text(label) },
                onClick = { onToggle(field) },
                trailingIcon = {
                    if (isActive) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
    }
}
