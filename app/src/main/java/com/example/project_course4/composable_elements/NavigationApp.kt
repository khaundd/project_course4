package com.example.project_course4.composable_elements

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.project_course4.Screen
import com.example.project_course4.composable_elements.auth.LoginScreen
import com.example.project_course4.composable_elements.auth.RegistrationScreen
import com.example.project_course4.composable_elements.auth.verification.VerificationScreen
import com.example.project_course4.composable_elements.scanner.BarcodeScannerManager
import com.example.project_course4.viewmodel.ProductViewModel

@Composable
fun NavigationApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val viewModel: ProductViewModel = viewModel()
    val scannerManager = remember { BarcodeScannerManager(context) }     // Инициализируем менеджер сканера

    // Общая функция обработки сканирования
    val handleScanning = {
        scannerManager.startScanning(
            onResult = { barcode ->
                Log.d("BarcodeScanner", "Scanned barcode: $barcode")
            },
            onError = { TODO("щас пока не нужна эта обработка ошибок") }
        )
    }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Registration.route
    ) {
        composable(Screen.Main.route) { backStackEntry ->
            MainScreen(
                navController = navController,
                viewModel = viewModel,
                onBarcodeScan = {
                    handleScanning()
                }
            )
        }
        
        composable("selectProductWithMeal/{mealId}") { backStackEntry ->
            val mealId = backStackEntry.arguments?.getString("mealId")
            SelectProductScreen(
                navController = navController,
                viewModel = viewModel,
                mealId = mealId,
                onBarcodeScan = {
                    handleScanning()
                }
            )
        }
        composable(Screen.SelectProduct.route) {backStackEntry ->
            SelectProductScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(Screen.ProductCreation.route) { backStackEntry ->
            ProductCreationScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        composable(Screen.Login.route) { backStackEntry ->
            LoginScreen(
                navController = navController
            )
        }
        composable(Screen.Registration.route) { backStackEntry ->
            RegistrationScreen(
                navController = navController
            )
        }
        composable(
            route = "${Screen.Verification.route}?email={email}",
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            VerificationScreen(navController = navController, email = email)
        }
    }
}