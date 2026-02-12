package com.example.project_course4.composable_elements.charts

import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.project_course4.ui.theme.CarbColor
import com.example.project_course4.ui.theme.FatColor
import com.example.project_course4.ui.theme.ProteinColor

@Composable
fun NutritionChart(
    protein: Float,
    fats: Float,
    carbs: Float,
    totalCalories: Float,
    modifier: Modifier = Modifier
) {
    // Константы норм
    val targetProtein = 100f
    val targetFats = 70f
    val targetCarbs = 230f
    val targetCalories = targetProtein * 4 + targetFats * 9 + targetCarbs * 4

    // Расчет долей в общей дуге (180 градусов) на основе весов норм
    val totalTargetWeight = targetProtein + targetFats + targetCarbs

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val strokeWidthTarget = 60f  // Толщина линии нормы
        val strokeWidthProgress = 60f // Толщина линии съеденного
        val centerX = size.width / 2
        val centerY = size.height * 0.9f
        val radius = size.width * 0.35f

        val startAngle = 180f
        val maxAngle = 180f

        val textPaint = AndroidPaint().apply {
            isAntiAlias = true
            textAlign = AndroidPaint.Align.CENTER
        }

        // Данные для отрисовки: Текущее, Норма, Цвет, Название
        val layers = listOf(
            Triple(protein, targetProtein, ProteinColor to "Белки"),
            Triple(fats, targetFats, FatColor to "Жиры"),
            Triple(carbs, targetCarbs, CarbColor to "Углеводы")
        )

        var currentAngle = startAngle

        layers.forEach { (current, target, info) ->
            val (color, label) = info
            // Угол, который этот нутриент занимает в общей норме
            val segmentMaxAngle = maxAngle * (target / totalTargetWeight)

            // 1. Рисуем подложку (Норма) - полупрозрачная или светлая
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = currentAngle,
                sweepAngle = segmentMaxAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidthTarget, cap = StrokeCap.Round)
            )

            // 2. Рисуем прогресс (Съеденное)
            // Ограничиваем sweepAngle, чтобы полоса не выходила за пределы своей секции нормы (max 100%)
            val progressPercent = (current / target).coerceAtMost(1f)
            val progressSweepAngle = segmentMaxAngle * progressPercent

            if (progressSweepAngle > 0) {
                drawArc(
                    color = color,
                    startAngle = currentAngle,
                    sweepAngle = progressSweepAngle,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidthProgress, cap = StrokeCap.Round)
                )
            }

            // 3. Легенда (только для нормы) огибает область
            drawContext.canvas.nativeCanvas.apply {
                val path = AndroidPath()
                val rectF = RectF(
                    centerX - radius - 45f, centerY - radius - 45f,
                    centerX + radius + 45f, centerY + radius + 45f
                )
                path.addArc(rectF, currentAngle, segmentMaxAngle)

                textPaint.textSize = 30f
                textPaint.color = AndroidColor.BLACK
                textPaint.isFakeBoldText = false
                drawTextOnPath(label, path, 0f, 0f, textPaint)

                // Значение внутри (съеденные граммы)
                val midAngle = currentAngle + progressSweepAngle / 2
                val angleRad = Math.toRadians(midAngle.toDouble())
                val vX = centerX + (radius * kotlin.math.cos(angleRad)).toFloat()
                val vY = centerY + (radius * kotlin.math.sin(angleRad)).toFloat()

                if (current > 0) {
                    textPaint.color = AndroidColor.WHITE
                    textPaint.textSize = 26f
                    textPaint.isFakeBoldText = true
                    drawText("${current.toInt()}г", vX, vY + 10f, textPaint)
                }
            }

            currentAngle += segmentMaxAngle
        }

        // 4. Текст по центру (Калории)
        drawContext.canvas.nativeCanvas.apply {
            // Съеденные калории (Bold)
            textPaint.color = AndroidColor.BLACK
            textPaint.textSize = 64f
            textPaint.isFakeBoldText = true
            drawText("${totalCalories.toInt()}", centerX, centerY - 80f, textPaint)

            // Норма калорий
            textPaint.textSize = 32f
            textPaint.isFakeBoldText = false
            textPaint.color = AndroidColor.GRAY
            drawText("${targetCalories.toInt()} кКал", centerX, centerY - 35f, textPaint)
        }
    }
}