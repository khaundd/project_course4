package com.example.project_course4.composable_elements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_course4.Meal
import com.example.project_course4.Product
import com.example.project_course4.SelectedProduct
import com.example.project_course4.MealNutrition
import com.example.project_course4.R
import com.example.project_course4.composable_elements.auth.TextButtonRedirect
import java.time.format.DateTimeFormatter
import com.example.project_course4.composable_elements.dialogs.TimePickerDialog
import java.time.LocalTime

@Composable
fun MealItem(
    meal: Meal,
    products: List<SelectedProduct>,
    nutrition: MealNutrition,
    onTimeClick: (Int, LocalTime) -> Unit,
    onAddProductClick: (Meal) -> Unit,
    onEditProduct: (Product, Meal) -> Unit,
    onDeleteProduct: (Product, Meal) -> Unit,
    onMealOptionsClick: (Meal) -> Unit,
    mealBackgroundColor: Color = Color(0xFFF5F5F5)
) {
    var showOptions by remember { mutableStateOf(false) }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var showTimePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = mealBackgroundColor,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        
        if (showTimePicker) {
            TimePickerDialog(
                initialTime = meal.time,
                onTimeSelected = { newTime ->
                    // Вызываем onTimeClick с новым временем
                    onTimeClick(meal.id, newTime)
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = meal.time.format(timeFormatter),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showTimePicker = true }
            )
            
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Удалить приём пищи",
                tint = Color.Red,
                modifier = Modifier
                    .clickable { onMealOptionsClick(meal) }
                    .padding(8.dp)
                    .size(24.dp)
            )
        }
        
        HorizontalDivider(
            color = Color.Black,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (products.isEmpty()) {
                Text(
                    text = "Здесь ничего нет",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    TextButtonRedirect(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp),
                        text = "Добавить",
                        normalColor = colorResource(id = R.color.textButtonRedirectColor),
                        pressedColor = colorResource(id = R.color.buttonColor),
                        onClick = { onAddProductClick(meal) },
                        textDecoration = null
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    products.forEachIndexed { index, selectedProduct ->
                        DishItem(
                            dishName = selectedProduct.product.name,
                            proteins = selectedProduct.product.protein * selectedProduct.weight / 100,
                            fats = selectedProduct.product.fats * selectedProduct.weight / 100,
                            carbs = selectedProduct.product.carbs * selectedProduct.weight / 100,
                            calories = selectedProduct.product.calories * selectedProduct.weight / 100,
                            weight = selectedProduct.weight,
                            onEdit = { onEditProduct(selectedProduct.product, meal) },
                            onDelete = { onDeleteProduct(selectedProduct.product, meal) }
                        )
                        if (index < products.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButtonRedirect(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp),
                            text = "Добавить",
                            normalColor = colorResource(id = R.color.textButtonRedirectColor),
                            pressedColor = colorResource(id = R.color.buttonColor),
                            onClick = { onAddProductClick(meal) }
                        )
                    }
                }
            }
            
            if (products.isNotEmpty()) {
                HorizontalDivider(
                    color = Color.Black,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = String.format("%.1f", nutrition.protein), 
                            color = com.example.project_course4.ui.theme.ProteinColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = String.format("%.1f", nutrition.fats), 
                            color = com.example.project_course4.ui.theme.FatColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = String.format("%.1f", nutrition.carbs), 
                            color = com.example.project_course4.ui.theme.CarbColor
                        )
                    }
                    Text(
                        text = String.format("%.0f ккал.", nutrition.calories), 
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }
    }
}