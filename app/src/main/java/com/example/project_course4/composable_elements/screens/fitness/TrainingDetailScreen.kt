package com.example.project_course4.composable_elements.screens.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.project_course4.api.TrainingData
import com.example.project_course4.api.TrainingExerciseData
import com.example.project_course4.viewmodel.FitnessViewModel
import com.example.project_course4.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingDetailScreen(
    navController: NavController,
    viewModel: FitnessViewModel,
    trainingId: Int,
    onStartActiveWorkout: ((com.example.project_course4.api.TrainingData) -> Unit)? = null
) {
    val trainings by viewModel.trainings.collectAsState()
    val training = trainings.find { it.id == trainingId }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isNavigatingBack by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить тренировку?") },
            text = { Text("«${training?.name}» будет удалена.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    training?.let { viewModel.deleteTraining(it.id) }
                    navController.popBackStack()
                }) { Text("Удалить", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тренировка") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isNavigatingBack) {
                            isNavigatingBack = true
                            navController.popBackStack()
                        }
                    }, enabled = !isNavigatingBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Опции")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Редактировать") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    training?.let {
                                        viewModel.startEditTraining(it)
                                        navController.navigate(Screen.TrainingEditor.route)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Удалить", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                onClick = { showMenu = false; showDeleteDialog = true }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (training == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Тренировка не найдена", color = Color.Gray)
            }
            return@Scaffold
        }

        val exercisesWithSets = training.exercises.filter { it.detailedSets.isNotEmpty() }
        val totalSets = training.exercises.sumOf { it.detailedSets.size }
            .takeIf { it > 0 } ?: training.exercises.sumOf { it.sets ?: 0 }
        val exerciseCount = training.exercises.size

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            item {
                Text(
                    training.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }

            // Сводная статистика — чипы (дата / упражнения / подходы)
            if (exerciseCount > 0 || totalSets > 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        training.date?.let { dateStr ->
                            val label = runCatching {
                                java.time.LocalDate.parse(dateStr).format(
                                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yy")
                                )
                            }.getOrDefault(dateStr)
                            DetailStatChip(value = label, label = "дата")
                        }
                        if (exerciseCount > 0) DetailStatChip(value = "$exerciseCount", label = exerciseWord(exerciseCount))
                        if (totalSets > 0) DetailStatChip(value = "$totalSets", label = setWord(totalSets))
                    }
                }
            }

            // Description card
            training.description?.takeIf { it.isNotBlank() }?.let { desc ->
                item {
                    InfoCard(title = "Описание") {
                        Text(desc, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Подробная статистика по подходам (как на экране итогов)
            if (exercisesWithSets.isNotEmpty()) {
                item {
                    InfoCard(title = "Подходы") {
                        exercisesWithSets.forEachIndexed { idx, ex ->
                            if (idx > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // Название упражнения
                            Text(
                                ex.exerciseNameRu ?: ex.exerciseName ?: "Упражнение",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            )

                            // Таблица: Подход | Повт. | Вес
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Подход", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(52.dp))
                                Text("Повт.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                                Text("Вес", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                            ex.detailedSets.forEachIndexed { setIdx, set ->
                                val bgColor = if (setIdx % 2 == 0) Color.Transparent
                                              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bgColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${set.setNumber}", fontSize = 13.sp, modifier = Modifier.width(52.dp))
                                    val repsStr = set.reps?.toString()
                                        ?: set.durationSec?.let { "${it}с" }
                                        ?: "—"
                                    Text(repsStr, fontSize = 13.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                                    val weightStr = set.weightKg?.let { w ->
                                        if (w > 0) {
                                            val fmt = if (w == w.toLong().toFloat()) w.toLong().toString() else w.toString()
                                            "$fmt кг."
                                        } else "—"
                                    } ?: "—"
                                    Text(weightStr, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }
                }
            } else if (training.exercises.isNotEmpty()) {
                // Нет детальных подходов — показываем агрегированные данные со сводкой
                item {
                    InfoCard(title = "Упражнения") {
                        training.exercises.forEach { ex ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        ex.exerciseNameRu ?: ex.exerciseName ?: "Упражнение",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val lines = buildDetailLines(ex)
                                    if (lines.isNotEmpty()) {
                                        Text(
                                            lines.joinToString(", "),
                                            fontSize = 13.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Progress card
            if (training.exercises.isNotEmpty()) {
                item {
                    val allTrainings = trainings
                    InfoCard(title = "Прогресс в упражнениях") {
                        training.exercises.forEach { ex ->
                            val progress = computeProgress(ex, training, allTrainings)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    ex.exerciseNameRu ?: ex.exerciseName ?: "Упражнение",
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                when (progress) {
                                    is Progress.Up -> Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = null,
                                            tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                        Text(progress.label, fontSize = 13.sp, color = Color(0xFF4CAF50))
                                    }
                                    is Progress.Down -> Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = null,
                                            tint = Color.Red, modifier = Modifier.size(16.dp))
                                        Text(progress.label, fontSize = 13.sp, color = Color.Red)
                                    }
                                    Progress.Same -> Text("без изменений", fontSize = 13.sp, color = Color.Gray)
                                    Progress.NoData -> Text("—", fontSize = 13.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Notes card
            training.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                item {
                    InfoCard(title = "Заметки") {
                        Text(notes, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Кнопка запуска активной тренировки
            item {
                Button(
                    onClick = { onStartActiveWorkout?.invoke(training) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    enabled = onStartActiveWorkout != null
                ) {
                    Text("▶  Начать тренировку", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

private fun buildExerciseDetail(ex: TrainingExerciseData): String = buildString {
    when {
        ex.sets != null && ex.reps != null -> append("${ex.sets}×${ex.reps}")
        ex.sets != null -> append("${ex.sets} подх.")
        ex.reps != null -> append("${ex.reps} повт.")
    }
    ex.weight?.let { if (it > 0) append(" ${it} кг.") }
    ex.exerciseTime?.let { append(" ${formatTime(it)}") }
}

/**
 * Returns a list of detail strings for an exercise.
 * If detailed sets are available, groups them by weight+reps into segments.
 * Otherwise falls back to the aggregated sets/reps/weight fields.
 */
private fun buildDetailLines(ex: TrainingExerciseData): List<String> {
    if (ex.detailedSets.isNotEmpty()) {
        // Group consecutive sets with same weight+reps
        data class Key(val reps: Int?, val weight: Float?)
        val segments = mutableListOf<String>()
        var currentKey = Key(ex.detailedSets[0].reps, ex.detailedSets[0].weightKg)
        var count = 1

        fun flush(key: Key, n: Int) {
            val repsStr = key.reps?.toString() ?: "—"
            val weightStr = key.weight?.let { w ->
                if (w > 0) {
                    val fmt = if (w == w.toLong().toFloat()) w.toLong().toString() else w.toString()
                    " $fmt кг."
                } else ""
            } ?: ""
            segments.add("${n}×${repsStr}${weightStr}")
        }

        for (i in 1 until ex.detailedSets.size) {
            val key = Key(ex.detailedSets[i].reps, ex.detailedSets[i].weightKg)
            if (key == currentKey) count++
            else { flush(currentKey, count); currentKey = key; count = 1 }
        }
        flush(currentKey, count)
        return segments
    }
    // Fallback: aggregated data
    val line = buildString {
        when {
            ex.sets != null && ex.reps != null -> append("${ex.sets}×${ex.reps}")
            ex.sets != null -> append("${ex.sets} подх.")
            ex.reps != null -> append("${ex.reps} повт.")
        }
        ex.weight?.let { if (it > 0) append(" ${it} кг.") }
        ex.exerciseTime?.let { append(" ${formatTime(it)}") }
    }
    return if (line.isNotBlank()) listOf(line) else emptyList()
}

private sealed class Progress {
    data class Up(val label: String) : Progress()
    data class Down(val label: String) : Progress()
    object Same : Progress()
    object NoData : Progress()
}

private fun computeProgress(
    ex: TrainingExerciseData,
    currentTraining: TrainingData,
    allTrainings: List<TrainingData>
): Progress {
    // Ищем предыдущую тренировку с этим упражнением
    val previous = allTrainings
        .filter { it.id != currentTraining.id }
        .sortedByDescending { it.date ?: "" }
        .firstOrNull { t -> t.exercises.any { it.exerciseId == ex.exerciseId } }
        ?: return Progress.NoData

    val prevEx = previous.exercises.firstOrNull { it.exerciseId == ex.exerciseId }
        ?: return Progress.NoData

    // Определяем метрику по наличию данных в текущем упражнении
    val hasWeight = (ex.detailedSets.any { (it.weightKg ?: 0f) > 0f }) ||
                    (ex.weight != null && ex.weight > 0f)
    val hasReps   = (ex.detailedSets.any { (it.reps ?: 0) > 0 }) ||
                    (ex.reps != null && ex.reps > 0)
    val hasDuration = (ex.detailedSets.any { (it.durationSec ?: 0) > 0 }) ||
                      (ex.exerciseTime != null && ex.exerciseTime > 0)

    return when {
        // Силовые — сравниваем максимальный вес подхода
        hasWeight -> {
            val curMax = if (ex.detailedSets.isNotEmpty())
                ex.detailedSets.mapNotNull { it.weightKg }.maxOrNull() ?: 0f
            else ex.weight ?: 0f

            val prevMax = if (prevEx.detailedSets.isNotEmpty())
                prevEx.detailedSets.mapNotNull { it.weightKg }.maxOrNull() ?: 0f
            else prevEx.weight ?: return Progress.NoData

            if (prevMax == 0f) return Progress.NoData
            val diff = curMax - prevMax
            val fmt = { v: Float -> if (v == v.toLong().toFloat()) "${v.toLong()} кг." else "$v кг." }
            when {
                diff > 0f -> Progress.Up("+${fmt(diff)}")
                diff < 0f -> Progress.Down("${fmt(diff)}")
                else -> Progress.Same
            }
        }

        // Кардио — сравниваем суммарное время
        hasDuration -> {
            val curSec = if (ex.detailedSets.isNotEmpty())
                ex.detailedSets.sumOf { it.durationSec ?: 0 }
            else ex.exerciseTime ?: 0

            val prevSec = if (prevEx.detailedSets.isNotEmpty())
                prevEx.detailedSets.sumOf { it.durationSec ?: 0 }
            else prevEx.exerciseTime ?: return Progress.NoData

            if (prevSec == 0) return Progress.NoData
            val diff = curSec - prevSec
            fun fmtSec(s: Int): String {
                val m = s / 60; val sec = s % 60
                return if (m > 0) "${m}м ${sec}с" else "${sec}с"
            }
            when {
                diff > 0 -> Progress.Up("+${fmtSec(diff)}")
                diff < 0 -> Progress.Down(fmtSec(diff))
                else -> Progress.Same
            }
        }

        // Плиометрика — сравниваем суммарные повторы
        hasReps -> {
            val curReps = if (ex.detailedSets.isNotEmpty())
                ex.detailedSets.sumOf { it.reps ?: 0 }
            else (ex.sets ?: 1) * (ex.reps ?: 0)

            val prevReps = if (prevEx.detailedSets.isNotEmpty())
                prevEx.detailedSets.sumOf { it.reps ?: 0 }
            else (prevEx.sets ?: 1) * (prevEx.reps ?: 0)

            if (prevReps == 0) return Progress.NoData
            val diff = curReps - prevReps
            when {
                diff > 0 -> Progress.Up("+$diff повт.")
                diff < 0 -> Progress.Down("$diff повт.")
                else -> Progress.Same
            }
        }

        else -> Progress.NoData
    }
}

@Composable
private fun DetailStatChip(value: String, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE8F5E9),
        modifier = Modifier.widthIn(min = 80.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2E7D32))
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

/**
 * Группирует детальные подходы в сегменты вида "3×12 80 кг., 1×8 70 кг."
 */
private fun groupDetailedSetsIntoSegments(sets: List<com.example.project_course4.api.TrainingSetData>): List<String> {
    if (sets.isEmpty()) return emptyList()
    data class Key(val reps: Int?, val duration: Int?, val weight: Float?)

    val segments = mutableListOf<String>()
    var currentKey = Key(sets[0].reps, sets[0].durationSec, sets[0].weightKg)
    var count = 1

    fun flush(key: Key, n: Int) {
        val repsStr = key.reps?.toString() ?: key.duration?.let { "${it}с" } ?: "—"
        val w = key.weight
        val weightStr = if (w != null && w > 0) {
            val fmt = if (w == w.toLong().toFloat()) w.toLong().toString() else w.toString()
            " $fmt кг."
        } else ""
        segments.add("${n}×${repsStr}${weightStr}")
    }

    for (i in 1 until sets.size) {
        val key = Key(sets[i].reps, sets[i].durationSec, sets[i].weightKg)
        if (key == currentKey) count++
        else { flush(currentKey, count); currentKey = key; count = 1 }
    }
    flush(currentKey, count)
    return segments
}

private fun setWord(n: Int) = when {
    n % 100 in 11..19 -> "подходов"
    n % 10 == 1 -> "подход"
    n % 10 in 2..4 -> "подхода"
    else -> "подходов"
}

private fun exerciseWord(n: Int) = when {
    n % 100 in 11..19 -> "упражнений"
    n % 10 == 1 -> "упражнение"
    n % 10 in 2..4 -> "упражнения"
    else -> "упражнений"
}
