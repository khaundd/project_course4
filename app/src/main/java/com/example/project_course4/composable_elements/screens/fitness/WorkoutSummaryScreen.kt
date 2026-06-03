package com.example.project_course4.composable_elements.screens.fitness

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.Screen
import com.example.project_course4.viewmodel.ActiveWorkoutViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val Green = Color(0xFF4CAF50)
private val LightGreen = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    navController: NavController,
    viewModel: ActiveWorkoutViewModel
) {
    val state by viewModel.state.collectAsState()
    var notes by remember { mutableStateOf(state.notes) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val dateLabel = remember {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("сегодня, HH:mm"))
    }

    val completedExercises = state.exercises.filter { ex -> ex.sets.any { it.isCompleted } }
    val skippedExercises = state.exercises.filter { ex -> ex.sets.all { it.isSkipped } && ex.sets.isNotEmpty() }
    val totalSets = state.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    val durationMin = (state.elapsedSec / 60).toInt()

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Удалить тренировку?") },
            text = { Text("Тренировка будет удалена из дневника.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    isDeleting = true
                    viewModel.deleteSavedWorkout {
                        isDeleting = false
                        navController.navigate(Screen.TrainingLog.route) {
                            popUpTo(Screen.WorkoutSummary.route) { inclusive = true }
                        }
                    }
                }) { Text("Удалить", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Отмена") }
            }
        )
    }

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
            TopAppBar(title = { Text("Итоги") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Иконка успеха
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = LightGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Green,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Тренировка завершена!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showRenameDialog = true }
                    ) {
                        Text(
                            "${state.name.ifBlank { "Тренировка" }} · $dateLabel",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Переименовать",
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                    }
                    if (state.saveError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Не удалось синхронизировать с сервером",
                            fontSize = 12.sp,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            // Статистика
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip(value = "$durationMin", label = "мин")
                    StatChip(value = "${completedExercises.size}", label = "упражнения")
                    StatChip(value = "$totalSets", label = "подходы")
                }
            }

            // Список упражнений
            item {
                SummaryCard(title = "УПРАЖНЕНИЯ") {
                    if (completedExercises.isEmpty() && skippedExercises.isEmpty()) {
                        Text("Нет выполненных упражнений", fontSize = 14.sp, color = Color.Gray)
                    }
                    completedExercises.forEach { ex ->
                        val completedSets = ex.sets.filter { it.isCompleted }
                        // Group consecutive sets with same weight+reps into "NxM @ W kg" segments
                        val segments = groupSetsIntoSegments(completedSets)
                        val detail = segments.joinToString(", ")
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ex.exerciseNameRu, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(detail, fontSize = 13.sp, color = Color.Gray)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Check, contentDescription = null, tint = Green, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    skippedExercises.forEach { ex ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(ex.exerciseNameRu, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("пропущено", fontSize = 13.sp, color = Color.Red)
                        }
                    }
                }
            }

            // Заметки
            item {
                SummaryCard(title = "ЗАМЕТКИ") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it; viewModel.updateNotes(it) },
                        placeholder = { Text("Как прошло? Что отметить...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        ),
                        minLines = 3
                    )
                }
            }

            // Кнопки
            item {
                Button(
                    onClick = {
                        viewModel.resetState()
                        navController.navigate(Screen.TrainingLog.route) {
                            popUpTo(Screen.WorkoutSummary.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    enabled = !isDeleting
                ) {
                    Text("Сохранить в дневник", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                TextButton(
                    onClick = { showDiscardDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Red, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Удалить тренировку", color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LightGreen,
        modifier = Modifier.width(90.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF2E7D32))
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun SummaryCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/**
 * Groups a list of completed sets into human-readable segments.
 * - Кардио (только duration): суммирует время всех подходов → "12м 30с"
 * - Повторы без веса: "3×12"
 * - Повторы с весом: "3×12 80 кг., 1×8 70 кг."
 */
private fun groupSetsIntoSegments(sets: List<com.example.project_course4.viewmodel.ActiveSet>): List<String> {
    if (sets.isEmpty()) return emptyList()

    val isCardio = sets.all { it.durationSec.isNotBlank() && it.durationSec != "0" && it.reps.isBlank() }
    if (isCardio) {
        val totalSec = sets.sumOf { it.durationSec.toIntOrNull() ?: 0 }
        val m = totalSec / 60; val s = totalSec % 60
        return listOf(if (m > 0) "${m}м ${s}с" else "${s}с")
    }

    data class Key(val reps: String, val weight: String, val duration: String)

    val segments = mutableListOf<String>()
    var currentKey = Key(sets[0].reps, sets[0].weightKg, sets[0].durationSec)
    var count = 1

    fun flush(key: Key, n: Int) {
        val hasDuration = key.duration.isNotBlank() && key.duration != "0"
        val hasReps = key.reps.isNotBlank() && key.reps != "0"
        val w = key.weight.replace(',', '.').toFloatOrNull()
        val hasWeight = w != null && w > 0

        val metricStr = when {
            hasReps -> key.reps
            hasDuration -> {
                val secs = key.duration.toIntOrNull() ?: 0
                val m = secs / 60; val s = secs % 60
                if (m > 0) "${m}м ${s}с" else "${s}с"
            }
            else -> "—"
        }
        val weightStr = if (hasWeight) {
            val fmt = if (w == w!!.toLong().toFloat()) w.toLong().toString() else w.toString()
            " $fmt кг."
        } else ""

        segments.add("${n}×${metricStr}${weightStr}")
    }

    for (i in 1 until sets.size) {
        val key = Key(sets[i].reps, sets[i].weightKg, sets[i].durationSec)
        if (key == currentKey) count++
        else { flush(currentKey, count); currentKey = key; count = 1 }
    }
    flush(currentKey, count)
    return segments
}
