package com.example.project_course4.composable_elements.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.unit.dp
import com.example.project_course4.ui.theme.CarbColor
import com.example.project_course4.ui.theme.FatColor
import com.example.project_course4.ui.theme.ProteinColor

@Composable
fun NutritionChart(
    protein: Float,
    fats: Float,
    carbs: Float,
    modifier: Modifier = Modifier
) {
    val total = protein + fats + carbs
    val proteinPercent = if (total > 0) protein / total else 0f
    val fatsPercent = if (total > 0) fats / total else 0f
    val carbsPercent = if (total > 0) carbs / total else 0f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height * 0.3f // Сохраняем центр
        val radius = minOf(size.width * 0.35f, size.height * 0.5f) // Увеличиваем радиус в два раза для увеличения диаграммы

        // Ограничиваем диаграмму полукругом (180 градусов), направленным вверх как холм
        val maxAngle = 180f // Положительный угол для направления вверх
        val startAngle = -180f // Начинаем с левой стороны для вертикального полукруга-холма
        
        // Отображение белков (левая часть полукруга-холма)
        if (proteinPercent > 0) {
            drawArc(
                color = ProteinColor,
                startAngle = startAngle,
                sweepAngle = maxAngle * proteinPercent,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 40f)
            )
        }
        
        // Отображение жиров (центральная часть полукруга-холма)
        if (fatsPercent > 0) {
            val fatsStartAngle = startAngle + maxAngle * proteinPercent
            drawArc(
                color = FatColor,
                startAngle = fatsStartAngle,
                sweepAngle = maxAngle * fatsPercent,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 40f)
            )
        }
        
        // Отображение углеводов (правая часть полукруга-холма)
        if (carbsPercent > 0) {
            val carbsStartAngle = startAngle + maxAngle * (proteinPercent + fatsPercent)
            drawArc(
                color = CarbColor,
                startAngle = carbsStartAngle,
                sweepAngle = maxAngle * carbsPercent,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 40f)
            )
        }
        
        // Создаем единый текстовый объект для рисования
        val textPaint = AndroidPaint().apply {
            textSize = 24f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        // Функция для рисования текста вдоль дуги
        fun drawTextAlongArc(
            text: String,
            value: Float,
            color: Color,
            startAngle: Float,
            sweepAngle: Float
        ) {
            // Вычисляем угол для размещения текста (половина сегмента)
            val midAngle = startAngle + sweepAngle / 2
            
            // Преобразуем угол в радианы
            val angleRad = Math.toRadians(midAngle.toDouble())
            
            // Вычисляем позицию для текста (по середине ширины дуги)
            val textRadius = radius - 40f // Смещаем текст ближе к центру (пропорционально увеличению диаграммы)
            val textX = centerX + (textRadius * kotlin.math.cos(angleRad)).toFloat()
            val textY = centerY + (textRadius * kotlin.math.sin(angleRad)).toFloat()
            
            // Рисуем название макронутриента
            textPaint.color = android.graphics.Color.BLACK
            drawContext.canvas.nativeCanvas.drawText(
                text,
                textX,
                textY,
                textPaint
            )
            
            // Вычисляем позицию для значения внутри дуги
            val valueRadius = radius - 120f // Еще ближе к центру (пропорционально увеличению диаграммы)
            val valueX = centerX + (valueRadius * kotlin.math.cos(angleRad)).toFloat()
            val valueY = centerY + (valueRadius * kotlin.math.sin(angleRad)).toFloat()
            
            // Рисуем значение внутри дуги
            textPaint.color = color.toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                "${"%.1f".format(value)}г",
                valueX,
                valueY,
                textPaint
            )
        }
        
        // Рисуем текст вдоль каждой дуги
        if (proteinPercent > 0) {
            drawTextAlongArc(
                "Белки",
                protein,
                ProteinColor,
                startAngle,
                maxAngle * proteinPercent
            )
        }
        
        if (fatsPercent > 0) {
            drawTextAlongArc(
                "Жиры",
                fats,
                FatColor,
                startAngle + maxAngle * proteinPercent,
                maxAngle * fatsPercent
            )
        }
        
        if (carbsPercent > 0) {
            drawTextAlongArc(
                "Углев.",
                carbs,
                CarbColor,
                startAngle + maxAngle * (proteinPercent + fatsPercent),
                maxAngle * carbsPercent
            )
        }
    }
}