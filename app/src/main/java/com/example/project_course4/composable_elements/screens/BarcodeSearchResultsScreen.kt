package com.example.project_course4.composable_elements.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_course4.Product
import com.example.project_course4.composable_elements.CustomButton
import com.example.project_course4.composable_elements.ProductElement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeSearchResultsScreen(
    databaseProducts: List<Product>,
    userProducts: List<Product>,
    onDismiss: () -> Unit,
    onProductAccept: (Product) -> Unit,
    onAddOwn: () -> Unit,
    modifier: Modifier = Modifier,
    currentUserId: Int = -1
) {
    var sheetOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    val sheetHeight = with(LocalDensity.current) { 600.dp.toPx() }
    val minOffset = 0f
    val maxOffset = sheetHeight * 0.8f // Allow collapsing to 20% of height
    
    val animatedOffset by animateFloatAsState(
        targetValue = if (isDragging) sheetOffset else minOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "sheetOffset"
    )
    
    // Check if sheet should be dismissed
    LaunchedEffect(animatedOffset) {
        if (animatedOffset >= maxOffset * 0.9f) {
            onDismiss()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Dimmed background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() }
        )
        
        // Bottom sheet
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .align(Alignment.BottomCenter)
                .offset(y = with(LocalDensity.current) { animatedOffset.toDp() })
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { },
                        onDragEnd = { 
                            // Snap to either fully expanded or collapsed based on current position
                            sheetOffset = if (sheetOffset > maxOffset / 2) {
                                maxOffset
                            } else {
                                minOffset
                            }
                        }
                    ) { _, dragAmount ->
                        val newOffset = sheetOffset + dragAmount
                        sheetOffset = newOffset.coerceIn(minOffset, maxOffset)
                    }
                }
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.Gray
                        )
                    }
                    
                    Text(
                        text = "Результат поиска",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.width(40.dp))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Products list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Database products section
                    item {
                        Column {
                            Text(
                                text = "Продукты из базы данных",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            if (databaseProducts.isEmpty()) {
                                Text(
                                    text = "Ничего не найдено",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                databaseProducts.forEach { product ->
                                    ProductWithSelection(
                                        product = product,
                                        onAccept = { onProductAccept(product) },
                                        currentUserId = currentUserId
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // User products section
                    item {
                        Column {
                            Text(
                                text = "Продукты пользователей",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            if (userProducts.isEmpty()) {
                                Text(
                                    text = "Ничего не найдено",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                CustomButton(
                                    text = "Добавить свой",
                                    backgroundColor = Color(0xFF2196F3),
                                    textColor = Color.White,
                                    onClick = onAddOwn
                                )
                            } else {
                                userProducts.forEachIndexed { index, product ->
                                    ProductWithSelection(
                                        product = product,
                                        onAccept = { onProductAccept(product) },
                                        currentUserId = currentUserId
                                    )
                                    
                                    // Add "Добавить свой" button after the last user product
                                    if (index == userProducts.size - 1) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        CustomButton(
                                            text = "Добавить свой",
                                            backgroundColor = Color(0xFF2196F3),
                                            textColor = Color.White,
                                            onClick = onAddOwn
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductWithSelection(
    product: Product,
    onAccept: () -> Unit,
    currentUserId: Int = -1
) {
    var isSelected by remember { mutableStateOf(false) }
    remember(product) {
        "${product.productId}_${product.name}_${product.barcode}" 
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product element with its own selection state
        ProductElement(
            product = product,
            isSelected = isSelected,
            onSelect = { isSelected = !isSelected },
            currentUserId = currentUserId
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Check button
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            IconButton(
                onClick = onAccept,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Принять",
                    tint = Color.White
                )
            }
        }
    }
}
