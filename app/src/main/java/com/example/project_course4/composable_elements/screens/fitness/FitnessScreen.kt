package com.example.project_course4.composable_elements.screens.fitness

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(
    navController: NavController,
    onStartEmptyWorkout: () -> Unit = {}
) {
    var isNavigatingBack by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Физическая активность") },
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FitnessMenuCard(
                icon = Icons.Default.PlayArrow,
                title = "Начать тренировку",
                subtitle = "Записывайте подходы, вес и повторения в реальном времени",
                color = Color(0xFF4CAF50),
                onClick = onStartEmptyWorkout
            )

            FitnessMenuCard(
                icon = Icons.Default.FitnessCenter,
                title = "Каталог упражнений",
                subtitle = "Просмотр упражнений с фильтрацией по мышцам, оборудованию и уровню",
                color = Color(0xFF4CAF50),
                onClick = { navController.navigate("exerciseCatalog") }
            )

            FitnessMenuCard(
                icon = Icons.AutoMirrored.Filled.List,
                title = "Дневник тренировок",
                subtitle = "Записывайте свои тренировки и отслеживайте прогресс",
                color = Color(0xFF2196F3),
                onClick = { navController.navigate("trainingLog") }
            )

            FitnessMenuCard(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "Планы тренировок",
                subtitle = "Создавайте и используйте готовые программы тренировок",
                color = Color(0xFF9C27B0),
                onClick = { navController.navigate("trainingPlans") }
            )
        }
    }
}

@Composable
private fun FitnessMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable { onClick() },
        shape = shape,
        color = color.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {}
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}
