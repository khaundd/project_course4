package com.example.project_course4.composable_elements

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.project_course4.ProductViewModel
import com.example.project_course4.Screen
import com.example.project_course4.composable_elements.auth.LoginScreen
import com.example.project_course4.composable_elements.auth.RegistrationScreen

@Composable
fun NavigationApp() {
    val navController = rememberNavController()
    val viewModel: ProductViewModel = viewModel()
    NavHost(
        navController = navController,
        startDestination = Screen.Registration.route
    ) {
        composable(Screen.Main.route) { backStackEntry ->
            MainScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        composable(Screen.SelectProduct.route) {backStackEntry ->
            SelectProductScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        composable("selectProductWithMeal/{mealId}") { backStackEntry ->
            val mealId = backStackEntry.arguments?.getString("mealId")
            SelectProductScreen(
                navController = navController,
                viewModel = viewModel,
                mealId = mealId
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
    }
}