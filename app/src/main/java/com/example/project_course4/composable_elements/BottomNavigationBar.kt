package com.example.project_course4.composable_elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.Screen

enum class TopLevelTab { NUTRITION, FITNESS, STATISTICS, PROFILE }

fun TopLevelTab.startRoute(): String = when (this) {
    TopLevelTab.NUTRITION  -> Screen.Main.route
    TopLevelTab.FITNESS    -> Screen.TrainingLog.route
    TopLevelTab.STATISTICS -> Screen.Statistics.route
    TopLevelTab.PROFILE    -> Screen.Profile.route
}

fun routeToTab(route: String?): TopLevelTab? = when {
    route == null -> null
    route == Screen.Profile.route -> TopLevelTab.PROFILE
    route == Screen.SelectTrainer.route -> TopLevelTab.PROFILE
    route.startsWith("roleFeature") -> TopLevelTab.PROFILE
    route.startsWith("clientStats") -> TopLevelTab.PROFILE
    route == Screen.Statistics.route -> TopLevelTab.STATISTICS
    route in listOf(
        Screen.Fitness.route,
        Screen.ExerciseCatalog.route,
        Screen.TrainingLog.route,
        Screen.TrainingPlans.route,
        Screen.ExerciseCatalogSelect.route,
        Screen.TrainingEditor.route,
        Screen.TrainingPlanEditor.route,
        Screen.ActiveWorkout.route,
        Screen.WorkoutSummary.route,
        Screen.ExerciseCatalogSelectActive.route
    ) -> TopLevelTab.FITNESS
    route.startsWith("exerciseCatalogSelectPlan") -> TopLevelTab.FITNESS
    route.startsWith("trainingPlanDetail") -> TopLevelTab.FITNESS
    route.startsWith("trainingDetail") -> TopLevelTab.FITNESS
    route.startsWith("exerciseDetail") -> TopLevelTab.FITNESS
    route in listOf(
        Screen.Main.route,
        Screen.SelectProduct.route,
        Screen.Products.route,
        Screen.Recipes.route,
        Screen.RecipeCreation.route,
        Screen.SelectProductForRecipe.route,
        Screen.MealPlans.route,
        Screen.MealPlanEditor.route
    ) -> TopLevelTab.NUTRITION
    route.startsWith("selectProductWithMeal") -> TopLevelTab.NUTRITION
    route.startsWith("dishComposition") -> TopLevelTab.NUTRITION
    route.startsWith("recipeEdit") -> TopLevelTab.NUTRITION
    route.startsWith("sharedRecipe") -> TopLevelTab.NUTRITION
    route.startsWith("selectProductForMealPlan") -> TopLevelTab.NUTRITION
    route.startsWith("mealPlanDetail") -> TopLevelTab.NUTRITION
    route.startsWith("productCreation") -> TopLevelTab.NUTRITION
    else -> null
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?
) {
    val currentTab = routeToTab(currentRoute)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 16.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationItem(
                icon = Icons.Default.Restaurant,
                label = "Питание",
                isSelected = currentTab == TopLevelTab.NUTRITION,
                onClick = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) { inclusive = false; saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )

            NavigationItem(
                icon = Icons.Default.FitnessCenter,
                label = "Активность",
                isSelected = currentTab == TopLevelTab.FITNESS,
                onClick = {
                    navController.navigate(Screen.TrainingLog.route) {
                        popUpTo(Screen.Main.route) { inclusive = false; saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )

            NavigationItem(
                icon = Icons.Default.BarChart,
                label = "Статистика",
                isSelected = currentTab == TopLevelTab.STATISTICS,
                onClick = {
                    navController.navigate(Screen.Statistics.route) {
                        popUpTo(Screen.Main.route) { inclusive = false; saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )

            NavigationItem(
                icon = Icons.Outlined.Person,
                label = "Профиль",
                isSelected = currentTab == TopLevelTab.PROFILE,
                onClick = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Main.route) { inclusive = false; saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun NavigationItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var lastClickTime by remember { mutableStateOf(0L) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                val now = System.currentTimeMillis()
                if (now - lastClickTime > 500L) {
                    lastClickTime = now
                    onClick()
                }
            }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFF4CAF50) else Color.Gray
        )
    }
}
