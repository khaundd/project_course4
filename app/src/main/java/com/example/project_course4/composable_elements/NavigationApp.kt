package com.example.project_course4.composable_elements

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.project_course4.ProductRepository
import com.example.project_course4.Screen
import com.example.project_course4.SessionManager
import com.example.project_course4.AuthViewModel
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.composable_elements.auth.LoginScreen
import com.example.project_course4.composable_elements.auth.RegistrationScreen
import com.example.project_course4.composable_elements.auth.verification.VerificationScreen
import com.example.project_course4.composable_elements.scanner.BarcodeScannerManager
import com.example.project_course4.local_db.AppDatabase
import com.example.project_course4.viewmodel.ProductViewModel

@Composable
fun NavigationApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scannerManager = remember { BarcodeScannerManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val clientAPI = remember { ClientAPI(sessionManager) }

    val database = remember {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "my_database"
        ).build()
    }

    val productRepository = remember {
        ProductRepository(
            productDao = database.productsDao(),
            clientAPI = clientAPI,
            sessionManager = sessionManager,
            mealDao = database.mealDao()
        )
    }

    // функция обработки сканирования
    val handleScanning = { navController: NavController ->
        scannerManager.startScanning(
            onResult = { barcode ->
                Log.d("BarcodeScanner", "Scanned barcode: $barcode")
                navController.navigate("productCreation?barcode=${java.net.URLEncoder.encode(barcode, Charsets.UTF_8.name())}")
            },
            onError = { e ->
                Log.e("BarcodeScanner", "Ошибка: $e")
                val message = e.message ?: e.javaClass.simpleName
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        )
    }

    // куда направить пользователя
    // если токен есть, то отправляем на главную, иначе на логин
    val startDestination = if (sessionManager.fetchAuthToken() != null) {
        Screen.Main.route
    } else {
        Screen.Login.route
    }

    // фабрика для создания ViewModel
    val factory = remember {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(ProductViewModel::class.java) -> {
                        val authVm = AuthViewModel(clientAPI, sessionManager, database)
                        ProductViewModel(productRepository, authVm) as T
                    }
                    modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                        AuthViewModel(clientAPI, sessionManager, database) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }

    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val productViewModel: ProductViewModel = viewModel(factory = factory)

    NavHost(
        navController = navController,
        startDestination = startDestination // используем вычисленный путь
    ) {
        composable(Screen.Main.route) { backStackEntry ->
            MainScreen(
                navController = navController,
                viewModel = productViewModel,
                onLogout = {
                    authViewModel.logoutAndNavigate(navController)
                }
            )
        }

        composable(Screen.Profile.route) { backStackEntry ->
            ProfileScreen(
                navController = navController,
                onLogout = { authViewModel.logoutAndNavigate(navController) }
            )
        }
        
        composable("selectProductWithMeal/{mealId}") { backStackEntry ->
            val mealId = backStackEntry.arguments?.getString("mealId")
            SelectProductScreen(
                navController = navController,
                viewModel = productViewModel,
                mealId = mealId,
                onBarcodeScan = {
                    handleScanning(navController)
                }
            )
        }
        composable(Screen.SelectProduct.route) {backStackEntry ->
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
                onBarcodeScan = {
                    handleScanning(navController)
                },
                initialBarcode = barcodeArg.takeIf { it.isNotBlank() }
            )
        }
        // альтернативный маршрут без параметра
        composable(Screen.ProductCreation.route) { backStackEntry ->
            ProductCreationScreen(
                navController = navController,
                viewModel = productViewModel,
                onBarcodeScan = {
                    handleScanning(navController)
                },
                initialBarcode = null
            )
        }
        composable(Screen.Login.route) { backStackEntry ->
            LoginScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }
        composable(Screen.Registration.route) { backStackEntry ->
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
    }
}