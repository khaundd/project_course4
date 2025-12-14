package com.example.project_course4.composable_elements

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.project_course4.Product
import com.example.project_course4.ProductViewModel
import com.example.project_course4.Screen

@Composable
fun NavigationApp() {
    val navController = rememberNavController()
    val viewModel: ProductViewModel = viewModel()
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
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
    }
}