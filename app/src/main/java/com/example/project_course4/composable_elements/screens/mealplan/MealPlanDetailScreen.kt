package com.example.project_course4.composable_elements.screens.mealplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
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
import com.example.project_course4.Product
import com.example.project_course4.api.MealPlanComponentData
import com.example.project_course4.api.MealPlanData
import com.example.project_course4.api.MealPlanDayData
import com.example.project_course4.api.MealPlanMealData
import com.example.project_course4.composable_elements.charts.NutritionChart
import com.example.project_course4.ui.theme.CarbColor
import com.example.project_course4.ui.theme.FatColor
import com.example.project_course4.ui.theme.ProteinColor
import com.example.project_course4.viewmodel.ProductViewModel
import java.util.Locale

private val CARD_SHAPE = RoundedCornerShape(12.dp)

// ── Helpers ────────────────────────────────────────────────────────────────

private fun calcNutrition(
    components: List<MealPlanComponentData>,
    allProducts: List<Product>
): FloatArray {
    val p = components.sumOf { comp ->
        val prod = allProducts.find { it.productId == comp.productId } ?: return@sumOf 0.0
        prod.protein.toDouble() * comp.weight / 100.0
    }.toFloat()
    val f = components.sumOf { comp ->
        val prod = allProducts.find { it.productId == comp.productId } ?: return@sumOf 0.0
        prod.fats.toDouble() * comp.weight / 100.0
    }.toFloat()
    val c = components.sumOf { comp ->
        val prod = allProducts.find { it.productId == comp.productId } ?: return@sumOf 0.0
        prod.carbs.toDouble() * comp.weight / 100.0
    }.toFloat()
    val kcal = components.sumOf { comp ->
        val prod = allProducts.find { it.productId == comp.productId } ?: return@sumOf 0.0
        prod.calories.toDouble() * comp.weight / 100.0
    }.toFloat()
    return floatArrayOf(p, f, c, kcal)
}

// ── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanDetailScreen(
    navController: NavController,
    plan: MealPlanData,
    allProducts: List<Product>,
    productViewModel: ProductViewModel
) {
    var snackMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan.name, maxLines = 1) },
                navigationIcon = {
                    var navigating by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        if (!navigating) { navigating = true; navController.popBackStack() }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Целевые показатели ─────────────────────────────────────────
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Целевые показатели",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    NutritionChart(
                        protein = plan.targetProteinG,
                        fats = plan.targetFatsG,
                        carbs = plan.targetCarbsG,
                        totalCalories = plan.targetCalories,
                        targetProtein = plan.targetProteinG,
                        targetFats = plan.targetFatsG,
                        targetCarbs = plan.targetCarbsG,
                        targetCalories = plan.targetCalories,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            }

            // ── Фактические показатели ─────────────────────────────────────
            if (plan.days.isNotEmpty()) {
                item {
                    ActualNutritionBlock(plan = plan, allProducts = allProducts)
                }
            }

            // ── Описание ──────────────────────────────────────────────────
            if (plan.description.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CARD_SHAPE,
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Описание",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            plan.description.split("\n").forEach { paragraph ->
                                if (paragraph.isNotBlank()) {
                                    Text("\u2003$paragraph", fontSize = 15.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }

            // ── Дни с приёмами пищи ────────────────────────────────────────
            itemsIndexed(plan.days) { _, day ->
                PlanDayCard(
                    day = day,
                    allProducts = allProducts,
                    onCopyDay = {
                        productViewModel.copyPlanDayToClipboard(day, allProducts)
                        snackMessage = "День ${day.dayNumber} скопирован — перейдите в дневник и нажмите «Вставить день»"
                    },
                    onCopyMeal = { meal ->
                        productViewModel.copyPlanMealToClipboard(meal, allProducts)
                        snackMessage = "«${meal.name.ifBlank { "Приём пищи" }}» скопирован — перейдите в дневник и нажмите вставить"
                    }
                )
            }
        }
    }
}

// ── Фактические показатели блок ────────────────────────────────────────────

@Composable
private fun ActualNutritionBlock(plan: MealPlanData, allProducts: List<Product>) {
    var selectedIndex by remember { mutableStateOf(0) }
    val days = plan.days
    val day = days[selectedIndex]

    // Суммируем КБЖУ всех приёмов пищи выбранного дня
    val allComponents = day.meals.flatMap { it.components }
    val (p, f, c, kcal) = calcNutrition(allComponents, allProducts).let {
        arrayOf(it[0], it[1], it[2], it[3])
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Заголовок + стрелки
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Стрелка влево — у левого края
            IconButton(
                onClick = { if (selectedIndex > 0) selectedIndex-- },
                enabled = selectedIndex > 0,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Предыдущий день",
                    tint = if (selectedIndex > 0) MaterialTheme.colorScheme.onSurface
                           else Color.LightGray
                )
            }
            // Заголовок по центру
            Text(
                "Фактические показатели",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            // Стрелка вправо — у правого края
            IconButton(
                onClick = { if (selectedIndex < days.lastIndex) selectedIndex++ },
                enabled = selectedIndex < days.lastIndex,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Следующий день",
                    tint = if (selectedIndex < days.lastIndex) MaterialTheme.colorScheme.onSurface
                           else Color.LightGray
                )
            }
        }

        // Название дня по центру
        Text(
            "День ${day.dayNumber}",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        NutritionChart(
            protein = p as Float,
            fats = f as Float,
            carbs = c as Float,
            totalCalories = kcal as Float,
            targetProtein = plan.targetProteinG,
            targetFats = plan.targetFatsG,
            targetCarbs = plan.targetCarbsG,
            targetCalories = plan.targetCalories,
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )
    }
}

// ── День ──────────────────────────────────────────────────────────────────

@Composable
private fun PlanDayCard(
    day: MealPlanDayData,
    allProducts: List<Product>,
    onCopyDay: () -> Unit,
    onCopyMeal: (MealPlanMealData) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CARD_SHAPE,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("День ${day.dayNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (day.meals.isNotEmpty()) {
                    TextButton(
                        onClick = onCopyDay,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Скопировать день", fontSize = 12.sp, color = Color(0xFF4CAF50))
                    }
                }
            }

            if (day.meals.isEmpty()) {
                Text(
                    "Нет приёмов пищи",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Spacer(Modifier.height(8.dp))
                day.meals.forEachIndexed { index, meal ->
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    PlanMealCard(meal = meal, allProducts = allProducts, onCopy = { onCopyMeal(meal) })
                }
                if (day.notes?.isNotBlank() == true) {
                    Spacer(Modifier.height(8.dp))
                    day.notes.split("\n").forEach { paragraph ->
                        if (paragraph.isNotBlank()) {
                            Text("\u2003$paragraph", fontSize = 13.sp, color = Color(0xFF757575))
                        }
                    }
                }
            }
        }
    }
}

// ── Приём пищи ─────────────────────────────────────────────────────────────

@Composable
private fun PlanMealCard(
    meal: MealPlanMealData,
    allProducts: List<Product>,
    onCopy: () -> Unit
) {
    val nutrition = calcNutrition(meal.components, allProducts)
    val mealProtein = nutrition[0]; val mealFats = nutrition[1]
    val mealCarbs = nutrition[2]; val mealKcal = nutrition[3]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Заголовок приёма
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        meal.name.ifBlank { "Приём пищи" },
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    if (meal.mealTime.isNotBlank()) {
                        Text(meal.mealTime, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Скопировать приём",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Продукты с КБЖУ
            if (meal.components.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                meal.components.forEach { comp ->
                    val product = allProducts.find { it.productId == comp.productId }
                    val w = comp.weight / 100f
                    val pp = (product?.protein ?: 0f) * w
                    val ff = (product?.fats ?: 0f) * w
                    val cc = (product?.carbs ?: 0f) * w
                    val kk = (product?.calories ?: 0f) * w

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                product?.name ?: "Продукт #${comp.productId}",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${comp.weight}г", fontSize = 12.sp, color = Color.Gray)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                String.format(Locale.getDefault(), "Б %.1f", pp),
                                fontSize = 11.sp,
                                color = ProteinColor
                            )
                            Text(
                                String.format(Locale.getDefault(), "Ж %.1f", ff),
                                fontSize = 11.sp,
                                color = FatColor
                            )
                            Text(
                                String.format(Locale.getDefault(), "У %.1f", cc),
                                fontSize = 11.sp,
                                color = CarbColor
                            )
                            Text(
                                String.format(Locale.getDefault(), "%.0f ккал", kk),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Итого по приёму
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            String.format(Locale.getDefault(), "Б %.1f", mealProtein),
                            fontSize = 12.sp,
                            color = ProteinColor,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            String.format(Locale.getDefault(), "Ж %.1f", mealFats),
                            fontSize = 12.sp,
                            color = FatColor,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            String.format(Locale.getDefault(), "У %.1f", mealCarbs),
                            fontSize = 12.sp,
                            color = CarbColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        String.format(Locale.getDefault(), "%.0f ккал", mealKcal),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
