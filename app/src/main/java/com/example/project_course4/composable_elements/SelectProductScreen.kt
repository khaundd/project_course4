package com.example.project_course4.composable_elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.example.project_course4.R
import com.example.project_course4.viewmodel.ProductViewModel
import com.example.project_course4.ui.theme.ProteinColor
import com.example.project_course4.ui.theme.FatColor
import com.example.project_course4.ui.theme.CarbColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectProductScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    mealId: String? = null,
    onBarcodeScan: (String) -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentSelection by viewModel.currentSelection.collectAsState()
    val shouldShowProductCreation by viewModel.shouldShowProductCreation.collectAsState()

    var isFabMenuExpanded by remember { mutableStateOf(false) }
    
    // Анимация поворота иконки
    val rotationAngle by animateFloatAsState(
        targetValue = if (isFabMenuExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "fab_rotation"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Продукты")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearCurrentSelection()
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                )
            )
        },
        floatingActionButton = {
            if (currentSelection.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        if (mealId != null) {
                            viewModel.addSelectionToMealWithWeightInput()
                        } else {
                            viewModel.saveCurrentSelection()
                        }
                        navController.popBackStack()
                    },
                    containerColor = Color.Green,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, "Готово")
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить (${currentSelection.size})")
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Легенда БЖУ как продолжение верхней панели
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD9D7D7)) // Сероватый фон
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Белки",
                        color = ProteinColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Жиры",
                        color = FatColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "Углеводы",
                        color = CarbColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Содержимое
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(products) { product ->
                        ProductElement(
                            product = product,
                            isSelected = currentSelection.contains(product)
                        ) {
                            viewModel.toggleCurrentSelection(product)
                        }
                    }
                }
            }
        }

        // FAB меню внизу справа
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp) // Отступ между кнопками
            ) {
                // Кнопки меню (выезжают снизу вверх)
                AnimatedVisibility(
                    visible = isFabMenuExpanded,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 300)
                    ) + slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(durationMillis = 300)
                    ) + scaleIn(
                        animationSpec = tween(durationMillis = 300),
                        initialScale = 0.8f
                    ),
                    exit = fadeOut(
                        animationSpec = tween(durationMillis = 200)
                    ) + slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(durationMillis = 200)
                    ) + scaleOut(
                        animationSpec = tween(durationMillis = 200),
                        targetScale = 0.8f
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp) // Отступ между доп. кнопками
                    ) {
                        // Кнопка "Сканировать"
                        FloatingActionButton(
                            onClick = {
                                isFabMenuExpanded = false
                                onBarcodeScan("OPEN_SCANNER")
                            },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(48.dp),
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 2.dp,
                                hoveredElevation = 1.dp
                            )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.barcode_scanner_24px),
                                contentDescription = "Сканировать штрихкод",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Кнопка "Новый"
                        FloatingActionButton(
                            onClick = {
                                isFabMenuExpanded = false
                                viewModel.navigateToProductCreation(navController)
                            },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(48.dp),
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 2.dp,
                                hoveredElevation = 1.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Новый продукт",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Основная FAB кнопка (всегда внизу колонки)
                FloatingActionButton(
                    onClick = { isFabMenuExpanded = !isFabMenuExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Меню",
                        tint = Color.White,
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }
        }
    }
}