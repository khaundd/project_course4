package com.example.project_course4.composable_elements

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.project_course4.ProductRepository
import com.example.project_course4.Screen
import com.example.project_course4.SessionManager
import com.example.project_course4.viewmodel.AuthViewModel
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.composable_elements.screens.auth.LoginScreen
import com.example.project_course4.composable_elements.screens.auth.RegistrationScreen
import com.example.project_course4.composable_elements.screens.auth.verification.VerificationScreen
import com.example.project_course4.composable_elements.scanner.BarcodeScannerManager
import com.example.project_course4.composable_elements.screens.recipe.DishCompositionScreen
import com.example.project_course4.composable_elements.screens.MainScreen
import com.example.project_course4.composable_elements.screens.product.ProductCreationScreen
import com.example.project_course4.composable_elements.screens.product.ProductScreen
import com.example.project_course4.composable_elements.screens.ProfileScreen
import com.example.project_course4.composable_elements.screens.recipe.RecipeCreationScreen
import com.example.project_course4.composable_elements.screens.recipe.RecipeScreen
import com.example.project_course4.composable_elements.screens.SelectProductScreen
import com.example.project_course4.composable_elements.screens.auth.PasswordResetScreen
import com.example.project_course4.local_db.AppDatabase
import com.example.project_course4.local_db.DatabaseProvider
import com.example.project_course4.utils.NetworkUtils
import com.example.project_course4.utils.Validation
import com.example.project_course4.viewmodel.ProductViewModel
import com.example.project_course4.viewmodel.ProfileViewModel
import com.example.project_course4.composable_elements.screens.recipe.SharedRecipeScreen
import com.example.project_course4.composable_elements.screens.mealplan.MealPlanScreen
import com.example.project_course4.composable_elements.screens.mealplan.MealPlanEditorScreen
import com.example.project_course4.composable_elements.screens.mealplan.MealPlanDetailScreen
import com.example.project_course4.viewmodel.MealPlanViewModel
import com.example.project_course4.viewmodel.RecipeCreationViewModel
import com.example.project_course4.viewmodel.RecipeViewModel
@Composable
fun NavigationApp(intentState: State<Intent?>) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scannerManager = remember { BarcodeScannerManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val clientAPI = remember { ClientAPI(sessionManager) }
    val validation = remember { Validation() }
    val snackbarHostState = remember { SnackbarHostState() }
    val database = remember {
        DatabaseProvider.getDatabase(context)
    }

    val productRepository = remember {
        ProductRepository(
            context = context,
            productDao = database.productsDao(),
            clientAPI = clientAPI,
            sessionManager = sessionManager,
            mealDao = database.mealDao()
        )
    }

    // фабрика для создания ViewModel
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(ProductViewModel::class.java) -> {
                        val authVm = AuthViewModel(clientAPI, sessionManager, database)
                        ProductViewModel(productRepository, authVm) as T
                    }
                    modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                        ProfileViewModel(sessionManager, clientAPI) as T
                    }
                    modelClass.isAssignableFrom(AuthViewModel::class.java) -> {

                        // Создаем ProductViewModel без зависимостей, чтобы избежать циклической ссылки
                        AuthViewModel(clientAPI, sessionManager, database) as T
                    }
                    modelClass.isAssignableFrom(RecipeViewModel::class.java) -> {
                        RecipeViewModel(clientAPI) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }

    // Сначала создаем AuthViewModel, чтобы получить доступ к dataUpdateEvent
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val productViewModel: ProductViewModel = viewModel(factory = factory)

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scannerManager.startScanning(
                onResult = { barcode ->
                    Log.d("BarcodeScanner", "Scanned barcode: $barcode")
                    productViewModel.searchProductByBarcode(barcode)
                },
                onError = { e ->
                    Log.e("BarcodeScanner", "Ошибка: $e")
                    val message = e.message ?: e.javaClass.simpleName
                    validation.toastMessage = message
                }
            )
        } else {
            Toast.makeText(context, "Разрешение на использование камеры не выдано", Toast.LENGTH_LONG).show()
        }
    }

    // Отдельный launcher для ProductCreationScreen — только вставляет штрих-код, без поиска в БД
    val productCreationCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scannerManager.startScanning(
                onResult = { barcode ->
                    Log.d("BarcodeScanner", "ProductCreation: scanned barcode: $barcode")
                    val currentState = productViewModel.productCreationState.value
                    productViewModel.updateProductCreationState(currentState.copy(barcode = barcode))
                },
                onError = { e ->
                    Log.e("BarcodeScanner", "Ошибка: $e")
                    validation.toastMessage = e.message ?: e.javaClass.simpleName
                }
            )
        } else {
            Toast.makeText(context, "Разрешение на использование камеры не выдано", Toast.LENGTH_LONG).show()
        }
    }
    val recipeCreationViewModel = remember { RecipeCreationViewModel(clientAPI) }
    val recipeCreationState by recipeCreationViewModel.state.collectAsState()
    val recipeViewModel: RecipeViewModel = viewModel(factory = factory)
    val mealPlanViewModel = remember { MealPlanViewModel(clientAPI) }
    val allProducts by productViewModel.products.collectAsState()
    val profileViewModel: ProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                        ProfileViewModel(sessionManager, clientAPI, authViewModel.dataUpdateEvent) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    )

    // Отслеживание toast сообщений
    LaunchedEffect(validation.toastMessage) {
        validation.toastMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            validation.clearToastMessage()
        }
    }

    // функция обработки сканирования
    val handleScanning = { _: NavController ->

        // Проверяем интернет-соединение перед вызовом сканера
        if (NetworkUtils.isInternetAvailable(context)) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                scannerManager.startScanning(
                    onResult = { barcode ->
                        Log.d("BarcodeScanner", "Scanned barcode: $barcode")
                        productViewModel.searchProductByBarcode(barcode)
                    },
                    onError = { e ->
                        Log.e("BarcodeScanner", "Ошибка: $e")
                        val message = e.message ?: e.javaClass.simpleName
                        validation.toastMessage = message
                    }
                )
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            validation.toastMessage = "Невозможно вызвать сканер штрих-кодов. Отсутствует интернет-соединение"
        }
    }

    // куда направить пользователя
    // если токен есть, то отправляем на главную, иначе на логин
    val startDestination = if (sessionManager.fetchAuthToken() != null) {
        Screen.Main.route
    } else {
        Screen.Login.route
    }

    // Обработка deep link при старте и при onNewIntent
    val currentIntent by intentState
    LaunchedEffect(currentIntent) {
        val data = currentIntent?.data ?: return@LaunchedEffect
        val path = data.path ?: return@LaunchedEffect
        if (path.startsWith("/recipes/shared/")) {
            val token = path.removePrefix("/recipes/shared/")
            if (token.isNotBlank()) {
                navController.navigate("sharedRecipe/$token")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {

            composable(Screen.Main.route) { _ ->
                MainScreen(
                    navController = navController,
                    viewModel = productViewModel,
                    profileViewModel = profileViewModel,
                )
            }

            composable(Screen.Profile.route) { _ ->
                ProfileScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    profileViewModel = profileViewModel
                )
            }

            composable("selectProductWithMeal/{mealId}") { backStackEntry ->
                val mealId = backStackEntry.arguments?.getString("mealId")
                Log.d("NavigationApp", "Создан SelectProductScreen с mealId: $mealId")
                SelectProductScreen(
                    navController = navController,
                    viewModel = productViewModel,
                    mealId = mealId,
                    onBarcodeScan = {
                        handleScanning(navController)
                    }
                )
            }

            composable(Screen.SelectProduct.route) { _ ->
                Log.d("NavigationApp", "Создан SelectProductScreen")
                SelectProductScreen(
                    navController = navController,
                    viewModel = productViewModel
                )
            }

            composable(
                route = "productCreation?barcode={barcode}",
                arguments = listOf(
                    navArgument("barcode") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = false
                    }
                )
            ) { backStackEntry ->
                val barcodeArg = backStackEntry.arguments?.getString("barcode").orEmpty()
                Log.d("NavigationApp", "Получен barcodeArg из навигации: '$barcodeArg'")
                Log.d("NavigationApp", "URL декодирован: '${java.net.URLDecoder.decode(barcodeArg, Charsets.UTF_8.name())}'")
                ProductCreationScreen(
                    navController = navController,
                    viewModel = productViewModel,
                    onBarcodeScan = { _ ->
                        // Для ProductCreationScreen сканер только вставляет штрих-код, без поиска в БД
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            scannerManager.startScanning(
                                onResult = { barcode ->
                                    Log.d(
                                        "BarcodeScanner",
                                        "ProductCreation: scanned barcode: $barcode"
                                    )
                                    val currentState = productViewModel.productCreationState.value
                                    productViewModel.updateProductCreationState(
                                        currentState.copy(
                                            barcode = barcode
                                        )
                                    )
                                },
                                onError = { e ->
                                    Log.e("BarcodeScanner", "Ошибка: $e")
                                    validation.toastMessage = e.message ?: e.javaClass.simpleName
                                }
                            )
                        } else {
                            productCreationCameraLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    initialBarcode = barcodeArg.takeIf { it.isNotBlank() }
                )
            }

            // альтернативный маршрут без параметра
            composable(Screen.ProductCreation.route) { _ ->
                ProductCreationScreen(
                    navController = navController,
                    viewModel = productViewModel,
                    onBarcodeScan = { _ ->
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            scannerManager.startScanning(
                                onResult = { barcode ->
                                    Log.d(
                                        "BarcodeScanner",
                                        "ProductCreation: scanned barcode: $barcode"
                                    )
                                    val currentState = productViewModel.productCreationState.value
                                    productViewModel.updateProductCreationState(
                                        currentState.copy(
                                            barcode = barcode
                                        )
                                    )
                                },
                                onError = { e ->
                                    Log.e("BarcodeScanner", "Ошибка: $e")
                                    validation.toastMessage = e.message ?: e.javaClass.simpleName
                                }
                            )
                        } else {
                            productCreationCameraLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    initialBarcode = null
                )
            }

            composable(Screen.Login.route) { _ ->
                LoginScreen(
                    navController = navController,
                    viewModel = authViewModel
                )
            }

            composable(Screen.PasswordReset.route) { _ ->
                PasswordResetScreen(
                    navController = navController,
                    viewModel = authViewModel
                )
            }

            composable(Screen.Registration.route) { _ ->
                RegistrationScreen(
                    navController = navController,
                    viewModel = authViewModel
                )
            }

            composable(
                route = "${Screen.Verification.route}?email={email}",
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                VerificationScreen(navController = navController, email = email, viewModel = authViewModel)
            }

            composable(Screen.Products.route) { _ ->
                ProductScreen(navController = navController)
            }

            composable(Screen.Recipes.route) { _ ->
                RecipeScreen(navController = navController, viewModel = recipeViewModel)
            }

            composable(Screen.RecipeCreation.route) { _ ->
                RecipeCreationScreen(
                    navController = navController,
                    viewModel = recipeCreationViewModel,
                    onSaveSuccess = {
                        recipeViewModel.refreshRecipes()
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.SelectProductForRecipe.route) { _ ->
                SelectProductScreen(
                    navController = navController,
                    viewModel = productViewModel,
                    mealId = null,
                    onBarcodeScan = { handleScanning(navController) },
                    onConfirmForRecipe = { selectedProducts ->
                        recipeCreationViewModel.addSelectedProducts(selectedProducts)
                        navController.popBackStack()
                    },
                    existingIngredientIds = recipeCreationState.ingredients
                        .map { it.product.productId }.toSet()
                )
            }

            composable(
                route = Screen.DishComposition.route,
                arguments = listOf(navArgument("dishName") { type = NavType.StringType })
            ) { backStackEntry ->
                val dishName = backStackEntry.arguments?.getString("dishName") ?: ""
                DishCompositionScreen(
                    navController = navController,
                    dishName = dishName,
                    recipeViewModel = recipeViewModel,
                    recipeCreationViewModel = recipeCreationViewModel
                )
            }

            composable(
                route = Screen.RecipeEdit.route,
                arguments = listOf(navArgument("dishName") { type = NavType.StringType })
            ) { _ ->
                RecipeCreationScreen(
                    navController = navController,
                    viewModel = recipeCreationViewModel,
                    onSaveSuccess = {
                        recipeViewModel.refreshRecipes()
                        navController.popBackStack(Screen.Recipes.route, false)
                    }
                )
            }

            composable(
                route = Screen.SharedRecipe.route,
                arguments = listOf(navArgument("token") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "https://loftily-adequate-urchin.cloudpub.ru/recipes/shared/{token}"
                    }
                )
            ) { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: ""
                SharedRecipeScreen(
                    navController = navController,
                    token = token,
                    viewModel = recipeViewModel
                )
            }

            composable(Screen.MealPlans.route) {
                MealPlanScreen(
                    navController = navController,
                    viewModel = mealPlanViewModel
                )
            }

            composable(Screen.MealPlanEditor.route) {
                MealPlanEditorScreen(
                    navController = navController,
                    viewModel = mealPlanViewModel,
                    allProducts = allProducts
                )
            }

            composable(
                route = "selectProductForMealPlan/{dayIndex}/{mealIndex}",
                arguments = listOf(
                    navArgument("dayIndex") { type = NavType.IntType },
                    navArgument("mealIndex") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val dayIndex = backStackEntry.arguments?.getInt("dayIndex") ?: 0
                val mealIndex = backStackEntry.arguments?.getInt("mealIndex") ?: 0
                SelectProductScreen(
                    navController = navController,
                    viewModel = productViewModel,
                    mealId = null,
                    onBarcodeScan = { handleScanning(navController) },
                    onConfirmForRecipe = { selectedProducts ->
                        mealPlanViewModel.setPendingMealTarget(dayIndex, mealIndex)
                        mealPlanViewModel.setPendingProductsForWeight(selectedProducts)
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "mealPlanDetail/{planId}",
                arguments = listOf(navArgument("planId") { type = NavType.IntType })
            ) { backStackEntry ->
                val planId = backStackEntry.arguments?.getInt("planId") ?: return@composable
                val plan = (mealPlanViewModel.plans.value + mealPlanViewModel.publicPlans.value)
                    .find { it.planId == planId } ?: return@composable
                MealPlanDetailScreen(
                    navController = navController,
                    plan = plan,
                    allProducts = allProducts,
                    productViewModel = productViewModel
                )
            }
        }
    }
}