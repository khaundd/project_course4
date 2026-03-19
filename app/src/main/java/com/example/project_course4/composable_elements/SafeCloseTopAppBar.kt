package com.example.project_course4.composable_elements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.project_course4.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeCloseTopAppBar(
    title: String,
    navController: NavController,
    actions: @Composable () -> Unit = {}
) {
    var isNavigatingBack by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (!isNavigatingBack) {
                        isNavigatingBack = true
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
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color.Black,
        )
    )
}
