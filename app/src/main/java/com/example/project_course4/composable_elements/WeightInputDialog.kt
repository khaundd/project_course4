package com.example.project_course4.composable_elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_course4.Product
import com.example.project_course4.viewmodel.ProductViewModel

@Composable
fun WeightInputDialog(
    product: Product,
    viewModel: ProductViewModel,
    onDismiss: () -> Unit
) {
    var weightInput by remember { mutableStateOf("0") }

    // при инициализации компонента определяем начальный вес
    LaunchedEffect(product) {
        if (viewModel.isAddingFromList.value) {
            weightInput = "0"
        } else {
            // в режиме редактирования показываем текущий вес продукта
            // Ищем продукт с таким же productId И mealId
            val currentMealId = viewModel.editingMealId.value
            val existingProduct = viewModel.finalSelection.value.find { 
                it.product.productId == product.productId && it.mealId == currentMealId 
            }
            if (existingProduct != null) {
                weightInput = existingProduct.weight.toString()
            } else {
                weightInput = "0"
            }
        }
    }

    // отслеживание состояния ввода веса
    val shouldShowWeightInput by viewModel.shouldShowWeightInput.collectAsState()

    LaunchedEffect(shouldShowWeightInput) {
        if (!shouldShowWeightInput) {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (viewModel.currentProductForWeight.value != null) {
                viewModel.skipCurrentProduct()
            }
        },
        title = {
            Text("Введите вес продукта")
        },
        text = {
            Column {
                Text(product.name)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                            weightInput = newValue
                        }
                    },
                    label = { Text("Вес (г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Информация о КБЖУ на 100г
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "На 100г: ${product.calories} ккал, " +
                            "Б: ${product.protein}г, Ж: ${product.fats}г, У: ${product.carbs}г",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        viewModel.skipCurrentProduct()
                    }
                ) {
                    Text("Отмена")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val weight = weightInput.toIntOrNull() ?: 0
                        viewModel.addProductWithWeight(weight)
                        weightInput = "0"
                        // Проверяем, есть ли еще продукты для обработки
                        if (viewModel.pendingProducts.value.isEmpty()) {
                            onDismiss()
                        }
                    }
                ) {
                    Text("+")
                }
            }
        }
    )
}