package com.example.project_course4.composable_elements.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.project_course4.Screen
import com.example.project_course4.MealNutrition
import com.example.project_course4.SelectedProduct
import com.example.project_course4.UserRole
import com.example.project_course4.composable_elements.charts.NutritionChart
import com.example.project_course4.composable_elements.dialogs.CustomCalendarDialog
import com.example.project_course4.viewmodel.ProductViewModel
import com.example.project_course4.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.project_course4.R
import com.example.project_course4.composable_elements.CustomButton
import com.example.project_course4.composable_elements.MealItem
import com.example.project_course4.composable_elements.dialogs.WeightInputDialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    profileViewModel: ProfileViewModel,
    userRole: UserRole,
) {
    val scope = rememberCoroutineScope()

    val macroNutrients by profileViewModel.macroNutrients.collectAsState()
    val dailyCalories by profileViewModel.dailyCalories.collectAsState()

    val meals by viewModel.meals.collectAsState()
    val dayClipboard by viewModel.dayClipboard.collectAsState()

    var showCustomCalendar by remember { mutableStateOf(false) }
    var selectedLocalDate by remember { mutableStateOf(LocalDate.now()) }
    var isNavigatingToAddProduct by remember { mutableStateOf(false) }

    // Nutrition chips
    val nutritionChips = listOf(
        "Дневник"       to Screen.Main.route,
        "Рецепты"       to Screen.Recipes.route,
        "Планы питания" to Screen.MealPlans.route
    )

    // Следим за изменением даты в ViewModel
    LaunchedEffect(Unit) {
        viewModel.selectedDate.collect { date ->
            selectedLocalDate = date
        }
    }
    // Загружаем приёмы пищи при изменении выбранной даты
    LaunchedEffect(selectedLocalDate) {
        viewModel.loadMealsForDate(selectedLocalDate, createDefaultsIfEmpty = true)
    }

    // Проверяем и загружаем продукты при старте, если они еще не загружены
    LaunchedEffect(Unit) {
        val products = viewModel.products.value
        if (products.isEmpty()) {
            Log.d("MainScreen", "Продукты не загружены, вызываем loadProductsAfterAuth")
            viewModel.loadProductsAfterAuth()
        } else {
            Log.d("MainScreen", "Продукты уже загружены: ${products.size} шт.")
        }
    }

    val dateButtonText = remember(selectedLocalDate) {
        val today = LocalDate.now()
        val label = when (selectedLocalDate) {
            today -> "Сегодня"
            today.minusDays(1) -> "Вчера"
            today.plusDays(1) -> "Завтра"
            else -> selectedLocalDate.format(DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("ru")))
                .replaceFirstChar { it.uppercase() }
        }
        "$label, ${selectedLocalDate.format(DateTimeFormatter.ofPattern("MMM dd", Locale.forLanguageTag("ru")))}"
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val finalSelection by viewModel.finalSelection.collectAsState()
                val currentProductForWeight by viewModel.currentProductForWeight.collectAsState()
                val shouldShowWeightInput by viewModel.shouldShowWeightInput.collectAsState()
                val mealClipboard by viewModel.mealClipboard.collectAsState()

                LaunchedEffect(shouldShowWeightInput) {
                    if (shouldShowWeightInput) viewModel.checkAndStartWeightInput()
                }

                if (currentProductForWeight != null && shouldShowWeightInput) {
                    WeightInputDialog(
                        product = currentProductForWeight!!,
                        viewModel = viewModel,
                        onDismiss = { viewModel.clearWeightInput() }
                    )
                }

                fun getMealNutrition(products: List<SelectedProduct>): MealNutrition {
                    val totalCalories = products.sumOf { (it.product.calories.toDouble() * it.weight / 100) }.toFloat()
                    val totalProtein = products.sumOf { (it.product.protein.toDouble() * it.weight / 100) }.toFloat()
                    val totalFats = products.sumOf { (it.product.fats.toDouble() * it.weight / 100) }.toFloat()
                    val totalCarbs = products.sumOf { (it.product.carbs.toDouble() * it.weight / 100) }.toFloat()
                    return MealNutrition(totalProtein, totalFats, totalCarbs, totalCalories)
                }

                val currentMealIds = meals.map { it.id }
                val currentDayProducts = finalSelection.filter { it.mealId in currentMealIds }

                val totalCalories = currentDayProducts.sumOf { (it.product.calories.toDouble() * it.weight / 100) }.toFloat()
                val totalProtein = currentDayProducts.sumOf { (it.product.protein.toDouble() * it.weight / 100) }.toFloat()
                val totalFats = currentDayProducts.sumOf { (it.product.fats.toDouble() * it.weight / 100) }.toFloat()
                val totalCarbs = currentDayProducts.sumOf { (it.product.carbs.toDouble() * it.weight / 100) }.toFloat()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    // Заголовок с датой + чипсы
                    item {
                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showCustomCalendar = true }
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        dateButtonText,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                }
                                // MoreVert меню
                                var menuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Копировать день") },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                            enabled = meals.isNotEmpty(),
                                            onClick = { viewModel.copyDayToClipboard(); menuExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (dayClipboard.isNotEmpty()) "Вставить день (${dayClipboard.size})"
                                                    else "Вставить день"
                                                )
                                            },
                                            leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                                            enabled = dayClipboard.isNotEmpty(),
                                            onClick = { viewModel.pasteDayFromClipboard(selectedLocalDate); menuExpanded = false }
                                        )
                                    }
                                }
                            }

                            // Чипсы
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(nutritionChips) { (label, route) ->
                                    val isSelected = route == Screen.Main.route
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (!isSelected) navController.navigate(route) {
                                                launchSingleTop = true; restoreState = true
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

                            NutritionChart(
                                protein = totalProtein,
                                fats = totalFats,
                                carbs = totalCarbs,
                                totalCalories = totalCalories,
                                targetProtein = macroNutrients.protein,
                                targetFats = macroNutrients.fats,
                                targetCarbs = macroNutrients.carbs,
                                targetCalories = dailyCalories,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }

                    items(meals) { meal ->
                        val products = finalSelection.filter { it.mealId == meal.id }
                        val nutrition = getMealNutrition(products)

                        MealItem(
                            meal = meal,
                            products = products,
                            nutrition = nutrition,
                            onTimeClick = { _, newTime -> viewModel.updateMealTime(meal.id, newTime) },
                            onCopyMeal = { m -> viewModel.copyMealToClipboard(m.id) },
                            onPasteMeal = { m -> viewModel.pasteMealFromClipboard(m.id) },
                            canPaste = mealClipboard.isNotEmpty(),
                            onAddProductClick = { m ->
                                if (!isNavigatingToAddProduct) {
                                    isNavigatingToAddProduct = true
                                    if (shouldShowWeightInput) {
                                        scope.launch { viewModel.saveCurrentMeal(meal.id) }
                                    }
                                    viewModel.setEditingMealId(m.id)
                                    navController.navigate("selectProductWithMeal/${m.id}")
                                    scope.launch {
                                        delay(500)
                                        isNavigatingToAddProduct = false
                                    }
                                }
                            },
                            onEditProduct = { p, m -> viewModel.editProductWeightInMeal(p, m.id) },
                            onDeleteProduct = { p, m ->
                                Log.d("DeleteProduct", "Удаляемый продукт: $p, id приема пищи: ${m.id}")
                                viewModel.removeProductFromMeal(p, m.id)
                            },
                            onMealOptionsClick = { m -> viewModel.removeMeal(m.id) }
                        )

                        if (meal == meals.last()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CustomButton(
                                onClick = { viewModel.addMeal("...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                backgroundColor = colorResource(id = R.color.buttonColor),
                                textColor = Color.White,
                                text = "Добавить приём пищи",
                                cornerRadius = 32.dp
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (meals.isEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            CustomButton(
                                onClick = { viewModel.addMeal("...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                backgroundColor = colorResource(id = R.color.buttonColor),
                                textColor = Color.White,
                                text = "Добавить приём пищи",
                                cornerRadius = 32.dp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCustomCalendar) {
        CustomCalendarDialog(
            initialDate = selectedLocalDate,
            viewModel = viewModel,
            onDateSelected = { newDate ->
                viewModel.setSelectedDate(newDate)
            },
            onDismiss = { showCustomCalendar = false }
        )
    }
}
