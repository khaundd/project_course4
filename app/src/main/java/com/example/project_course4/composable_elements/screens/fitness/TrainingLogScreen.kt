package com.example.project_course4.composable_elements.screens.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.project_course4.api.TrainingData
import com.example.project_course4.api.TrainingPlanData
import com.example.project_course4.composable_elements.dialogs.DateRangePickerDialog
import com.example.project_course4.viewmodel.FitnessViewModel
import com.example.project_course4.Screen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingLogScreen(
    navController: NavController,
    viewModel: FitnessViewModel,
    drawerState: DrawerState? = null,
    onStartActiveWorkout: (() -> Unit)? = null,
    activeWorkoutName: String? = null,
    onResumeWorkout: (() -> Unit)? = null
) {
    val trainings by viewModel.trainings.collectAsState()
    val isLoading by viewModel.isLoadingTrainings.collectAsState()
    val isDeletingTraining by viewModel.isDeletingTraining.collectAsState()
    val trainingPlans by viewModel.trainingPlans.collectAsState()
    val scope = rememberCoroutineScope()

    val fitnessChips = listOf(
        "Дневник"  to Screen.TrainingLog.route,
        "Упражнения" to Screen.ExerciseCatalog.route,
        "Планы"    to Screen.TrainingPlans.route
    )

    var fabExpanded by remember { mutableStateOf(false) }
    var showPlanPickerDialog by remember { mutableStateOf(false) }
    var isPastingFromPlan by remember { mutableStateOf(false) }
    var pasteResult by remember { mutableStateOf<Boolean?>(null) }

    var dateFrom by remember { mutableStateOf<LocalDate?>(null) }
    var dateTo by remember { mutableStateOf<LocalDate?>(null) }
    var showRangePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadTrainings()
        viewModel.loadTrainingPlans()
    }
    val filtered = remember(trainings, dateFrom, dateTo) {
        trainings.filter { t ->
            val d = t.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@filter true
            (dateFrom == null || !d.isBefore(dateFrom)) && (dateTo == null || !d.isAfter(dateTo))
        }
    }
    val grouped = remember(filtered) {
        filtered.groupBy { it.date ?: "" }.entries
            .sortedByDescending { it.key }
    }

    val hasFilter = dateFrom != null || dateTo != null
    val filterLabel = remember(dateFrom, dateTo) {
        val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        when {
            dateFrom != null && dateTo != null -> "${dateFrom!!.format(fmt)} – ${dateTo!!.format(fmt)}"
            dateFrom != null -> "с ${dateFrom!!.format(fmt)}"
            dateTo != null -> "до ${dateTo!!.format(fmt)}"
            else -> ""
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (fabExpanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                "Начать тренировку",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 14.sp
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = { fabExpanded = false; onStartActiveWorkout?.invoke() },
                            containerColor = Color(0xFF4CAF50)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Начать тренировку", tint = Color.White)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                "Создать запись",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 14.sp
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                fabExpanded = false
                                viewModel.startNewTraining()
                                navController.navigate(Screen.TrainingEditor.route)
                            },
                            containerColor = Color(0xFF4CAF50)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Создать запись", tint = Color.White)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                "Из плана тренировок",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 14.sp
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = { fabExpanded = false; showPlanPickerDialog = true },
                            containerColor = Color(0xFF9C27B0)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Из плана", tint = Color.White)
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Icon(
                        if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (fabExpanded) "Закрыть" else "Добавить",
                        tint = Color.White
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Заголовок + чипсы
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Активность",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp
                        )
                        IconButton(onClick = { showRangePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Фильтр по дате")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fitnessChips.forEach { (label, route) ->
                            val isSelected = route == Screen.TrainingLog.route
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) navController.navigate(route) { launchSingleTop = true }
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF4CAF50),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Resume active workout banner
                if (activeWorkoutName != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResumeWorkout?.invoke() },
                        color = Color(0xFF4CAF50),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Тренировка в процессе", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                                Text(
                                    activeWorkoutName.ifBlank { "Тренировка" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Продолжить", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                // Date range chip
                if (hasFilter) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = { dateFrom = null; dateTo = null },
                            label = { Text(filterLabel, fontSize = 13.sp) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Сбросить", modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        filtered.isEmpty() -> Text(
                            if (hasFilter) "Нет тренировок за выбранный период." else "Нет записей. Нажмите + чтобы добавить тренировку.",
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            color = Color.Gray
                        )
                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                grouped.forEach { (dateStr, dayTrainings) ->
                                    item(key = "header_$dateStr") {
                                        val label = dateStr.let {
                                            runCatching {
                                                LocalDate.parse(it).format(
                                                    DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag("ru"))
                                                )
                                            }.getOrDefault(it)
                                        }
                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                    }
                                    items(dayTrainings, key = { it.id }) { training ->
                                        TrainingLogCard(
                                            training = training,
                                            onEdit = {
                                                viewModel.startEditTraining(training)
                                                navController.navigate(Screen.TrainingEditor.route)
                                            },
                                            onDelete = { viewModel.deleteTraining(training.id) },
                                            onClick = { navController.navigate("trainingDetail/${training.id}") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (isDeletingTraining) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = Color.Red)
                            Text("Удаление...", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // Range picker dialog — custom calendar with visual range selection
    if (showRangePicker) {
        DateRangePickerDialog(
            initialFrom = dateFrom,
            initialTo = dateTo,
            onDismiss = { showRangePicker = false },
            onConfirm = { from, to ->
                dateFrom = from
                dateTo = to
                showRangePicker = false
            }
        )
    }

    // Диалог выбора плана и дня для вставки в дневник
    if (showPlanPickerDialog) {
        PlanDayPickerDialog(
            plans = trainingPlans,
            onDismiss = { showPlanPickerDialog = false },
            onConfirm = { planId, dayNumber ->
                showPlanPickerDialog = false
                isPastingFromPlan = true
                viewModel.createTrainingFromPlanDay(planId, dayNumber) { success ->
                    isPastingFromPlan = false
                    pasteResult = success
                }
            }
        )
    }

    // Индикатор загрузки при вставке из плана
    if (isPastingFromPlan) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 8.dp) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF9C27B0))
                    Text("Добавление тренировки...", fontSize = 14.sp)
                }
            }
        }
    }

    // Снэкбар с результатом вставки
    LaunchedEffect(pasteResult) {
        pasteResult?.let { success ->
            // сбрасываем после показа
            pasteResult = null
        }
    }
}

@Composable
private fun TrainingLogCard(
    training: TrainingData,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить тренировку?") },
            text = { Text("«${training.name}» будет удалена.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Удалить", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable { onClick() },
        shape = shape,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(training.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val subtitle = training.description?.takeIf { it.isNotBlank() }
                    ?: training.exercises.take(3).joinToString(", ") {
                        it.exerciseNameRu ?: it.exerciseName ?: ""
                    }.takeIf { it.isNotBlank() }
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                if (training.exercises.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${training.exercises.size} ${exerciseWord(training.exercises.size)}",
                        fontSize = 13.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать",
                        tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить",
                        tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun exerciseWord(n: Int) = when {
    n % 100 in 11..19 -> "упражнений"
    n % 10 == 1 -> "упражнение"
    n % 10 in 2..4 -> "упражнения"
    else -> "упражнений"
}

internal fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}м ${s}с" else "${s}с"
}

/**
 * Диалог выбора плана тренировок и конкретного дня для вставки в дневник.
 */
@Composable
private fun PlanDayPickerDialog(
    plans: List<TrainingPlanData>,
    onDismiss: () -> Unit,
    onConfirm: (planId: Int, dayNumber: Int) -> Unit
) {
    var selectedPlan by remember { mutableStateOf<TrainingPlanData?>(null) }
    var selectedDayNumber by remember { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Заголовок
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Вставить из плана",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (plans.isEmpty()) {
                        Text(
                            "Нет планов тренировок. Создайте план в разделе «Планы тренировок».",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    } else {
                        // Шаг 1: выбор плана
                        Text("Выберите план:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        plans.forEach { plan ->
                            val isSelected = selectedPlan?.id == plan.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedPlan = plan
                                        selectedDayNumber = null
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF9C27B0).copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedPlan = plan
                                            selectedDayNumber = null
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFF9C27B0)
                                        )
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(plan.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        if (!plan.description.isNullOrBlank()) {
                                            Text(
                                                plan.description,
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Шаг 2: выбор дня (если план выбран и у него есть дни)
                        val currentPlan = selectedPlan
                        if (currentPlan != null && currentPlan.days.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Выберите день:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            currentPlan.days.sortedBy { it.dayNumber }.forEach { day ->
                                val isSelected = selectedDayNumber == day.dayNumber
                                val dayTitle = day.dayName?.takeIf { it.isNotBlank() }
                                    ?: "День ${day.dayNumber}"
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedDayNumber = day.dayNumber },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF9C27B0).copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedDayNumber = day.dayNumber },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFF9C27B0)
                                            )
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(dayTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            if (day.exercises.isNotEmpty()) {
                                                Text(
                                                    day.exercises.take(3).joinToString(", ") {
                                                        it.exerciseNameRu ?: it.exerciseName ?: ""
                                                    }.let { if (day.exercises.size > 3) "$it..." else it },
                                                    fontSize = 12.sp,
                                                    color = Color.Gray,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Кнопки
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            val plan = selectedPlan ?: return@Button
                            // Если у плана нет дней — вставляем весь план как один день (dayNumber = 0)
                            // Если есть дни — требуем выбора конкретного дня
                            val day = if (plan.days.isEmpty()) 0 else (selectedDayNumber ?: return@Button)
                            onConfirm(plan.id, day)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                        enabled = selectedPlan != null && (selectedPlan!!.days.isEmpty() || selectedDayNumber != null)
                    ) {
                        Text("Добавить")
                    }
                }
            }
        }
    }
}
