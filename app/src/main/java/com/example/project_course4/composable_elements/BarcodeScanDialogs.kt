package com.example.project_course4.composable_elements

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_course4.Product

@Composable
fun ProductNotFoundDialog(
    barcode: String,
    onDismiss: () -> Unit,
    onCreateProduct: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Продукт не найден",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Мы не нашли совпадений в нашей базе данных по штрих-коду $barcode. Хотите добавить свой продукт?",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreateProduct()
                    onDismiss()
                }
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отменить")
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun ProductFoundDialog(
    barcode: String,
    product: Product,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onEdit: () -> Unit,
    onAddOwn: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Найдено совпадение",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Найдено совпадение по штрих-коду $barcode. Если все значения совпадают, то нажмите \"Принять\". Если вы хотите что-то уточнить, нажмите \"Добавить свой\".",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Карточка с информацией о продукте
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = product.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Белки", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text("${product.protein}г", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Column {
                                Text("Жиры", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text("${product.fats}г", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Column {
                                Text("Углеводы", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text("${product.carbs}г", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Column {
                                Text("Калории", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text("${product.calories}кКал", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("Принять")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onEdit) {
                    Text("Изменить")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onAddOwn) {
                    Text("Добавить свой")
                }
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun BarcodeScanLoadingDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Поиск продукта",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Идет поиск продукта по штрих-коду...",
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
        modifier = Modifier.padding(16.dp)
    )
}
