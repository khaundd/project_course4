package com.example.project_course4.composable_elements.screens.mealplan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.Meal
import com.example.project_course4.MealNutrition
import com.example.project_course4.Product
import com.example.project_course4.SelectedProduct
import com.example.project_course4.composable_elements.MealItem
import com.example.project_course4.composable_elements.dialogs.StandaloneWeightInputDialog
import com.example.project_course4.viewmodel.MealPlanEditorDay
import com.example.project_course4.viewmodel.MealPlanViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanEditorScreen(
    navController: NavController,
    viewModel: MealPlanViewModel,
    allProducts: List<Product>
) {
    val state by viewModel.editor.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val pendingProducts by viewModel.pendingProductsForWeight.collectAsState()

    // Диалог ввода веса для продуктов из SelectProductScreen
    if (pendingProducts.isNotEmpty()) {
        val product = pendingProducts.first()
        StandaloneWeightInputDialog(
            product = product,
            initialWeight = 0,
            showDelete = false,
            onConfirm = { w ->
                if (w > 0) viewModel.confirmProductWeight(product, w)
                else viewModel.skipPendingProduct()
            },
            onDismiss = { viewModel.skipPendingProduct() }
        )
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            viewModel.resetSaveSuccess()
            navController.popBackStack()
        }
    }

    // Скролл вниз при добавлении нового дня
    val prevDayCount = remember { mutableIntStateOf(state.days.size) }
    LaunchedEffect(state.days.size) {
        if (state.days.size > prevDayCount.intValue) {
            coroutineScope.launch {
                // Прокручиваем к последнему элементу (кнопка "Добавить день" — после всех дней)
                listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
            }
        }
        prevDayCount.intValue = state.days.size
    }

    val calories = state.targetCalories.toFloatOrNull() ?: 0f
    val pPct = state.proteinPct.toFloatOrNull() ?: 0f
    val fPct = state.fatsPct.toFloatOrNull() ?: 0f
    val cPct = state.carbsPct.toFloatOrNull() ?: 0f
    val pctSum = pPct + fPct + cPct
    val proteinG = if (calories > 0) (calories * pPct / 100f / 4f).roundToInt() else 0
    val fatsG = if (calories > 0) (calories * fPct / 100f / 9f).roundToInt() else 0
    val carbsG = if (calories > 0) (calories * cPct / 100f / 4f).roundToInt() else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.planId == 0) "Новый план" else "Редактировать план") },
                navigationIcon = {
                    var isNavigatingBack by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            if (!isNavigatingBack) {
                                isNavigatingBack = true
                                navController.popBackStack()
                            }
                        },
                        enabled = !isNavigatingBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.savePlan() },
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Сохранить", color = Color(0xFF4CAF50))
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.updateEditorName(it) },
                    label = { Text("Название плана") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.error != null && state.name.isBlank(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.updateEditorDescription(it) },
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.isPublic, onCheckedChange = { viewModel.updateEditorPublic(it) })
                    Text("Публичный план")
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Целевые показатели за день", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = state.targetCalories,
                            onValueChange = { viewModel.updateEditorTargetCalories(it.filter { c -> c.isDigit() || c == '.' }) },
                            label = { Text("Целевая калорийность (ккал)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Text("Распределение БЖУ (%)", fontSize = 13.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.proteinPct,
                                onValueChange = { viewModel.updateEditorProteinPct(it.filter { c -> c.isDigit() || c == '.' }) },
                                label = { Text("Белки %") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = state.fatsPct,
                                onValueChange = { viewModel.updateEditorFatsPct(it.filter { c -> c.isDigit() || c == '.' }) },
                                label = { Text("Жиры %") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = state.carbsPct,
                                onValueChange = { viewModel.updateEditorCarbsPct(it.filter { c -> c.isDigit() || c == '.' }) },
                                label = { Text("Углев. %") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        if (calories > 0 && pctSum > 0) {
                            val sumColor = if (Math.abs(pctSum - 100f) < 0.5f) Color(0xFF4CAF50) else Color(0xFFE53935)
                            Text(
                                "Сумма: ${pctSum.roundToInt()}%  •  Б: ${proteinG}г  •  Ж: ${fatsG}г  •  У: ${carbsG}г",
                                fontSize = 12.sp,
                                color = sumColor
                            )
                        }
                    }
                }
            }

            itemsIndexed(state.days) { dayIndex, day ->
                DayCard(
                    day = day,
                    dayIndex = dayIndex,
                    allProducts = allProducts,
                    canRemove = state.days.size > 1,
                    onRemoveDay = { viewModel.removeDay(dayIndex) },
                    onAddMeal = { viewModel.addMealToDay(dayIndex) },
                    onRemoveMeal = { mealIndex -> viewModel.removeMealFromDay(dayIndex, mealIndex) },
                    onMealNameChange = { mealIndex, name -> viewModel.updateMealName(dayIndex, mealIndex, name) },
                    onMealTimeChange = { mealIndex, time -> viewModel.updateMealTime(dayIndex, mealIndex, time) },
                    onAddComponent = { mealIndex, productId, weight ->
                        viewModel.addComponentToMeal(dayIndex, mealIndex, productId, weight)
                    },
                    onRemoveComponent = { mealIndex, compIndex ->
                        viewModel.removeComponentFromMeal(dayIndex, mealIndex, compIndex)
                    },
                    onWeightChange = { mealIndex, compIndex, weight ->
                        viewModel.updateComponentWeight(dayIndex, mealIndex, compIndex, weight)
                    },
                    onNotesChange = { notes -> viewModel.updateDayNotes(dayIndex, notes) },
                    onOpenProductPicker = { mealIndex ->
                        viewModel.setPendingMealTarget(dayIndex, mealIndex)
                        navController.navigate("selectProductForMealPlan/$dayIndex/$mealIndex")
                    }
                )
            }

            item {
                OutlinedButton(
                    onClick = { viewModel.addDay() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить день")
                }
            }

            state.error?.let { err ->
                item {
                    Text(err, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun DayCard(
    day: MealPlanEditorDay,
    dayIndex: Int,
    allProducts: List<Product>,
    canRemove: Boolean,
    onRemoveDay: () -> Unit,
    onAddMeal: () -> Unit,
    onRemoveMeal: (Int) -> Unit,
    onMealNameChange: (Int, String) -> Unit,
    onMealTimeChange: (Int, String) -> Unit,
    onAddComponent: (Int, Int, Int) -> Unit,
    onRemoveComponent: (Int, Int) -> Unit,
    onWeightChange: (Int, Int, Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onOpenProductPicker: (mealIndex: Int) -> Unit
) {
    var editorClipboard by remember { mutableStateOf<List<SelectedProduct>>(emptyList()) }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("День ${day.dayNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (canRemove) {
                    IconButton(onClick = onRemoveDay, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить день", tint = Color.Red)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Заметки ко дню
            OutlinedTextField(
                value = day.notes,
                onValueChange = onNotesChange,
                label = { Text("Заметки ко дню (необязательно)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(Modifier.height(8.dp))

            day.meals.forEachIndexed { mealIndex, editorMeal ->
                val timeParts = editorMeal.mealTime.split(":").map { it.toIntOrNull() ?: 0 }
                val mealAsUiMeal = Meal(
                    id = mealIndex,
                    time = LocalTime.of(
                        timeParts.getOrElse(0) { 12 }.coerceIn(0, 23),
                        timeParts.getOrElse(1) { 0 }.coerceIn(0, 59)
                    ),
                    name = editorMeal.name
                )
                val productsForMeal = editorMeal.components.mapNotNull { comp ->
                    val product = allProducts.find { it.productId == comp.productId } ?: return@mapNotNull null
                    SelectedProduct(product = product, weight = comp.weight, mealId = mealIndex)
                }
                val nutrition = MealNutrition(
                    protein = productsForMeal.sumOf { it.product.protein * it.weight / 100.0 }.toFloat(),
                    fats = productsForMeal.sumOf { it.product.fats * it.weight / 100.0 }.toFloat(),
                    carbs = productsForMeal.sumOf { it.product.carbs * it.weight / 100.0 }.toFloat(),
                    calories = productsForMeal.sumOf { it.product.calories * it.weight / 100.0 }.toFloat()
                )

                // Диалог редактирования веса продукта
                var editingCompIndex by remember { mutableIntStateOf(-1) }
                if (editingCompIndex >= 0 && editingCompIndex < editorMeal.components.size) {
                    val comp = editorMeal.components[editingCompIndex]
                    val product = allProducts.find { it.productId == comp.productId }
                    if (product != null) {
                        StandaloneWeightInputDialog(
                            product = product,
                            initialWeight = comp.weight,
                            showDelete = true,
                            onConfirm = { newWeight ->
                                onWeightChange(mealIndex, editingCompIndex, newWeight)
                                editingCompIndex = -1
                            },
                            onDelete = {
                                onRemoveComponent(mealIndex, editingCompIndex)
                                editingCompIndex = -1
                            },
                            onDismiss = { editingCompIndex = -1 }
                        )
                    }
                }

                OutlinedTextField(
                    value = editorMeal.name,
                    onValueChange = { onMealNameChange(mealIndex, it) },
                    label = { Text("Название приёма") },
                    placeholder = { Text("Завтрак", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(4.dp))

                MealItem(
                    meal = mealAsUiMeal,
                    products = productsForMeal,
                    nutrition = nutrition,
                    onTimeClick = { _, newTime ->
                        onMealTimeChange(
                            mealIndex,
                            "${newTime.hour.toString().padStart(2, '0')}:${newTime.minute.toString().padStart(2, '0')}"
                        )
                    },
                    onAddProductClick = { onOpenProductPicker(mealIndex) },
                    onEditProduct = { product, _ ->
                        val compIndex = editorMeal.components.indexOfFirst { it.productId == product.productId }
                        if (compIndex != -1) editingCompIndex = compIndex
                    },
                    onDeleteProduct = { product, _ ->
                        val compIndex = editorMeal.components.indexOfFirst { it.productId == product.productId }
                        if (compIndex != -1) onRemoveComponent(mealIndex, compIndex)
                    },
                    onMealOptionsClick = { onRemoveMeal(mealIndex) },
                    onCopyMeal = { editorClipboard = productsForMeal },
                    onPasteMeal = { _ ->
                        editorClipboard.forEach { item ->
                            val existing = editorMeal.components.indexOfFirst { it.productId == item.product.productId }
                            if (existing != -1) {
                                onWeightChange(mealIndex, existing, editorMeal.components[existing].weight + item.weight)
                            } else {
                                onAddComponent(mealIndex, item.product.productId, item.weight)
                            }
                        }
                    },
                    canPaste = editorClipboard.isNotEmpty()
                )

                if (mealIndex < day.meals.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Итого КБЖУ за день
            val dayProtein = day.meals.sumOf { meal ->
                meal.components.sumOf { comp ->
                    val product = allProducts.find { it.productId == comp.productId }
                    (product?.protein?.toDouble() ?: 0.0) * comp.weight / 100.0
                }
            }.toFloat()
            val dayFats = day.meals.sumOf { meal ->
                meal.components.sumOf { comp ->
                    val product = allProducts.find { it.productId == comp.productId }
                    (product?.fats?.toDouble() ?: 0.0) * comp.weight / 100.0
                }
            }.toFloat()
            val dayCarbs = day.meals.sumOf { meal ->
                meal.components.sumOf { comp ->
                    val product = allProducts.find { it.productId == comp.productId }
                    (product?.carbs?.toDouble() ?: 0.0) * comp.weight / 100.0
                }
            }.toFloat()
            val dayCalories = day.meals.sumOf { meal ->
                meal.components.sumOf { comp ->
                    val product = allProducts.find { it.productId == comp.productId }
                    (product?.calories?.toDouble() ?: 0.0) * comp.weight / 100.0
                }
            }.toFloat()

            if (day.meals.any { it.components.isNotEmpty() }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Итого за день:", fontSize = 12.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Б %.0fг".format(dayProtein),
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Ж %.0fг".format(dayFats),
                                fontSize = 12.sp,
                                color = Color(0xFFFFC107),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "У %.0fг".format(dayCarbs),
                                fontSize = 12.sp,
                                color = Color(0xFFFF5722),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "%.0f ккал".format(dayCalories),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            TextButton(onClick = onAddMeal) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Добавить приём пищи")
            }
        }
    }
}


