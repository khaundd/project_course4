package com.example.project_course4.composable_elements.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    profileViewModel: ProfileViewModel,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Закрываем drawer каждый раз, когда MainScreen становится активным (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { drawerState.snapTo(DrawerValue.Closed) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val macroNutrients by profileViewModel.macroNutrients.collectAsState()
    val dailyCalories by profileViewModel.dailyCalories.collectAsState()

    val meals by viewModel.meals.collectAsState()
    val dayClipboard by viewModel.dayClipboard.collectAsState()

    var showCustomCalendar by remember { mutableStateOf(false) }
    var selectedLocalDate by remember { mutableStateOf(LocalDate.now()) }
    var isNavigatingToAddProduct by remember { mutableStateOf(false) }

    // Состояния раскрытия секций меню (только один раздел открыт одновременно)
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val nutritionExpanded = expandedSection == "nutrition"
    val activityExpanded = expandedSection == "activity"

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Меню", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)

                // Профиль
                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text("Профиль") },
                    selected = currentRoute == Screen.Profile.route,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screen.Profile.route)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Питание (раскрывающаяся секция)
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    label = { Text("Питание") },
                    selected = false,
                    badge = {
                        Icon(
                            if (nutritionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    },
                    onClick = { expandedSection = if (nutritionExpanded) null else "nutrition" },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                if (nutritionExpanded) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                        label = { Text("Дневник питания") },
                        selected = currentRoute == Screen.Main.route,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.Main.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(start = 28.dp, end = 8.dp, top = 0.dp, bottom = 0.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                        label = { Text("Создать продукт") },
                        selected = currentRoute?.startsWith(Screen.ProductCreation.route) == true,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                navController.navigate("productCreation?barcode=")
                            }
                        },
                        modifier = Modifier.padding(start = 28.dp, end = 8.dp, top = 0.dp, bottom = 0.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Рецепты") },
                        selected = currentRoute == Screen.Recipes.route,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.Recipes.route)
                            }
                        },
                        modifier = Modifier.padding(start = 28.dp, end = 8.dp, top = 0.dp, bottom = 0.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
                        label = { Text("Планы питания") },
                        selected = currentRoute == Screen.MealPlans.route,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.MealPlans.route)
                            }
                        },
                        modifier = Modifier.padding(start = 28.dp, end = 8.dp, top = 0.dp, bottom = 0.dp)
                    )
                }

                // Активность (раскрывающаяся секция)
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                    label = { Text("Активность") },
                    selected = false,
                    badge = {
                        Icon(
                            if (activityExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    },
                    onClick = { expandedSection = if (activityExpanded) null else "activity" },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                if (activityExpanded) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                        label = { Text("Каталог упражнений") },
                        selected = currentRoute == Screen.ExerciseCatalog.route,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.ExerciseCatalog.route)
                            }
                        },
                        modifier = Modifier.padding(start = 28.dp, end = 8.dp, top = 0.dp, bottom = 0.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Дневник тренировок") },
                        selected = currentRoute == Screen.TrainingLog.route,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.TrainingLog.route)
                            }
                        },
                        modifier = Modifier.padding(start = 28.dp, end = 8.dp, top = 0.dp, bottom = 0.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
                        label = { Text("Планы тренировок") },
                        selected = currentRoute == Screen.TrainingPlans.route,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(Screen.TrainingPlans.route)
                            }
                        },
                        modifier = Modifier.padding(start = 28.dp, end = 8.dp, top = 0.dp, bottom = 0.dp)
                    )
                }
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
                        IconButton(
                            onClick = {
                                if (!drawerState.isOpen) {
                                    scope.launch { drawerState.open() }
                                }
                            },
                            enabled = !drawerState.isOpen
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                        }
                    },
                    actions = {
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
                                    onClick = {
                                        viewModel.copyDayToClipboard()
                                        menuExpanded = false
                                    }
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
                                    onClick = {
                                        viewModel.pasteDayFromClipboard(selectedLocalDate)
                                        menuExpanded = false
                                    }
                                )
                            }
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
