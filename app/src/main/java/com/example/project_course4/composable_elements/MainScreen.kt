package com.example.project_course4.composable_elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.MealNutrition
import com.example.project_course4.Product
import com.example.project_course4.SelectedProduct
import com.example.project_course4.Screen
import com.example.project_course4.ui.theme.CarbColor
import com.example.project_course4.ui.theme.FatColor
import com.example.project_course4.ui.theme.ProteinColor
import com.example.project_course4.composable_elements.charts.NutritionChart
import com.example.project_course4.viewmodel.ProductViewModel

@Composable
fun MainScreen(
    navController: NavController,
    viewModel: ProductViewModel
) {
    // Инициализация приёмов пищи при первом запуске
    LaunchedEffect(Unit) {
        viewModel.initializeMeals()
    }
    
    // Получаем список приёмов пищи из ViewModel
    val meals by viewModel.meals.collectAsState()
    val currentProductForWeight by viewModel.currentProductForWeight.collectAsState()
    val shouldShowWeightInput by viewModel.shouldShowWeightInput.collectAsState()

    // автоматический запуск ввода веса
    LaunchedEffect(shouldShowWeightInput) {
        if (shouldShowWeightInput) {
            viewModel.checkAndStartWeightInput()
        }
    }

    if (currentProductForWeight != null && shouldShowWeightInput) {
        WeightInputDialog(
            product = currentProductForWeight!!,
            viewModel = viewModel,
            onDismiss = { viewModel.clearWeightInput() }
        )
    }

    // Общие итоги по всем приёмам пищи (если нужно)
    val totalCalories = meals.sumOf { meal ->
        viewModel.getMealNutrition(meal.id).calories.toDouble()
    }.toFloat()

    val totalProtein = meals.sumOf { meal ->
        viewModel.getMealNutrition(meal.id).protein.toDouble()
    }.toFloat()

    val totalFats = meals.sumOf { meal ->
        viewModel.getMealNutrition(meal.id).fats.toDouble()
    }.toFloat()

    val totalCarbs = meals.sumOf { meal ->
        viewModel.getMealNutrition(meal.id).carbs.toDouble()
    }.toFloat()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Полукруговая диаграмма БЖУ
            NutritionChart(
                protein = totalProtein,
                fats = totalFats,
                carbs = totalCarbs,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .padding(start = 16.dp, end = 16.dp, top = 104.dp, bottom = 104.dp)
            )
            // Список приёмов пищи
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(meals) { meal ->
                    val products = viewModel.getProductsForMeal(meal.id)
                    val nutrition = viewModel.getMealNutrition(meal.id)
                    
                    MealItem(
                        meal = meal,
                        products = products,
                        nutrition = nutrition,
                        onTimeClick = { selectedMeal ->
                            // Передаём новый объект Meal с обновлённым временем
                            viewModel.updateMealTime(selectedMeal.id, selectedMeal.time)
                        },
                        onAddProductClick = { selectedMeal ->
                            // Загружаем продукты и переходим к экрану выбора продукта
                            viewModel.loadProducts()
                            // Передаем mealId для последующего добавления продукта в конкретный приём пищи
                            viewModel.setEditingMealId(selectedMeal.id)
                            navController.navigate("selectProductWithMeal/${selectedMeal.id}")
                        },
                        onEditProduct = { product, selectedMeal ->
                            // Редактирование веса продукта
                            viewModel.editProductWeightInMeal(product, selectedMeal.id, products.find { it.product == product }?.weight ?: 0)
                        },
                        onDeleteProduct = { product, selectedMeal ->
                            // Удаление продукта из приёма пищи
                            viewModel.removeProductFromMeal(product, selectedMeal.id)
                        },
                        onMealOptionsClick = { selectedMeal ->
                            // Обработка нажатия на кнопку с тремя точками (удаление приёма пищи)
                            viewModel.removeMeal(selectedMeal.id)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Кнопка "Добавить приём пищи"
            Button(
                onClick = { viewModel.addMeal() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0E0E0)
                )
            ) {
                Text("Добавить приём пищи")
            }
            

        }
    }
}