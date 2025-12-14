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
    val selectedProductsList = selectedProducts.toList()
    Log.d("api_test", "$selectedProducts")

    val totalCalories = selectedProducts.sumOf { it.calories.toDouble() }.toFloat()
    val totalProtein = selectedProducts.sumOf { it.protein.toDouble() }.toFloat()
    val totalFats = selectedProducts.sumOf { it.fats.toDouble() }.toFloat()
    val totalCarbs = selectedProducts.sumOf { it.carbs.toDouble() }.toFloat()
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White),
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
            ) { items(selectedProductsList){ product ->
                DishItem(
                    dishName = product.name,
                    proteins = product.protein,
                    fats = product.fats,
                    carbs = product.carbs,
                    calories = product.calories,
                    weight = 100
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
            ){
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ){
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
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Button(
                onClick = {
                    navController.navigate(Screen.SelectProduct.route)
                },
                modifier = Modifier
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
}

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