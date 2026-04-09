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
                            // Храним id раскрытой карточки
                            var expandedPlanId by remember { mutableStateOf<Int?>(null) }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                itemsIndexed(plans) { _, plan ->
                                    var showDeleteDialog by remember { mutableStateOf(false) }
                                    val isExpanded = expandedPlanId == plan.planId

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
                                        isExpanded = isExpanded,
                                        onCardClick = {
                                            expandedPlanId = if (isExpanded) null else plan.planId
                                        },
                                        onView = { navController.navigate("mealPlanDetail/${plan.planId}") },
                                        onEdit = { viewModel.startEditPlan(plan); navController.navigate("mealPlanEditor") },
                                        onDelete = { showDeleteDialog = true }
                                    )
                                    // Отступ между карточками — фиксированный, панель кнопок уже внутри карточки
                                    Spacer(Modifier.height(12.dp))
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
    isExpanded: Boolean,
    onCardClick: () -> Unit,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardShape = RoundedCornerShape(12.dp)
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(1.dp, borderColor, cardShape),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
            // Основная часть карточки
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCardClick() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = plan.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                    val days = plan.days.size.takeIf { it > 0 } ?: (plan.dayCount ?: 0)
                    Text(
                        text = "$days ${dayWord(days)}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Icon(
                        imageVector = if (plan.isPublic) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = if (plan.isPublic) "Публичный" else "Непубличный",
                        tint = Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (plan.description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(plan.description, fontSize = 14.sp, color = Color.Gray)
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "${plan.targetCalories.roundToInt()} ккал  ·  " +
                    "Б ${plan.targetProteinG.roundToInt()}г  ·  " +
                    "Ж ${plan.targetFatsG.roundToInt()}г  ·  " +
                    "У ${plan.targetCarbsG.roundToInt()}г",
                    fontSize = 13.sp,
                    color = Color(0xFF4CAF50)
                )
            }

            // Выезжающая панель кнопок внутри той же карточки
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column {
                    HorizontalDivider(color = borderColor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red, modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = onView) {
                            Icon(Icons.Default.Visibility, contentDescription = "Просмотр", tint = Color(0xFF2196F3), modifier = Modifier.size(28.dp))
                        }
                    }
                }
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Левая часть — информация о плане
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = plan.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                    val days = plan.dayCount ?: plan.days.size
                    Text(
                        text = "$days ${dayWord(days)}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                if (plan.description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(plan.description, fontSize = 14.sp, color = Color.Gray)
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "${plan.targetCalories.roundToInt()} ккал  ·  " +
                    "Б ${plan.targetProteinG.roundToInt()}г  ·  " +
                    "Ж ${plan.targetFatsG.roundToInt()}г  ·  " +
                    "У ${plan.targetCarbsG.roundToInt()}г",
                    fontSize = 13.sp,
                    color = Color(0xFF4CAF50)
                )
            }

            // Вертикальный разделитель
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Правая часть — кликабельная область с иконкой глаза
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .clickable { onView() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Просмотреть",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private fun dayWord(n: Int): String = when {
    n % 100 in 11..19 -> "дней"
    n % 10 == 1 -> "день"
    n % 10 in 2..4 -> "дня"
    else -> "дней"
}
