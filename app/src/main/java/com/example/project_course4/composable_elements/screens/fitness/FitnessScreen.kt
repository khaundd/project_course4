package com.example.project_course4.composable_elements.screens.fitness

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    val fitnessChips = listOf(
        "Дневник"    to Screen.TrainingLog.route,
        "Упражнения" to Screen.ExerciseCatalog.route,
        "Планы"      to Screen.TrainingPlans.route
    )

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Заголовок + чипсы
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Активность",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fitnessChips.forEach { (label, route) ->
                        FilterChip(
                            selected = false,
                            onClick = { navController.navigate(route) { launchSingleTop = true } },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4CAF50),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
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
                    onClick = { navController.navigate(Screen.ExerciseCatalog.route) }
                )

                FitnessMenuCard(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = "Дневник тренировок",
                    subtitle = "Записывайте свои тренировки и отслеживайте прогресс",
                    color = Color(0xFF2196F3),
                    onClick = { navController.navigate(Screen.TrainingLog.route) }
                )

                FitnessMenuCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Планы тренировок",
                    subtitle = "Создавайте и используйте готовые программы тренировок",
                    color = Color(0xFF9C27B0),
                    onClick = { navController.navigate(Screen.TrainingPlans.route) }
                )
            }
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
