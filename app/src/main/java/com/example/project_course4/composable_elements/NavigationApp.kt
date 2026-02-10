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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.project_course4.Screen
import com.example.project_course4.SessionManager
import com.example.project_course4.ViewModel
import com.example.project_course4.api.ClientAPI
import com.example.project_course4.composable_elements.auth.LoginScreen
import com.example.project_course4.composable_elements.auth.RegistrationScreen
import com.example.project_course4.composable_elements.auth.verification.VerificationScreen
import com.example.project_course4.composable_elements.scanner.BarcodeScannerManager
import com.example.project_course4.viewmodel.ProductViewModel

@Composable
fun NavigationApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scannerManager = remember { BarcodeScannerManager(context) }     // Инициализируем менеджер сканера
    val sessionManager = remember { SessionManager(context) }
    val clientAPI = remember { ClientAPI(sessionManager) }

    // Общая функция обработки сканирования
    val handleScanning = {
        scannerManager.startScanning(
            onResult = { barcode ->
                Log.d("BarcodeScanner", "Scanned barcode: $barcode")
            },
            onError = { TODO("щас пока не нужна эта обработка ошибок") }
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
                    // Для ProductViewModel
                    modelClass.isAssignableFrom(ProductViewModel::class.java) ->
                        ProductViewModel(clientAPI, sessionManager) as T

                    // Для ViewModel (логин/регистрация)
                    modelClass.isAssignableFrom(ViewModel::class.java) ->
                        ViewModel(clientAPI, sessionManager) as T

                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }

    // экземпляры ViewModel
    val productViewModel: ProductViewModel = viewModel(factory = factory)
    val authViewModel: ViewModel = viewModel(factory = factory)
    
    NavHost(
        navController = navController,
        startDestination = startDestination // Используем вычисленный путь
    ) {
        composable(Screen.Main.route) { backStackEntry ->
            MainScreen(
                navController = navController,
                viewModel = productViewModel,
                onBarcodeScan = {
                    handleScanning()
                }
            )
        }
        
        composable("selectProductWithMeal/{mealId}") { backStackEntry ->
            val mealId = backStackEntry.arguments?.getString("mealId")
            SelectProductScreen(
                navController = navController,
                viewModel = productViewModel,
                mealId = mealId,
                onBarcodeScan = {
                    handleScanning()
                }
            )
        }
        composable(Screen.SelectProduct.route) {backStackEntry ->
            SelectProductScreen(
                navController = navController,
                viewModel = productViewModel
            )
        }

        composable(Screen.ProductCreation.route) { backStackEntry ->
            ProductCreationScreen(
                navController = navController,
                viewModel = productViewModel
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