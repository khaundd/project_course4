package com.example.project_course4.composable_elements.dialogs

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.project_course4.Product
import com.example.project_course4.R
import com.example.project_course4.composable_elements.CustomButton

/**
 * Standalone-версия WeightInputDialog без зависимости от ProductViewModel.
 * Используется в редакторе плана питания.
 *
 * @param product продукт для отображения
 * @param initialWeight начальный вес (0 для добавления, текущий для редактирования)
 * @param showDelete показывать ли кнопку удаления (только в режиме редактирования)
 * @param onConfirm вызывается с введённым весом при подтверждении
 * @param onDelete вызывается при нажатии на корзину (только если showDelete = true)
 * @param onDismiss вызывается при закрытии диалога
 */
@Composable
fun StandaloneWeightInputDialog(
    product: Product,
    initialWeight: Int = 0,
    showDelete: Boolean = false,
    onConfirm: (Int) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var weightInput by remember(product.productId) { mutableStateOf(if (initialWeight > 0) initialWeight.toString() else "0") }

    val weight = weightInput.toIntOrNull() ?: 0
    val displayCalories = product.calories * weight / 100f
    val displayProtein = product.protein * weight / 100f
    val displayFats = product.fats * weight / 100f
    val displayCarbs = product.carbs * weight / 100f

    val primaryColor = colorResource(id = R.color.buttonColor)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = product.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StandaloneNutrientColumn("Белки", displayProtein, Color(0xFF4CAF50))
                    StandaloneNutrientColumn("Жиры", displayFats, Color(0xFFFFC107))
                    StandaloneNutrientColumn("Углево...", displayCarbs, Color(0xFFFF5722))
                    StandaloneNutrientColumn("кКал", displayCalories, Color.Black, bold = true)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        listOf(-10, -50, -100).forEach { delta ->
                            StandaloneStepButton(
                                label = "$delta",
                                borderColor = Color.Red,
                                textColor = Color.Red,
                                onClick = {
                                    val newVal = ((weightInput.toIntOrNull() ?: 0) + delta).coerceAtLeast(0)
                                    weightInput = newVal.toString()
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .padding(horizontal = 8.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = weightInput,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                    weightInput = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        listOf(10, 50, 100).forEach { delta ->
                            StandaloneStepButton(
                                label = "+$delta",
                                borderColor = primaryColor,
                                textColor = primaryColor,
                                onClick = {
                                    val newVal = ((weightInput.toIntOrNull() ?: 0) + delta).coerceAtMost(9999)
                                    weightInput = newVal.toString()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (showDelete && onDelete != null) {
                            CustomButton(
                                modifier = Modifier.size(48.dp),
                                fillMaxWidth = false,
                                icon = { tint ->
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить", tint = tint)
                                },
                                backgroundColor = Color.White,
                                textColor = Color.Red,
                                cornerRadius = 24.dp,
                                onClick = { onDelete(); onDismiss() }
                            )
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }

                    // Кнопка "0" — центрирована относительно поля ввода (weight 1.5f)
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(48.dp)
                                .border(1.5.dp, Color.LightGray, CircleShape)
                        ) {
                            CustomButton(
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                text = "0",
                                backgroundColor = Color.White,
                                textColor = Color.Black,
                                cornerRadius = 24.dp,
                                onClick = { weightInput = "0" }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        CustomButton(
                            modifier = Modifier
                                .width(72.dp)
                                .height(48.dp),
                            fillMaxWidth = false,
                            text = "✓",
                            backgroundColor = primaryColor,
                            textColor = Color.White,
                            cornerRadius = 24.dp,
                            onClick = {
                                onConfirm(weightInput.toIntOrNull() ?: 0)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StandaloneNutrientColumn(label: String, value: Float, color: Color, bold: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(
            text = if (bold) "%.0f".format(value) else "%.0fг".format(value),
            fontSize = 13.sp,
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun StandaloneStepButton(
    label: String,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        CustomButton(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            text = label,
            backgroundColor = Color.White,
            textColor = textColor,
            cornerRadius = 20.dp,
            onClick = onClick
        )
    }
}
