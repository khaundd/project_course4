package com.example.project_course4.composable_elements

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.project_course4.Screen
import com.example.project_course4.MealNutrition
import com.example.project_course4.SelectedProduct
import com.example.project_course4.composable_elements.charts.NutritionChart
import com.example.project_course4.dialogs.CustomCalendarDialog
import com.example.project_course4.viewmodel.ProductViewModel
import com.example.project_course4.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.project_course4.R
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    profileViewModel: ProfileViewModel,
    onLogout: () -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Гарантируем, что drawer закрыт при возвращении на MainScreen
    LaunchedEffect(Unit) {
        Log.d("MainScreen", "MainScreen создан, текущее состояние drawer: ${drawerState.currentValue}")
        if (drawerState.currentValue != DrawerValue.Closed) {
            scope.launch {
                drawerState.close()
                Log.d("MainScreen", "Drawer принудительно закрыт")
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Отслеживаем получение фокуса экраном
    LaunchedEffect(currentRoute) {
        Log.d("MainScreen", "MainScreen получил фокус, currentRoute: $currentRoute")
        if (currentRoute == Screen.Main.route) {
            scope.launch {
                if (drawerState.currentValue != DrawerValue.Closed) {
                    drawerState.close()
                    Log.d("MainScreen", "Drawer закрыт при получении фокуса")
                }
            }
        }
    }

    val macroNutrients by profileViewModel.macroNutrients.collectAsState()
    val dailyCalories by profileViewModel.dailyCalories.collectAsState()

    var showCustomCalendar by remember { mutableStateOf(false) }
    var selectedLocalDate by remember { mutableStateOf(LocalDate.now()) }
    var isNavigatingToAddProduct by remember { mutableStateOf(false) }
            
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
            else -> selectedLocalDate.format(DateTimeFormatter.ofPattern("EEE", Locale("ru")))
                .replaceFirstChar { it.uppercase() }
        }
        "$label, ${selectedLocalDate.format(DateTimeFormatter.ofPattern("MMM dd", Locale("ru")))}"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Меню", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    label = { Text("Дневник питания") },
                    selected = currentRoute == Screen.Main.route,
                    onClick = { 
                        Log.d("MainScreen", "Нажат пункт 'Дневник питания'")
                        scope.launch { drawerState.close() } 
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                    label = { Text("Создать продукт") },
                    selected = currentRoute?.startsWith(Screen.ProductCreation.route) == true,
                    onClick = {
                        Log.d("MainScreen", "Нажат пункт 'Создать продукт'")
                        scope.launch { drawerState.close() }
                        navController.navigate("productCreation?barcode=")
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text("Профиль") },
                    selected = currentRoute == Screen.Profile.route,
                    onClick = {
                        Log.d("MainScreen", "Нажат пункт 'Профиль'")
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Profile.route)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showCustomCalendar = true }.padding(8.dp)
                        ) {
                            Text(dateButtonText, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            Log.d("MainScreen", "Нажата кнопка меню, текущее состояние drawer: ${drawerState.currentValue}")
                            scope.launch { 
                                try {
                                    drawerState.open()
                                    Log.d("MainScreen", "Drawer успешно открыт")
                                } catch (e: Exception) {
                                    Log.e("MainScreen", "Ошибка при открытии drawer: ${e.message}", e)
                                }
                            } 
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White)
            ) {
                // Приёмы пищи уже инициализируются в ProductViewModel.init()
                var isLoading by remember { mutableStateOf(false) }
                val meals by viewModel.meals.collectAsState()
                val finalSelection by viewModel.finalSelection.collectAsState()
                val currentProductForWeight by viewModel.currentProductForWeight.collectAsState()
                val shouldShowWeightInput by viewModel.shouldShowWeightInput.collectAsState()

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

                // Вспомогательная функция для расчета нутриентов приёма пищи
                fun getMealNutrition(mealId: Int, products: List<SelectedProduct>): MealNutrition {
                    val totalCalories = products.sumOf {
                        (it.product.calories.toDouble() * it.weight / 100)
                    }.toFloat()

                    val totalProtein = products.sumOf {
                        (it.product.protein.toDouble() * it.weight / 100)
                    }.toFloat()

                    val totalFats = products.sumOf {
                        (it.product.fats.toDouble() * it.weight / 100)
                    }.toFloat()

                    val totalCarbs = products.sumOf {
                        (it.product.carbs.toDouble() * it.weight / 100)
                    }.toFloat()
                    
                    return MealNutrition(totalProtein, totalFats, totalCarbs, totalCalories)
                }

                // Расчет totals с использованием finalSelection для правильного отслеживания изменений
                // Фильтруем только продукты из приёмов пищи текущего дня
                val currentMealIds = meals.map { it.id }
                val currentDayProducts = finalSelection.filter { it.mealId in currentMealIds }
                
                val totalCalories = currentDayProducts.sumOf {
                    (it.product.calories.toDouble() * it.weight / 100)
                }.toFloat()
                val totalProtein = currentDayProducts.sumOf {
                    (it.product.protein.toDouble() * it.weight / 100)
                }.toFloat()
                val totalFats = currentDayProducts.sumOf {
                    (it.product.fats.toDouble() * it.weight / 100)
                }.toFloat()
                val totalCarbs = currentDayProducts.sumOf {
                    (it.product.carbs.toDouble() * it.weight / 100)
                }.toFloat()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    // Диаграмма как первый элемент в LazyColumn
                    item {
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
                    items(meals) { meal ->
                        val products = finalSelection.filter { it.mealId == meal.id }
                        val nutrition = getMealNutrition(meal.id, products)

                        MealItem(
                            meal = meal,
                            products = products,
                            nutrition = nutrition,
                            onTimeClick = { mealId, newTime -> viewModel.updateMealTime(mealId, newTime) },
                            onAddProductClick = { m ->
                                if (!isNavigatingToAddProduct) {
                                    isNavigatingToAddProduct = true
                                    Log.d("MainScreen", "Начинаем навигацию на SelectProductScreen для mealId: ${m.id}")
                                    if (shouldShowWeightInput){
                                        scope.launch { viewModel.saveCurrentMeal(meal.id) }
                                    }
                                    viewModel.setEditingMealId(m.id)
                                    navController.navigate("selectProductWithMeal/${m.id}")
                                    
                                    // Сбрасываем флаг через 500мс, чтобы предотвратить множественные нажатия
                                    scope.launch {
                                        kotlinx.coroutines.delay(500)
                                        isNavigatingToAddProduct = false
                                        Log.d("MainScreen", "Флаг навигации сброшен")
                                    }
                                } else {
                                    Log.d("MainScreen", "Навигация уже выполняется, игнорируем нажатие")
                                }
                            },
                            onEditProduct = { p, m -> viewModel.editProductWeightInMeal(p, m.id, products.find { it.product == p }?.weight ?: 0) },
                            onDeleteProduct = { p, m ->
                                Log.d("DeleteProduct","Удаляемый продукт: $p, id приема пищи: ${m.id}")
                                viewModel.removeProductFromMeal(p, m.id) },
                            onMealOptionsClick = { m -> viewModel.removeMeal(m.id) }
                        )

                        // Добавляем кнопку "Добавить приём пищи" после последнего приёма пищи
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

                    // Если нет приёмов пищи, добавляем кнопку сразу после диаграммы
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

    // окно календаря
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