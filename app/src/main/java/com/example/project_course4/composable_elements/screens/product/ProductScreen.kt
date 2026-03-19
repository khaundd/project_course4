package com.example.project_course4.composable_elements.screens.product

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.composable_elements.BottomNavigationBar
import com.example.project_course4.composable_elements.SafeCloseTopAppBar

@Composable
fun ProductScreen(navController: NavController) {
    Scaffold(
        topBar = {
            SafeCloseTopAppBar(title = "Продукты", navController = navController)
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentScreen = "products")
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "В дальнейшем тут будут только продукты",
                fontSize = 18.sp,
                color = Color.Gray
            )
        }
    }
}
