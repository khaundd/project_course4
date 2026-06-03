package com.example.project_course4.composable_elements

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.project_course4.Screen
import kotlinx.coroutines.launch

@Composable
fun AppDrawerContent(
    navController: NavController,
    currentRoute: String?,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val nutritionExpanded = expandedSection == "nutrition"
    val activityExpanded = expandedSection == "activity"

    ModalDrawerSheet {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Меню", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)

        NavigationDrawerItem(
            icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            label = { Text("Профиль") },
            selected = currentRoute == Screen.Profile.route,
            onClick = {
                scope.launch {
                    onClose()
                    navController.navigate(Screen.Profile.route)
                }
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        // Питание
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
            label = { Text("Питание") },
            selected = false,
            badge = {
                Icon(if (nutritionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            },
            onClick = { expandedSection = if (nutritionExpanded) null else "nutrition" },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        if (nutritionExpanded) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                label = { Text("Дневник питания") },
                selected = currentRoute == Screen.Main.route,
                onClick = {
                    scope.launch {
                        onClose()
                        navController.navigate(Screen.Main.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                },
                modifier = Modifier.padding(start = 28.dp, end = 8.dp)
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                label = { Text("Создать продукт") },
                selected = currentRoute?.startsWith(Screen.ProductCreation.route) == true,
                onClick = { scope.launch { onClose(); navController.navigate("productCreation?barcode=") } },
                modifier = Modifier.padding(start = 28.dp, end = 8.dp)
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                label = { Text("Рецепты") },
                selected = currentRoute == Screen.Recipes.route,
                onClick = { scope.launch { onClose(); navController.navigate(Screen.Recipes.route) } },
                modifier = Modifier.padding(start = 28.dp, end = 8.dp)
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
                label = { Text("Планы питания") },
                selected = currentRoute == Screen.MealPlans.route,
                onClick = { scope.launch { onClose(); navController.navigate(Screen.MealPlans.route) } },
                modifier = Modifier.padding(start = 28.dp, end = 8.dp)
            )
        }

        // Активность
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
            label = { Text("Активность") },
            selected = false,
            badge = {
                Icon(if (activityExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            },
            onClick = { expandedSection = if (activityExpanded) null else "activity" },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        if (activityExpanded) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                label = { Text("Каталог упражнений") },
                selected = currentRoute == Screen.ExerciseCatalog.route,
                onClick = { scope.launch { onClose(); navController.navigate(Screen.ExerciseCatalog.route) } },
                modifier = Modifier.padding(start = 28.dp, end = 8.dp)
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                label = { Text("Дневник тренировок") },
                selected = currentRoute == Screen.TrainingLog.route,
                onClick = { scope.launch { onClose(); navController.navigate(Screen.TrainingLog.route) } },
                modifier = Modifier.padding(start = 28.dp, end = 8.dp)
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
                label = { Text("Планы тренировок") },
                selected = currentRoute == Screen.TrainingPlans.route,
                onClick = { scope.launch { onClose(); navController.navigate(Screen.TrainingPlans.route) } },
                modifier = Modifier.padding(start = 28.dp, end = 8.dp)
            )
        }
    }
}
