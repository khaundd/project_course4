package com.example.project_course4.composable_elements.screens.fitness

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.composable_elements.TransparentTextField
import com.example.project_course4.viewmodel.FitnessViewModel
import com.example.project_course4.viewmodel.TrainingPlanEditorExercise
import com.example.project_course4.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlanEditorScreen(
    navController: NavController,
    viewModel: FitnessViewModel
) {
    val state by viewModel.planEditor.collectAsState()
    val isLoadingDetail by viewModel.isLoadingPlanDetail.collectAsState()
    var isNavigatingBack by remember { mutableStateOf(false) }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            viewModel.resetPlanEditorSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.planId == 0) "Создание плана тренировок" else "Редактирование плана тренировок",
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isNavigatingBack) {
                                isNavigatingBack = true
                                val navigated = navController.navigateUp()
                                if (!navigated) navController.navigate(Screen.Main.route) {
                                    popUpTo(0) { inclusive = true }
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
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp))
                    } else {
                        IconButton(onClick = { viewModel.saveTrainingPlan() }) {
                            Icon(Icons.Default.Check, contentDescription = "Сохранить",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoadingDetail) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Plan name
            item {
                TransparentTextField(
                    value = state.name,
                    onValueChange = { viewModel.updatePlanName(it) },
                    placeholder = "Название плана...",
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
            }

            // Description
            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.updatePlanDescription(it) },
                    placeholder = { Text("Описание плана...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Error
            state.error?.let { err ->
                item { Text(err, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }

            // Days header
            item {
                Text(
                    "Тренировочные дни",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }

            // Day cards
            itemsIndexed(state.days) { dayIndex, day ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Day header: name + copy + delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = day.dayName,
                                onValueChange = { viewModel.updatePlanDayName(dayIndex, it) },
                                placeholder = { Text("Название дня...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            )
                            // Copy day
                            IconButton(onClick = { viewModel.copyPlanDay(dayIndex) }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Копировать день",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Delete day
                            if (state.days.size > 1) {
                                IconButton(onClick = { viewModel.removePlanDay(dayIndex) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить день",
                                        tint = Color.Red, modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Notes field
                        OutlinedTextField(
                            value = day.notes,
                            onValueChange = { viewModel.updatePlanDayNotes(dayIndex, it) },
                            placeholder = { Text("Заметка...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        // Exercises
                        if (day.exercises.isEmpty()) {
                            Text("Нет упражнений", fontSize = 13.sp, color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            day.exercises.forEachIndexed { exIndex, ex ->
                                PlanExerciseEditor(
                                    exercise = ex,
                                    onUpdate = { viewModel.updatePlanDayExercise(dayIndex, exIndex, it) },
                                    onDelete = { viewModel.removeExerciseFromPlanDay(dayIndex, exIndex) }
                                )
                                if (exIndex < day.exercises.lastIndex) Spacer(Modifier.height(8.dp))
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Add exercise button
                        OutlinedButton(
                            onClick = {
                                viewModel.setPendingPlanDayIndex(dayIndex)
                                navController.navigate("exerciseCatalogSelectPlan/$dayIndex")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("+ Добавить упражнение")
                        }
                    }
                }
            }

            // Add day button
            item {
                Button(
                    onClick = { viewModel.addPlanDay() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("+ Добавить день")
                }
            }

            // Public switch
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Скрыть план для других", fontSize = 15.sp)
                    Switch(
                        checked = !state.isPublic,
                        onCheckedChange = { viewModel.updatePlanPublic(!it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF7C4DFF),
                            checkedTrackColor = Color(0xFFB39DDB)
                        )
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PlanExerciseEditor(
    exercise: TrainingPlanEditorExercise,
    onUpdate: (TrainingPlanEditorExercise) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "*${exercise.exerciseNameRu}*",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null,
                        tint = Color.Red, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            if (exercise.isTimeBased) {
                OutlinedTextField(
                    value = exercise.exerciseTime,
                    onValueChange = { onUpdate(exercise.copy(exerciseTime = it)) },
                    label = { Text("Время (сек)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = exercise.sets,
                        onValueChange = { onUpdate(exercise.copy(sets = it)) },
                        label = { Text("Подходы", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = exercise.reps,
                        onValueChange = { onUpdate(exercise.copy(reps = it)) },
                        label = { Text("Повторы", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = exercise.weight,
                        onValueChange = { onUpdate(exercise.copy(weight = it)) },
                        label = { Text("Вес (кг)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
            // Configure button (time/sets toggle)
            Box(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { onUpdate(exercise.copy(isTimeBased = !exercise.isTimeBased)) },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Настроить", fontSize = 13.sp)
                }
            }
        }
    }
}
