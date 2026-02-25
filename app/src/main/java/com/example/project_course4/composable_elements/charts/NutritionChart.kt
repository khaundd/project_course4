package com.example.project_course4.composable_elements.charts

import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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
    targetProtein: Float = 100f,
    targetFats: Float = 70f,
    targetCarbs: Float = 230f,
    targetCalories: Float = 1950f,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Анимация для плавного перехода
    val transitionProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "chart_transition"
    )
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Проверяем, был ли клип по области диаграммы
                    if (offset.x in 0f..size.width.toFloat() && offset.y in 0f..size.height.toFloat()) {
                        isExpanded = !isExpanded
                    }
                }
            }
    ) {
        val strokeWidthTarget = 60f
        val strokeWidthProgress = 60f
        
        if (!isExpanded) {
            // Режим одной круговой диаграммы калорий
            drawCaloriesChart(
                totalCalories = totalCalories,
                targetCalories = targetCalories,
                strokeWidthTarget = strokeWidthTarget,
                strokeWidthProgress = strokeWidthProgress,
                centerX = size.width / 2,
                centerY = size.height / 2,
                radius = size.width * 0.2f
            )
        } else {
            // Режим трёх диаграмм БЖУ
            val chartWidth = size.width / 3
            val spacing = chartWidth * 0.1f
            
            // Белки
            drawNutrientChart(
                current = protein,
                target = targetProtein,
                color = ProteinColor,
                label = "Белки",
                unit = "г",
                strokeWidthTarget = strokeWidthTarget * 0.9f,
                strokeWidthProgress = strokeWidthProgress * 0.9f,
                centerX = chartWidth / 2,
                centerY = size.height / 2,
                radius = chartWidth * 0.3f,
                transitionProgress = transitionProgress
            )
            
            // Жиры (на месте калорий)
            drawNutrientChart(
                current = fats,
                target = targetFats,
                color = FatColor,
                label = "Жиры",
                unit = "г",
                strokeWidthTarget = strokeWidthTarget * 0.9f,
                strokeWidthProgress = strokeWidthProgress * 0.9f,
                centerX = size.width / 2,
                centerY = size.height / 2,
                radius = chartWidth * 0.3f,
                transitionProgress = transitionProgress
            )
            
            // Углеводы
            drawNutrientChart(
                current = carbs,
                target = targetCarbs,
                color = CarbColor,
                label = "Углеводы",
                unit = "г",
                strokeWidthTarget = strokeWidthTarget * 0.9f,
                strokeWidthProgress = strokeWidthProgress * 0.9f,
                centerX = chartWidth * 2.5f,
                centerY = size.height / 2,
                radius = chartWidth * 0.3f,
                transitionProgress = transitionProgress
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCaloriesChart(
    totalCalories: Float,
    targetCalories: Float,
    strokeWidthTarget: Float,
    strokeWidthProgress: Float,
    centerX: Float,
    centerY: Float,
    radius: Float
) {
    val startAngle = -90f
    val maxAngle = 360f
    
    // Подложка с нормой
    drawArc(
        color = Color(0xFFE63946).copy(alpha = 0.2f),
        startAngle = startAngle,
        sweepAngle = maxAngle,
        useCenter = false,
        topLeft = Offset(centerX - radius, centerY - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidthTarget, cap = StrokeCap.Round)
    )
    
    // Прогресс (съеденные калории)
    val progressPercent = (totalCalories / targetCalories).coerceAtMost(1f)
    val progressSweepAngle = maxAngle * progressPercent
    
    if (progressSweepAngle > 0) {
        drawArc(
            color = Color(0xFFE63946),
            startAngle = startAngle,
            sweepAngle = progressSweepAngle,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidthProgress, cap = StrokeCap.Round)
        )
    }
    
    // Текст в центре круга
    drawContext.canvas.nativeCanvas.apply {
        val textPaint = AndroidPaint().apply {
            isAntiAlias = true
            textAlign = AndroidPaint.Align.CENTER
        }
        
        // Съеденные калории (жирный)
        textPaint.color = AndroidColor.BLACK
        textPaint.textSize = 48f
        textPaint.isFakeBoldText = true
        drawText("${totalCalories.toInt()}", centerX, centerY - 10, textPaint)
        
        // Норма
        textPaint.textSize = 24f
        textPaint.isFakeBoldText = false
        textPaint.color = AndroidColor.GRAY
        drawText("из ${targetCalories.toInt()} кКал", centerX, centerY + 20f, textPaint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNutrientChart(
    current: Float,
    target: Float,
    color: Color,
    label: String,
    unit: String,
    strokeWidthTarget: Float,
    strokeWidthProgress: Float,
    centerX: Float,
    centerY: Float,
    radius: Float,
    transitionProgress: Float
) {
    val startAngle = -90f
    val maxAngle = 360f
    
    // Анимация появления
    val alpha = transitionProgress
    val scale = 0.8f + (0.2f * transitionProgress)
    val animatedRadius = radius * scale
    
    // Подложка с нормой
    drawArc(
        color = color.copy(alpha = 0.2f * alpha),
        startAngle = startAngle,
        sweepAngle = maxAngle,
        useCenter = false,
        topLeft = Offset(centerX - animatedRadius, centerY - animatedRadius),
        size = Size(animatedRadius * 2, animatedRadius * 2),
        style = Stroke(width = strokeWidthTarget, cap = StrokeCap.Round)
    )
    
    // Прогресс (съеденное)
    val progressPercent = (current / target).coerceAtMost(1f)
    val progressSweepAngle = maxAngle * progressPercent
    
    if (progressSweepAngle > 0) {
        drawArc(
            color = color.copy(alpha = alpha),
            startAngle = startAngle,
            sweepAngle = progressSweepAngle,
            useCenter = false,
            topLeft = Offset(centerX - animatedRadius, centerY - animatedRadius),
            size = Size(animatedRadius * 2, animatedRadius * 2),
            style = Stroke(width = strokeWidthProgress, cap = StrokeCap.Round)
        )
    }
    
    // Текст в центре круга
    drawContext.canvas.nativeCanvas.apply {
        val textPaint = AndroidPaint().apply {
            isAntiAlias = true
            textAlign = AndroidPaint.Align.CENTER
        }
        
        // Съеденное количество (жирный)
        textPaint.color = AndroidColor.BLACK
        textPaint.textSize = 36f
        textPaint.isFakeBoldText = true
        textPaint.alpha = (255 * alpha).toInt()
        drawText("${current.toInt()}$unit", centerX, centerY - 8f, textPaint)
        
        // Норма
        textPaint.textSize = 18f
        textPaint.isFakeBoldText = false
        textPaint.color = AndroidColor.GRAY
        textPaint.alpha = (255 * alpha).toInt()
        drawText("из ${target.toInt()}$unit", centerX, centerY + 15f, textPaint)
        
        // Название нутриента
        textPaint.textSize = 16f
        textPaint.color = color.toArgb()
        textPaint.alpha = (255 * alpha).toInt()
        drawText(label, centerX, centerY + 35f, textPaint)
    }
}