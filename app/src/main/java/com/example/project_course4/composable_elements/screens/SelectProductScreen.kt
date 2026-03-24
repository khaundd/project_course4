package com.example.project_course4.composable_elements.screens

import android.util.Log
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.Product
import com.example.project_course4.R
import com.example.project_course4.Screen
import com.example.project_course4.composable_elements.BottomNavigationBar
import com.example.project_course4.composable_elements.ProductElement
import com.example.project_course4.composable_elements.dialogs.ProductNotFoundDialog
import com.example.project_course4.ui.theme.CarbColor
import com.example.project_course4.ui.theme.FatColor
import com.example.project_course4.ui.theme.ProteinColor
import com.example.project_course4.viewmodel.ProductViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SelectProductScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    mealId: String? = null,
    onBarcodeScan: (String) -> Unit = {},
    onConfirmForRecipe: ((List<Product>) -> Unit)? = null,
    existingIngredientIds: Set<Int> = emptySet()
) {
    val displayedProducts by viewModel.displayedProducts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMoreProducts by viewModel.hasMoreProducts.collectAsState()
    val noInternetError by viewModel.noInternetError.collectAsState()
    val currentSelection by viewModel.currentSelection.collectAsState()
    val showProductNotFoundDialog by viewModel.showProductNotFoundDialog.collectAsState()
    val showBarcodeSearchResults by viewModel.showBarcodeSearchResults.collectAsState()
    val databaseSearchResults by viewModel.databaseSearchResults.collectAsState()
    val userSearchResults by viewModel.userSearchResults.collectAsState()
    val scannedBarcode by viewModel.scannedBarcode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchNoInternet by viewModel.searchNoInternet.collectAsState()

    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var isNavigatingBack by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isFabMenuExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "fab_rotation"
    )
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Сбрасываем выборку при входе на экран
    LaunchedEffect(Unit) {
        viewModel.clearCurrentSelection()
        viewModel.clearSearch()
        viewModel.refreshRecentProducts()
        if (viewModel.displayedProducts.value.isEmpty()) {
            viewModel.loadInitialProductsPage()
        }
    }

    // Дебаунс поиска
    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(400)
            .distinctUntilChanged()
            .collect { query ->
                viewModel.executeSearch(query)
            }
    }

    // Подгрузка следующей страницы при достижении конца списка
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3 && total > 0
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && searchQuery.isBlank()) {
            viewModel.loadNextProductsPage()
        }
    }

    val isInSearchMode = searchQuery.isNotBlank()
    val productsToShow = if (isInSearchMode) searchResults else displayedProducts

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Продукты") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isNavigatingBack) {
                                Log.d("SelectProductScreen", "Нажата кнопка закрытия, вызываем popBackStack")
                                isNavigatingBack = true
                                viewModel.clearCurrentSelection()
                                viewModel.clearSearch()
                                val navigated = navController.navigateUp()
                                if (!navigated) {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        },
                        enabled = !isNavigatingBack
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
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
                        if (onConfirmForRecipe != null) {
                            onConfirmForRecipe(currentSelection.toList())
                            viewModel.clearCurrentSelection()
                        } else if (mealId != null) {
                            viewModel.addSelectionToMealWithWeightInput()
                            navController.popBackStack()
                        } else {
                            viewModel.saveCurrentSelection()
                            navController.popBackStack()
                        }
                    },
                    containerColor = Color(0xFF4CAF50),
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
        bottomBar = {
            Column {
                // Строка поиска над навигационной панелью
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchProducts(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    placeholder = { Text("Поиск продуктов...", fontSize = 14.sp) },
                    leadingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
                BottomNavigationBar(navController = navController, currentScreen = "search")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Легенда БЖУ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Белки", color = ProteinColor, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(20.dp))
                    Text("Жиры", color = FatColor, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(20.dp))
                    Text("Углеводы", color = CarbColor, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Уведомление об отсутствии интернета при поиске
            if (searchNoInternet) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3CD))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Отсутствует интернет соединение",
                        color = Color(0xFF856404),
                        fontSize = 13.sp
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(productsToShow) { product ->
                        val alreadyAdded = existingIngredientIds.contains(product.productId)
                        ProductElement(
                            product = product,
                            isSelected = if (alreadyAdded) false else currentSelection.contains(product)
                        ) {
                            if (!alreadyAdded) {
                                viewModel.toggleCurrentSelection(product)
                            }
                        }
                    }

                    // Индикатор подгрузки / конец списка / нет интернета
                    if (!isInSearchMode) {
                        if (isLoadingMore) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Загрузка продуктов", fontSize = 13.sp, color = Color.Gray)
                                }
                            }
                        } else if (noInternetError && displayedProducts.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Отсутствует интернет соединение",
                                        fontSize = 13.sp,
                                        color = Color(0xFF856404)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB меню (сканер + создать продукт)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = isFabMenuExpanded,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                        initialOffsetY = { it }, animationSpec = tween(300)
                    ) + scaleIn(animationSpec = tween(300), initialScale = 0.8f),
                    exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                        targetOffsetY = { it }, animationSpec = tween(200)
                    ) + scaleOut(animationSpec = tween(200), targetScale = 0.8f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                isFabMenuExpanded = false
                                onBarcodeScan("OPEN_SCANNER")
                            },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(48.dp),
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 2.dp, 1.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.barcode_scanner_24px),
                                contentDescription = "Сканировать штрихкод",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        FloatingActionButton(
                            onClick = {
                                isFabMenuExpanded = false
                                viewModel.navigateToProductCreation(navController)
                            },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(48.dp),
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 2.dp, 1.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Новый продукт", tint = Color.White)
                        }
                    }
                }
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

    if (showProductNotFoundDialog) {
        ProductNotFoundDialog(
            barcode = scannedBarcode,
            onDismiss = { viewModel.hideAllBarcodeDialogs() },
            onCreateProduct = {
                viewModel.createProductFromBarcode()
                navController.navigate("productCreation?barcode=$scannedBarcode")
            }
        )
    }
    if (showBarcodeSearchResults) {
        BarcodeSearchResultsScreen(
            databaseProducts = databaseSearchResults,
            userProducts = userSearchResults,
            onDismiss = { viewModel.hideAllBarcodeDialogs() },
            onProductAccept = { product -> viewModel.acceptProductFromSearchResults(product) },
            onAddOwn = {
                viewModel.addOwnProductFromSearchResults()
                navController.navigate("productCreation?barcode=$scannedBarcode")
            }
        )
    }
}
