package com.example.project_course4.composable_elements.screens.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.example.project_course4.api.ExerciseResponse
import com.example.project_course4.viewmodel.FitnessViewModel
import com.example.project_course4.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    navController: NavController,
    viewModel: FitnessViewModel,
    exerciseId: Int
) {
    val exercise by viewModel.selectedExercise.collectAsState()
    val isLoading by viewModel.isLoadingExerciseDetail.collectAsState()
    var isNavigatingBack by remember { mutableStateOf(false) }

    LaunchedEffect(exerciseId) {
        viewModel.loadExerciseDetail(exerciseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Информация об упражнении") },
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
        if (isLoading || exercise == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        ExerciseDetailContent(exercise = exercise!!, modifier = Modifier.padding(padding))
    }
}

@Composable
fun ExerciseDetailContent(exercise: ExerciseResponse, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Название упражнения
        item {
            Text(
                text = exercise.nameRu ?: exercise.name,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        }

        // Оборудование
        exercise.equipment?.let { eq ->
            item {
                Text("Необходимое оборудование: $eq", fontSize = 15.sp, color = Color.Gray)
            }
        }

        // Техника выполнения
        if (exercise.instructions.isNotEmpty()) {
            item {
                Text(
                    "Техника выполнения",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(exercise.instructions.sortedBy { it.stepOrder }) { step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF4CAF50), RoundedCornerShape(50))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${step.stepOrder}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = step.instructionRu ?: step.instruction,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // GIF с индикатором загрузки
        exercise.gifUrl?.let { gifPath ->
            item {
                val gifFullUrl = "https://loftily-adequate-urchin.cloudpub.ru/$gifPath"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(gifFullUrl)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = exercise.nameRu ?: exercise.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                            }
                        }
                    )
                }
            }
        }

        // Целевая мышца
        item {
            MuscleSection(
                title = "Целевая мышца",
                muscles = listOfNotNull(exercise.targetMuscleName),
                color = Color(0xFF7C4DFF)
            )
        }

        // Вторичные мышцы (только если есть)
        if (exercise.secondaryMuscles.isNotEmpty()) {
            item {
                MuscleSection(
                    title = "Вторичные мышцы",
                    muscles = exercise.secondaryMuscles.map { it.name },
                    color = Color(0xFFB39DDB)
                )
            }
        }

        // Категория и уровень
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercise.category?.let {
                    MetaChip("Категория", it, Color(0xFF9C27B0))
                }
                exercise.level?.let {
                    MetaChip(
                        label = "Уровень",
                        value = when (it) {
                            "beginner" -> "Начинающий"
                            "intermediate" -> "Средний"
                            "expert" -> "Продвинутый"
                            else -> it
                        },
                        color = when (it) {
                            "beginner" -> Color(0xFF4CAF50)
                            "intermediate" -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaChip(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 10.sp, color = color.copy(alpha = 0.7f))
        Text(value, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MuscleSection(title: String, muscles: List<String>, color: Color) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            muscles.forEach { muscle ->
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(muscle, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
