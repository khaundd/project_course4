package com.example.project_course4.composable_elements.screens.recipe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.Product
import com.example.project_course4.R
import com.example.project_course4.composable_elements.CustomButton
import com.example.project_course4.composable_elements.charts.BJUCircularChartWithText
import com.example.project_course4.viewmodel.RecipeCreationViewModel
import com.example.project_course4.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishCompositionScreen(
    navController: NavController,
    dishName: String,
    recipeViewModel: RecipeViewModel? = null,
    recipeCreationViewModel: RecipeCreationViewModel? = null
) {
    val recipe = recipeViewModel?.getRecipeByName(dishName)

    val ingredients = recipe?.dishComposition?.map { it.productName to it.weight.toFloat() }
        ?: emptyList()
    val protein = recipe?.protein ?: 0f
    val fats = recipe?.fats ?: 0f
    val carbs = recipe?.carbs ?: 0f
    val calories = recipe?.calories ?: 0f

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Состав блюда",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = Color.Black.copy(alpha = 0.1f),
                        spotColor = Color.Black.copy(alpha = 0.15f)
                    )
            ) {
                    // Top block - Dish name with rounded bottom corners only
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 0.dp,
                                    bottomStart = 14.dp,
                                    bottomEnd = 14.dp
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = dishName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Dotted line separator (without background)
                    DottedLineSeparator()

                    // Bottom block - Ingredients and nutrition with rounded top corners only
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Ingredients list
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ingredients.forEachIndexed { index, (name, weight) ->
                                Text(
                                    text = "${index + 1}. $name x ${weight.toInt()} гр.",
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Solid separator line after ingredients
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.Gray)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Nutrition info and chart
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Б Ж У",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "${protein.toInt()} ${fats.toInt()} ${carbs.toInt()}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${calories.toInt()} кКал.",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }

                            BJUCircularChartWithText(
                                protein = protein,
                                fats = fats,
                                carbs = carbs,
                                chartSize = 100f
                            )
                        }
                    }
            }

            // Кнопка редактирования рецепта
            if (recipe != null && recipeCreationViewModel != null) {
                Spacer(modifier = Modifier.height(16.dp))
                CustomButton(
                    text = "Изменить рецепт",
                    backgroundColor = colorResource(id = R.color.buttonColor),
                    textColor = Color.White,
                    cornerRadius = 50.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val editIngredients = recipe.dishComposition.map { ingredient ->
                            RecipeIngredientItem(
                                product = Product(
                                    productId = ingredient.productId,
                                    name = ingredient.productName,
                                    protein = ingredient.protein,
                                    fats = ingredient.fats,
                                    carbs = ingredient.carbs,
                                    calories = ingredient.calories
                                ),
                                weight = ingredient.weight
                            )
                        }
                        recipeCreationViewModel.loadForEditing(
                            productId = recipe.productId,
                            name = recipe.name,
                            ingredients = editIngredients,
                            afterCookingWeight = 0f,
                            description = ""
                        )
                        navController.navigate("recipeEdit/${recipe.name}")
                    }
                )
            }
        }
    }
}


@Composable
fun DottedLineSeparator() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 12.dp) // Same padding as the blocks
    ) {
        val strokeWidth = 1.dp.toPx()
        val dashLength = 8.dp.toPx()
        val gapLength = 4.dp.toPx()
        
        val path = Path()
        var currentX = 0f
        
        while (currentX < size.width) {
            val nextX = (currentX + dashLength).coerceAtMost(size.width)
            path.moveTo(currentX, size.height / 2)
            path.lineTo(nextX, size.height / 2)
            currentX = nextX + gapLength
        }
        
        drawPath(
            path = path,
            color = Color.Gray,
            style = Stroke(width = strokeWidth)
        )
    }
}
