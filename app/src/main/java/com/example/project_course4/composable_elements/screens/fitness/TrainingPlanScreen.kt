package com.example.project_course4.composable_elements.screens.fitness

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.api.TrainingPlanData
import com.example.project_course4.viewmodel.FitnessViewModel
import com.example.project_course4.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlanScreen(
    navController: NavController,
    viewModel: FitnessViewModel,
    drawerState: DrawerState? = null
) {
    val plans by viewModel.trainingPlans.collectAsState()
    val publicPlans by viewModel.publicTrainingPlans.collectAsState()
    val isLoading by viewModel.isLoadingPlans.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val fitnessChips = listOf(
        "Дневник"  to Screen.TrainingLog.route,
        "Упражнения" to Screen.ExerciseCatalog.route,
        "Планы"    to Screen.TrainingPlans.route
    )

    LaunchedEffect(Unit) { viewModel.loadTrainingPlans() }
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) viewModel.loadPublicTrainingPlans()
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        viewModel.startNewTrainingPlan()
                        navController.navigate(Screen.TrainingPlanEditor.route)
                    },
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать план", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Заголовок + чипсы
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Активность",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fitnessChips.forEach { (label, route) ->
                        val isSelected = route == Screen.TrainingPlans.route
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
                Spacer(Modifier.height(4.dp))
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Мои планы", color = if (selectedTab == 0) Color(0xFF7C4DFF) else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Публичные", color = if (selectedTab == 1) Color(0xFF7C4DFF) else Color.Gray) }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    selectedTab == 0 -> {
                        if (plans.isEmpty()) {
                            Text(
                                "Нет планов тренировок. Нажмите + чтобы создать.",
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(plans) { _, plan ->
                                    MyTrainingPlanCard(
                                        plan = plan,
                                        onEdit = {
                                            viewModel.loadTrainingPlanForEdit(plan.id) {
                                                navController.navigate(Screen.TrainingPlanEditor.route)
                                            }
                                        },
                                        onDelete = { viewModel.deleteTrainingPlan(plan.id) },
                                        onTogglePublic = { viewModel.toggleTrainingPlanPublic(plan.id) },
                                        onClick = { navController.navigate("trainingPlanDetail/${plan.id}") }
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        if (publicPlans.isEmpty()) {
                            Text(
                                "Нет публичных планов.",
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(publicPlans) { _, plan ->
                                    PublicTrainingPlanCard(
                                        plan = plan,
                                        onClick = { navController.navigate("trainingPlanDetail/${plan.id}") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyTrainingPlanCard(
    plan: TrainingPlanData,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePublic: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить план?") },
            text = { Text("«${plan.name}» будет удалён.") },
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Лейбл «от тренера» — только текст с иконкой, без фона
                    if (plan.isAssignedByTrainer) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF7C4DFF),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Назначен тренером",
                                fontSize = 11.sp,
                                color = Color(0xFF7C4DFF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    plan.description?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // Кнопки управления только для своих планов
                if (!plan.isAssignedByTrainer) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Опции", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Редактировать") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { showMenu = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text("Удалить", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                onClick = { showMenu = false; showDeleteDialog = true }
                            )
                        }
                    }
                    IconButton(onClick = onTogglePublic, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (plan.isPublic) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = if (plan.isPublic) "Сделать приватным" else "Сделать публичным",
                            tint = if (plan.isPublic) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicTrainingPlanCard(plan: TrainingPlanData, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable { onClick() },
        shape = shape,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            plan.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun dayWord(n: Int) = when {
    n % 100 in 11..19 -> "дней"
    n % 10 == 1 -> "день"
    n % 10 in 2..4 -> "дня"
    else -> "дней"
}
