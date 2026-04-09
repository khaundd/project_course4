package com.example.project_course4.composable_elements.screens.recipe

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.Product
import com.example.project_course4.R
import com.example.project_course4.Screen
import com.example.project_course4.composable_elements.charts.BJUCircularChartWithLegend
import com.example.project_course4.viewmodel.RecipeCreationViewModel
import com.example.project_course4.R.drawable.ic_close_24px
import com.example.project_course4.R.drawable.ic_check_24px
import com.example.project_course4.composable_elements.CustomButton
import com.example.project_course4.composable_elements.LabeledTransparentTextField
import com.example.project_course4.composable_elements.TransparentTextField
import com.example.project_course4.composable_elements.dialogs.StandaloneWeightInputDialog

data class RecipeIngredientItem(
    val product: Product,
    val weight: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCreationScreen(
    navController: NavController,
    viewModel: RecipeCreationViewModel,
    onSaveSuccess: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingProduct by viewModel.pendingProductForWeight.collectAsState()
    val focusManager = LocalFocusManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingIngredient by remember { mutableStateOf<Pair<Int, RecipeIngredientItem>?>(null) }

    // Диалог редактирования веса существующего ингредиента
    editingIngredient?.let { (index, item) ->
        StandaloneWeightInputDialog(
            product = item.product,
            initialWeight = item.weight,
            showDelete = true,
            onConfirm = { weight ->
                viewModel.updateIngredientWeightAt(index, weight)
                editingIngredient = null
            },
            onDelete = { viewModel.removeIngredientAt(index) },
            onDismiss = { editingIngredient = null }
        )
    }

    // Диалог ввода веса нового продукта
    if (pendingProduct != null) {
        StandaloneWeightInputDialog(
            product = pendingProduct!!,
            initialWeight = 0,
            showDelete = false,
            onConfirm = { weight -> viewModel.confirmProductWeight(weight) },
            onDismiss = { viewModel.cancelPendingProduct() }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (state.editingProductId != null) "Редактировать рецепт" else "Новый рецепт", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(ic_close_24px),
                            contentDescription = "Закрыть"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveRecipe(onSuccess = { 
                            if (onSaveSuccess != null) onSaveSuccess() else navController.popBackStack()
                        }) },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = ImageVector.vectorResource(ic_check_24px),
                                contentDescription = "Сохранить"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Название рецепта
            TransparentTextField(
                value = state.name,
                onValueChange = { viewModel.updateName(it) },
                placeholder = "Мой рецепт",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Если продукты есть — показываем диаграмму и список
            if (state.ingredients.isNotEmpty()) {
                RecipeNutritionSection(
                    ingredients = state.ingredients,
                    selectedTab = selectedTab,
                    portionWeight = state.portionWeight,
                    totalIngredientsWeight = state.totalIngredientsWeight,
                    afterCookingWeight = state.afterCookingWeight
                )
                Spacer(modifier = Modifier.height(8.dp))
                RecipeIngredientsList(
                    ingredients = state.ingredients,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    portionWeight = state.portionWeight,
                    onEditIngredient = { index, item -> editingIngredient = index to item },
                    onDeleteIngredient = { index, _ -> viewModel.removeIngredientAt(index) }
                )
            } else {
                // Пустое состояние
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Давайте добавим пару продуктов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопка Добавить
            CustomButton(
                text = "+ Добавить",
                backgroundColor = colorResource(id = R.color.buttonColor),
                textColor = Color.White,
                cornerRadius = 50.dp,
                modifier = Modifier.fillMaxWidth(0.7f),
                onClick = {
                    viewModel.prepareForProductSelection()
                    navController.navigate(Screen.SelectProductForRecipe.route)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()

            // Вес продуктов (вычисляется автоматически)
            LabeledTransparentTextField(
                label = "Вес продуктов",
                unit = "г",
                value = state.totalIngredientsWeight.let { if (it > 0) it.toString() else "0" },
                onValueChange = {},
                enabled = false
            )

            HorizontalDivider()

            // Вес после приготовления
            LabeledTransparentTextField(
                label = "Вес после приготовления",
                unit = "г",
                value = state.afterCookingWeight,
                onValueChange = { viewModel.updateAfterCookingWeight(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = "0"
            )

            HorizontalDivider()

            // Порция
            LabeledTransparentTextField(
                label = "Порция",
                unit = "г",
                value = state.portionWeight,
                onValueChange = { viewModel.updatePortionWeight(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = "100"
            )

            HorizontalDivider()

            // Количество порций (вычисляется автоматически)
            LabeledTransparentTextField(
                label = "Порций",
                unit = "",
                value = state.portionsCount.let {
                    if (it > 0f) "%.1f".format(it) else "0"
                },
                onValueChange = {},
                enabled = false
            )

            HorizontalDivider()

            // Описание
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Описание:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { if (it.length <= 500) viewModel.updateDescription(it) },
                    placeholder = { Text("Введите текст", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    )
                )
            }

            Text(
                text = "${state.description.length} / 500",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Ошибка
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RecipeNutritionSection(
    ingredients: List<RecipeIngredientItem>,
    selectedTab: Int,
    portionWeight: String,
    totalIngredientsWeight: Int,
    afterCookingWeight: String
) {
    // Коэффициент приготовления
    val cookingWeight = afterCookingWeight.toFloatOrNull()?.takeIf { it > 0 }
        ?: totalIngredientsWeight.toFloat()
    val cookingCoeff = if (cookingWeight > 0) totalIngredientsWeight / cookingWeight else 1f

    // Базовые КБЖУ (всего, с учётом коэффициента)
    val baseProtein = ingredients.fold(0f) { acc, i -> acc + i.product.protein * i.weight / 100f } * cookingCoeff
    val baseFats    = ingredients.fold(0f) { acc, i -> acc + i.product.fats    * i.weight / 100f } * cookingCoeff
    val baseCarbs   = ingredients.fold(0f) { acc, i -> acc + i.product.carbs   * i.weight / 100f } * cookingCoeff
    val baseCal     = ingredients.fold(0f) { acc, i -> acc + i.product.calories * i.weight / 100f } * cookingCoeff

    val (protein, fats, carbs, calories) = when (selectedTab) {
        1 -> { // на 100г
            val w = cookingWeight
            if (w > 0) listOf(baseProtein / w * 100f, baseFats / w * 100f, baseCarbs / w * 100f, baseCal / w * 100f)
            else listOf(0f, 0f, 0f, 0f)
        }
        2 -> { // на порцию
            val portion = portionWeight.toFloatOrNull() ?: 100f
            if (cookingWeight > 0) listOf(baseProtein / cookingWeight * portion,
                baseFats / cookingWeight * portion,
                baseCarbs / cookingWeight * portion, baseCal / cookingWeight * portion)
            else listOf(0f, 0f, 0f, 0f)
        }
        else -> listOf(baseProtein, baseFats, baseCarbs, baseCal)
    }

    BJUCircularChartWithLegend(
        protein = protein,
        fats = fats,
        carbs = carbs,
        calories = calories
    )
}

@Composable
private fun RecipeIngredientsList(
    ingredients: List<RecipeIngredientItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    portionWeight: String,
    onEditIngredient: (Int, RecipeIngredientItem) -> Unit,
    onDeleteIngredient: (Int, RecipeIngredientItem) -> Unit
) {
    val portionGrams = portionWeight.toFloatOrNull()?.takeIf { it > 0 } ?: 100f
    val tabs = listOf("Всего", "100г", "Порция ${portionGrams.toInt()}г")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            FilterChip(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                label = { Text(label, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    ingredients.forEachIndexed { index, item ->
        RecipeIngredientRow(
            item = item,
            onEdit = { onEditIngredient(index, item) },
            onDelete = { onDeleteIngredient(index, item) }
        )
        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
    }
}

@Composable
private fun RecipeIngredientRow(
    item: RecipeIngredientItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Б: ${"%.1f".format(item.product.protein * item.weight / 100f)} | " +
                       "Ж: ${"%.1f".format(item.product.fats * item.weight / 100f)} | " +
                       "У: ${"%.1f".format(item.product.carbs * item.weight / 100f)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${item.weight} г", style = MaterialTheme.typography.bodyMedium)
            Text(
                "${"%.0f".format(item.product.calories * item.weight / 100f)} кКал",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Удалить",
                tint = Color.Gray
            )
        }
    }
}


