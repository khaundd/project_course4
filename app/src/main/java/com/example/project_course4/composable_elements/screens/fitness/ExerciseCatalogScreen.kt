package com.example.project_course4.composable_elements.screens.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
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
import com.example.project_course4.api.ExerciseResponse
import com.example.project_course4.api.MuscleResponse
import com.example.project_course4.viewmodel.FitnessViewModel
import com.example.project_course4.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCatalogScreen(
    navController: NavController,
    viewModel: FitnessViewModel,
    selectionMode: Boolean = false,
    onExercisesSelected: ((List<ExerciseResponse>) -> Unit)? = null
) {
    val exercises by viewModel.exercises.collectAsState()
    val muscles by viewModel.muscles.collectAsState()
    val isLoading by viewModel.isLoadingExercises.collectAsState()
    val hasMore by viewModel.hasMoreExercises.collectAsState()
    val exercisesTotal by viewModel.exercisesTotal.collectAsState()
    val filterMuscleIds by viewModel.filterMuscleIds.collectAsState()
    val filterLevels by viewModel.filterLevels.collectAsState()
    val filterEquipments by viewModel.filterEquipments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val equipmentList by viewModel.equipmentList.collectAsState()

    var localSearch by remember { mutableStateOf(searchQuery) }
    var showFilterDialog by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<ExerciseResponse>() }

    val listState = rememberLazyListState()

    val fitnessChips = listOf(
        "Дневник"    to Screen.TrainingLog.route,
        "Упражнения" to Screen.ExerciseCatalog.route,
        "Планы"      to Screen.TrainingPlans.route
    )

    LaunchedEffect(Unit) {
        if (muscles.isEmpty()) viewModel.loadMuscles()
        if (exercises.isEmpty()) viewModel.resetAndLoadExercises()
        viewModel.loadEquipmentList()
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastVisible >= exercises.size - 5 && hasMore && !isLoading) {
            viewModel.loadExercises()
        }
    }

    val hasActiveFilters = filterMuscleIds.isNotEmpty() || filterLevels.isNotEmpty() || filterEquipments.isNotEmpty()

    Scaffold(
        floatingActionButton = {
            if (selectionMode && selected.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        onExercisesSelected?.invoke(selected.toList())
                        navController.popBackStack()
                    },
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Добавить", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // ── Заголовок + чипсы + поиск ───────────────────────────────
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp, bottom = 4.dp)
                    ) {
                        // Заголовок
                        Text(
                            text = if (selectionMode) "Выбор упражнений" else "Активность",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        // Section chips (только не в режиме выбора)
                        if (!selectionMode) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 4.dp)
                            ) {
                                items(fitnessChips) { (label, route) ->
                                    val isSelected = route == Screen.ExerciseCatalog.route
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (!isSelected) {
                                                navController.navigate(route) { launchSingleTop = true }
                                            }
                                        },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF4CAF50),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        // Строка поиска
                        OutlinedTextField(
                            value = localSearch,
                            onValueChange = { localSearch = it; viewModel.setSearchQuery(it) },
                            placeholder = { Text("Поиск упражнений...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (localSearch.isNotEmpty()) {
                                    IconButton(onClick = { localSearch = ""; viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Очистить", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        // Кнопка фильтра
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val filterLabel = if (hasActiveFilters) "Отобрано ($exercisesTotal)" else "Все ($exercisesTotal)"
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showFilterDialog = true },
                                shape = RoundedCornerShape(20.dp),
                                color = if (hasActiveFilters) Color(0xFF3D5AFE) else Color(0xFF5C6BC0)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Text(filterLabel, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            }
                            if (hasActiveFilters) {
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = {
                                    viewModel.applyFilters(emptyList(), emptyList(), emptyList())
                                }) {
                                    Text("Очистить", color = Color(0xFF3D5AFE), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                // ── Активные чипсы фильтра ───────────────────────────────────
                if (hasActiveFilters) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            items(filterMuscleIds) { id ->
                                val muscleName = muscles.find { it.id == id }?.name ?: id.toString()
                                ActiveFilterChip(label = muscleName, onRemove = {
                                    viewModel.applyFilters(filterMuscleIds - id, filterLevels, filterEquipments)
                                })
                            }
                            items(filterEquipments) { eq ->
                                ActiveFilterChip(label = eq, onRemove = {
                                    viewModel.applyFilters(filterMuscleIds, filterLevels, filterEquipments - eq)
                                })
                            }
                            items(filterLevels) { level ->
                                ActiveFilterChip(label = levelLabel(level), onRemove = {
                                    viewModel.applyFilters(filterMuscleIds, filterLevels - level, filterEquipments)
                                })
                            }
                        }
                    }
                }

                // ── Список упражнений ────────────────────────────────────────
                items(exercises) { exercise ->
                    val isSelected = selected.any { it.id == exercise.id }
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        ExerciseCard(
                            exercise = exercise,
                            selectionMode = selectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (selectionMode) {
                                    if (isSelected) selected.removeIf { it.id == exercise.id }
                                    else selected.add(exercise)
                                } else {
                                    viewModel.selectExercise(exercise)
                                    navController.navigate("exerciseDetail/${exercise.id}")
                                }
                            }
                        )
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            if (!isLoading && exercises.isEmpty()) {
                Text(
                    "Упражнения не найдены",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            muscles = muscles,
            equipmentList = equipmentList,
            selectedMuscleIds = filterMuscleIds,
            selectedLevels = filterLevels,
            selectedEquipments = filterEquipments,
            onDismiss = { showFilterDialog = false },
            onApply = { muscleIds, levels, equipments ->
                viewModel.applyFilters(muscleIds, levels, equipments)
                showFilterDialog = false
            }
        )
    }
}

@Composable
private fun ActiveFilterChip(label: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Удалить", modifier = Modifier.size(14.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterDialog(
    muscles: List<MuscleResponse>,
    equipmentList: List<String>,
    selectedMuscleIds: List<Int>,
    selectedLevels: List<String>,
    selectedEquipments: List<String>,
    onDismiss: () -> Unit,
    onApply: (List<Int>, List<String>, List<String>) -> Unit
) {
    val tempMuscleIds = remember { mutableStateListOf<Int>().also { it.addAll(selectedMuscleIds) } }
    val tempLevels = remember { mutableStateListOf<String>().also { it.addAll(selectedLevels) } }
    val tempEquipments = remember { mutableStateListOf<String>().also { it.addAll(selectedEquipments) } }

    val muscleGroups = muscles.groupBy { it.groupName ?: "Другое" }
    val allLevels = listOf("beginner", "intermediate", "expert")

    val hasTemp = tempMuscleIds.isNotEmpty() || tempLevels.isNotEmpty() || tempEquipments.isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Все упражнения", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (hasTemp) {
                        TextButton(onClick = { tempMuscleIds.clear(); tempLevels.clear(); tempEquipments.clear() }) {
                            Text("Сбросить", color = Color(0xFF4CAF50))
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    // Область внимания
                    Text("Область внимания", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        muscles.forEach { muscle ->
                            val isSelected = tempMuscleIds.contains(muscle.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) tempMuscleIds.remove(muscle.id)
                                    else tempMuscleIds.add(muscle.id)
                                },
                                label = { Text(muscle.name, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // Уровень
                    Text("Уровень", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allLevels.forEach { level ->
                            val isSelected = tempLevels.contains(level)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) tempLevels.remove(level)
                                    else tempLevels.add(level)
                                },
                                label = { Text(levelLabel(level), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = levelColor(level).copy(alpha = 0.2f),
                                    selectedLabelColor = levelColor(level)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    // Оборудование
                    Text("Оборудование", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))

                    if (equipmentList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else {
                        equipmentList.forEach { eq ->
                            val isSelected = tempEquipments.contains(eq)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isSelected) tempEquipments.remove(eq)
                                        else tempEquipments.add(eq)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF4CAF50).copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                tonalElevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        eq,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 15.sp,
                                        color = if (isSelected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // Buttons
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Button(
                        onClick = { onApply(tempMuscleIds.toList(), tempLevels.toList(), tempEquipments.toList()) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Сохранить", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Отмена", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseResponse,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable { onClick() },
        shape = shape,
        color = if (isSelected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50))
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                // Название
                val name = exercise.nameRu ?: exercise.name
                Text(name, fontWeight = FontWeight.Medium, fontSize = 17.sp)
                Spacer(Modifier.height(6.dp))
                // Целевая мышца
                exercise.targetMuscleName?.let { muscle ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF7C4DFF).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("Целевая мышца: $muscle", fontSize = 14.sp, color = Color(0xFF7C4DFF), fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                // Оборудование — цветной блок
                exercise.equipment?.let { eq ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2196F3).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "Необходимое оборудование: $eq",
                            fontSize = 14.sp,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                // Уровень — цветной блок
                exercise.level?.let { lvl ->
                    val color = levelColor(lvl)
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "Уровень: ${levelLabel(lvl)}",
                            fontSize = 14.sp,
                            color = color,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun levelLabel(level: String) = when (level) {
    "beginner" -> "Начинающий"
    "intermediate" -> "Средний"
    "expert" -> "Продвинутый"
    else -> level
}

private fun levelColor(level: String) = when (level) {
    "beginner" -> Color(0xFF4CAF50)
    "intermediate" -> Color(0xFFFF9800)
    "expert" -> Color(0xFFF44336)
    else -> Color.Gray
}
