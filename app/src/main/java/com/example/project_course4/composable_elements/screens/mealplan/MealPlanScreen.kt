package com.example.project_course4.composable_elements.screens.mealplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
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
import com.example.project_course4.api.MealPlanData
import com.example.project_course4.viewmodel.MealPlanViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    navController: NavController,
    viewModel: MealPlanViewModel
) {
    val plans by viewModel.plans.collectAsState()
    val publicPlans by viewModel.publicPlans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { viewModel.loadPlans() }
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) viewModel.loadPublicPlans()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Планы питания") },
                navigationIcon = {
                    var isNavigatingBack by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { if (!isNavigatingBack) { isNavigatingBack = true; navController.popBackStack() } },
                        enabled = !isNavigatingBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { viewModel.startNewPlan(); navController.navigate("mealPlanEditor") },
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать план", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Мои планы") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Публичные") })
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    selectedTab == 0 -> {
                        if (plans.isEmpty()) {
                            Text(
                                "Нет планов питания. Нажмите + чтобы создать.",
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(plans) { _, plan ->
                                    var showDeleteDialog by remember { mutableStateOf(false) }

                                    if (showDeleteDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showDeleteDialog = false },
                                            title = { Text("Удалить план?") },
                                            text = { Text("«${plan.name}» будет удалён безвозвратно.") },
                                            confirmButton = {
                                                TextButton(onClick = { showDeleteDialog = false; viewModel.deletePlan(plan.planId) }) {
                                                    Text("Удалить", color = Color.Red)
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
                                            }
                                        )
                                    }

                                    MyMealPlanCard(
                                        plan = plan,
                                        onClick = { navController.navigate("mealPlanDetail/${plan.planId}") },
                                        onEdit = { viewModel.startEditPlan(plan); navController.navigate("mealPlanEditor") },
                                        onDelete = { showDeleteDialog = true },
                                        onTogglePublic = { viewModel.togglePlanPublic(plan.planId) }
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        if (publicPlans.isEmpty()) {
                            Text(
                                "Нет доступных публичных планов.",
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(publicPlans) { _, plan ->
                                    PublicMealPlanCard(
                                        plan = plan,
                                        onView = { navController.navigate("mealPlanDetail/${plan.planId}") }
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
private fun MyMealPlanCard(
    plan: MealPlanData,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePublic: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable { onClick() },
        shape = shape,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                if (plan.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(plan.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${plan.targetCalories.roundToInt()} ккал  ·  " +
                    "Б ${plan.targetProteinG.roundToInt()}г  ·  " +
                    "Ж ${plan.targetFatsG.roundToInt()}г  ·  " +
                    "У ${plan.targetCarbsG.roundToInt()}г",
                    fontSize = 13.sp,
                    color = Color(0xFF4CAF50)
                )
            }
            // Троеточие — меню
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
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
            // Замок — переключение публичности
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

@Composable
private fun PublicMealPlanCard(
    plan: MealPlanData,
    onView: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable { onView() },
        shape = shape,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                val days = plan.dayCount ?: plan.days.size
                Text(
                    text = "$days ${dayWord(days)}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            if (plan.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(plan.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${plan.targetCalories.roundToInt()} ккал  ·  " +
                "Б ${plan.targetProteinG.roundToInt()}г  ·  " +
                "Ж ${plan.targetFatsG.roundToInt()}г  ·  " +
                "У ${plan.targetCarbsG.roundToInt()}г",
                fontSize = 13.sp,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

private fun dayWord(n: Int): String = when {
    n % 100 in 11..19 -> "дней"
    n % 10 == 1 -> "день"
    n % 10 in 2..4 -> "дня"
    else -> "дней"
}
