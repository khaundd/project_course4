package com.example.project_course4.composable_elements.screens.fitness

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
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
import com.example.project_course4.api.TrainingPlanDayData
import com.example.project_course4.api.TrainingPlanExerciseData
import com.example.project_course4.viewmodel.FitnessViewModel
import com.example.project_course4.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlanDetailScreen(
    navController: NavController,
    viewModel: FitnessViewModel,
    planId: Int
) {
    val plan by viewModel.selectedPlan.collectAsState()
    val isLoading by viewModel.isLoadingPlanDetail.collectAsState()
    val myPlans by viewModel.trainingPlans.collectAsState()
    val isOwner = plan?.let { p -> myPlans.any { it.id == p.id } } ?: false

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isNavigatingBack by remember { mutableStateOf(false) }

    LaunchedEffect(planId) { viewModel.loadTrainingPlanById(planId) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить план?") },
            text = { Text("«${plan?.name}» будет удалён.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    plan?.let { viewModel.deleteTrainingPlan(it.id) }
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
                title = { Text("Тренировочный план") },
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
                    if (isOwner) {
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
                                        plan?.let {
                                            viewModel.loadTrainingPlanForEdit(it.id) {
                                                navController.navigate(Screen.TrainingPlanEditor.route)
                                            }
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
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            plan == null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("План не найден", color = Color.Gray)
            }
            else -> {
                val p = plan!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title
                    item {
                        Text(
                            p.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }

                    // Description
                    p.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 1.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Описание", fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text(desc, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Training days header
                    item {
                        Text(
                            "Тренировочные дни",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Day cards
                    items(p.days.sortedBy { it.dayNumber }) { day ->
                        TrainingPlanDayCard(day = day)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingPlanDayCard(day: TrainingPlanDayData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val title = day.dayName?.takeIf { it.isNotBlank() } ?: "День ${day.dayNumber}"
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { /* copy day */ }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Копировать",
                        modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (day.exercises.isEmpty()) {
                Text("Нет упражнений", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            } else {
                Spacer(Modifier.height(8.dp))
                day.exercises.forEachIndexed { i, ex ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${i + 1}. ${ex.exerciseNameRu ?: ex.exerciseName ?: "Упражнение"}",
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        val detail = buildPlanExerciseDetail(ex)
                        if (detail.isNotBlank()) {
                            Text(detail, fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Notes
            day.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Заметки", fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(notes, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun buildPlanExerciseDetail(ex: TrainingPlanExerciseData): String = buildString {
    when {
        ex.sets != null && ex.reps != null -> append("${ex.sets}×${ex.reps}")
        ex.sets != null -> append("${ex.sets} подх.")
        ex.reps != null -> append("${ex.reps} повт.")
    }
    ex.weight?.let { if (it > 0) append(" ${it} кг.") }
    ex.exerciseTime?.let { append(" ${formatSeconds(it)}") }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}м ${s}с" else "${s}с"
}
