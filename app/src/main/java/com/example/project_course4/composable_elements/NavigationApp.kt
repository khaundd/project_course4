package com.example.project_course4.composable_elements

import androidx.compose.runtime.Composable
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
import com.example.project_course4.viewmodel.ProductViewModel

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
        composable(
            route = "${Screen.Verification.route}?email={email}",
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            VerificationScreen(navController = navController, email = email)
        }
    }
}