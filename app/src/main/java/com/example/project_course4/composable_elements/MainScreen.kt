package com.example.project_course4.composable_elements

import android.util.Log
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.project_course4.Screen
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import com.example.project_course4.ProductViewModel

@Composable
fun MainScreen(
    navController: NavController,
    viewModel: ProductViewModel,
) {
    // Получаем финальный выбор продуктов из ViewModel
    val selectedProducts by viewModel.finalSelection.collectAsState()
    val currentProductForWeight by viewModel.currentProductForWeight.collectAsState()
    val shouldShowWeightInput by viewModel.shouldShowWeightInput.collectAsState()

    // LaunchedEffect для автоматического запуска ввода веса
    LaunchedEffect(shouldShowWeightInput) {
        if (shouldShowWeightInput) {
            viewModel.checkAndStartWeightInput()
        }
    }

    // Проверяем, есть ли продукты для ввода веса
    if (currentProductForWeight != null) {
        WeightInputDialog(
            product = currentProductForWeight!!,
            viewModel = viewModel,
            onDismiss = { viewModel.clearWeightInput() }
        )
    }

    // Рассчитываем общие значения с учетом веса
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

    Box(  // ЗАМЕНИЛИ Column на Box
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),  // ДОБАВИЛИ padding снизу для кнопки
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
                        val product = selectedProduct.product
                        DishItem(
                            dishName = product.name,
                            proteins = product.protein * selectedProduct.weight / 100,
                            fats = product.fats * selectedProduct.weight / 100,
                            carbs = product.carbs * selectedProduct.weight / 100,
                            calories = product.calories * selectedProduct.weight / 100,
                            weight = selectedProduct.weight
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Б: ${String.format("%.1f", totalProtein)}")
                        Spacer(Modifier.padding(start = 5.dp))
                        Text("Ж: ${String.format("%.1f", totalFats)}")
                        Spacer(Modifier.padding(start = 5.dp))
                        Text("У: ${String.format("%.1f", totalCarbs)}")
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${String.format("%.0f", totalCalories)} ккал.", Modifier.padding(end = 5.dp))
                }
            }
            // УДАЛИЛИ Spacer и Box с кнопкой отсюда
        }

        // ПЕРЕМЕСТИЛИ кнопку сюда - в конец Box
        Button(
            onClick = {
                navController.navigate(Screen.SelectProduct.route)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)  // ДОБАВИЛИ align
                .padding(bottom = 16.dp, end = 16.dp)
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

//@Composable
//fun MainScreen(
//    navController: NavController,
//    viewModel: ProductViewModel,
//) {
//    // Получаем финальный выбор продуктов из ViewModel
//    val selectedProducts by viewModel.finalSelection.collectAsState()
//    val currentProductForWeight by viewModel.currentProductForWeight.collectAsState()
//    val shouldShowWeightInput by viewModel.shouldShowWeightInput.collectAsState()
//
//    // LaunchedEffect для автоматического запуска ввода веса
//    LaunchedEffect(shouldShowWeightInput) {
//        if (shouldShowWeightInput) {
//            viewModel.checkAndStartWeightInput()
//        }
//    }
//
//    // Проверяем, есть ли продукты для ввода веса
//    if (currentProductForWeight != null) {
//        WeightInputDialog(
//            product = currentProductForWeight!!,
//            viewModel = viewModel,
//            onDismiss = { viewModel.clearWeightInput() }
//        )
//    }
//
//    // Рассчитываем общие значения с учетом веса
//    val totalCalories = selectedProducts.sumOf {
//        (it.product.calories.toDouble() * it.weight / 100)
//    }.toFloat()
//
//    val totalProtein = selectedProducts.sumOf {
//        (it.product.protein.toDouble() * it.weight / 100)
//    }.toFloat()
//
//    val totalFats = selectedProducts.sumOf {
//        (it.product.fats.toDouble() * it.weight / 100)
//    }.toFloat()
//
//    val totalCarbs = selectedProducts.sumOf {
//        (it.product.carbs.toDouble() * it.weight / 100)
//    }.toFloat()
//
//    Column(
//        Modifier
//            .fillMaxSize()
//            .background(Color.White),
//        verticalArrangement = Arrangement.spacedBy(4.dp)
//    ) {
//        Column {
//            Box(
//                Modifier
//                    .fillMaxWidth()
//                    .height(50.dp)
//                    .background(Color.LightGray)
//            )
//            LazyColumn(
//                Modifier
//                    .weight(1f)
//                    .padding(horizontal = 10.dp, vertical = 15.dp)
//            ) { items(selectedProducts){ selectedProduct ->
//                val product = selectedProduct.product
//                DishItem(
//                    dishName = product.name,
//                    proteins = product.protein*selectedProduct.weight/100,
//                    fats = product.fats*selectedProduct.weight/100,
//                    carbs = product.carbs*selectedProduct.weight/100,
//                    calories = product.calories*selectedProduct.weight/100,
//                    weight = selectedProduct.weight
//                )
//            }
//            }
//            HorizontalDivider(
//                color = Color.LightGray,
//                thickness = 1.dp,
//                modifier = Modifier
//                    .padding(horizontal = 5.dp)
//            )
//            Row(
//                Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 10.dp, vertical = 10.dp)
//            ){
//                Row(
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ){
//                    Text("Б: ${String.format("%.1f", totalProtein)}")
//                    Spacer(Modifier.padding(start = 5.dp))
//                    Text("Ж: ${String.format("%.1f", totalFats)}")
//                    Spacer(Modifier.padding(start = 5.dp))
//                    Text("У: ${String.format("%.1f", totalCarbs)}")
//                }
//                Spacer(Modifier.weight(1f))
//                Text("${String.format("%.0f", totalCalories)} ккал.", Modifier.padding(end = 5.dp))
//            }
//        }
////        Spacer(Modifier.weight(1f))
//        Box(
//            modifier = Modifier.fillMaxWidth(),
//            contentAlignment = Alignment.CenterEnd
//        ) {
//            Button(
//                onClick = {
//                    navController.navigate(Screen.SelectProduct.route)
//                },
//                modifier = Modifier
//                    .padding(bottom = 16.dp, end = 16.dp)
//                    .size(60.dp),
//                shape = CircleShape,
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color(0xFF2196F3),
//                    contentColor = Color.White
//                )
//            ) {
//                Text("+", fontSize = 24.sp)
//            }
//        }
//    }
//}

//@Composable
//@Preview
//fun MainViewPreview() {
//    MainScreen(
//        navController = rememberNavController(),
//        selectedProducts = listOf(
//            Product("Яблоко", 1f, 2f, 3f, 10f),
//            Product("Апельсин", 2f, 3f, 4f, 20f)),
//        onItemsSelected = {})
//}