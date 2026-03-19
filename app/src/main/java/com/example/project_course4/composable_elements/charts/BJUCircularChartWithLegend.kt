package com.example.project_course4.composable_elements.charts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_course4.ui.theme.CarbColor
import com.example.project_course4.ui.theme.FatColor
import com.example.project_course4.ui.theme.ProteinColor

@Composable
fun BJUCircularChartWithLegend(
    protein: Float,
    fats: Float,
    carbs: Float,
    calories: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Легенда
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = ProteinColor, label = "Белки")
            LegendItem(color = FatColor, label = "Жиры")
            LegendItem(color = CarbColor, label = "Углеводы")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Диаграмма с калориями в центре
        Box(contentAlignment = Alignment.Center) {
            BJUCircularChartWithCalories(
                protein = protein,
                fats = fats,
                carbs = carbs,
                calories = calories,
                chartSize = 140f
            )
        }
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .also { }
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(color = color)
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 12.sp)
    }
}
