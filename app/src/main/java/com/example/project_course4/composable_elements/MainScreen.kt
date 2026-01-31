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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.ProductViewModel
import com.example.project_course4.Screen
import com.example.project_course4.ui.theme.CarbColor
import com.example.project_course4.ui.theme.FatColor
import com.example.project_course4.ui.theme.ProteinColor

@Composable
fun MainScreen(
    navController: NavController,
    viewModel: ProductViewModel,
) {
    // Получаем финальный выбор продуктов из ViewModel
    val selectedProducts by viewModel.finalSelection.collectAsState()
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

    val totalCalories = selectedProducts.sumOf {
        (it.product.calories.toDouble() * it.weight / 100)
    }.toFloat()

    val totalProtein = selectedProducts.sumOf {
        (it.product.protein.toDouble() * it.weight / 100)
    }.toFloat()

    val totalFats = selectedProducts.sumOf {
        (it.product.fats.toDouble() * it.weight / 100)
    }.toFloat()

    val totalCarbs = selectedProducts.sumOf {
        (it.product.carbs.toDouble() * it.weight / 100)
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color.LightGray)
                )
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 15.dp)
                ) {
                    items(selectedProducts) { selectedProduct ->
                        var showOptions by remember { mutableStateOf(false) }

                        val product = selectedProduct.product
                        DishItem(
                            dishName = product.name,
                            proteins = product.protein * selectedProduct.weight / 100,
                            fats = product.fats * selectedProduct.weight / 100,
                            carbs = product.carbs * selectedProduct.weight / 100,
                            calories = product.calories * selectedProduct.weight / 100,
                            weight = selectedProduct.weight,
                            onEdit = {
                                showOptions = false
                                // Для редактирования используем существующую функцию editProductWeight
                                viewModel.editProductWeight(product, selectedProduct.weight)
                            },
                            onDelete = {
                                showOptions = false
                                viewModel.removeProduct(product)
                            }
                        )
                    }
                }
                HorizontalDivider(
                    color = Color.LightGray,
                    thickness = 1.dp,
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = String.format("%.1f", totalProtein), color = ProteinColor)
                        Spacer(modifier = Modifier.padding(start = 12.dp))
                        Text(text = String.format("%.1f", totalFats), color = FatColor)
                        Spacer(modifier = Modifier.padding(start = 12.dp))
                        Text(text = String.format("%.1f", totalCarbs), color = CarbColor)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(text = String.format("%.0f ккал.", totalCalories), modifier = Modifier.padding(end = 5.dp))
                }
            }
        }
        
        // Кнопка + поверх всех элементов в правом нижнем углу
        Button(
            onClick = {
                viewModel.loadProducts()
                navController.navigate(Screen.SelectProduct.route)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(60.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            )
        ) {
            Text("+", fontSize = 24.sp)
        }
    }
}