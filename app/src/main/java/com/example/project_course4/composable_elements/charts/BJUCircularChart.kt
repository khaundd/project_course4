package com.example.project_course4.composable_elements.charts

import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.project_course4.ui.theme.CarbColor
import com.example.project_course4.ui.theme.FatColor
import com.example.project_course4.ui.theme.ProteinColor
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BJUCircularChart(
    modifier: Modifier = Modifier,
    protein: Float,
    fats: Float,
    carbs: Float,
    chartSize: Float = 60f
) {
    val total = protein + fats + carbs
    
    Canvas(
        modifier = modifier
            .size(chartSize.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.width * 0.4f
        val strokeWidth = chartSize * 0.8f
        
        val startAngle = -90f
        
        // Calculate angles for each nutrient
        val proteinAngle = if (total > 0) (protein / total) * 360f else 0f
        val fatsAngle = if (total > 0) (fats / total) * 360f else 0f
        val carbsAngle = if (total > 0) (carbs / total) * 360f else 0f
        
        var currentAngle = startAngle
        
        // Draw protein segment
        if (proteinAngle > 0) {
            drawArc(
                color = ProteinColor,
                startAngle = currentAngle,
                sweepAngle = proteinAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            currentAngle += proteinAngle
        }
        
        // Draw fats segment
        if (fatsAngle > 0) {
            drawArc(
                color = FatColor,
                startAngle = currentAngle,
                sweepAngle = fatsAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            currentAngle += fatsAngle
        }
        
        // Draw carbs segment
        if (carbsAngle > 0) {
            drawArc(
                color = CarbColor,
                startAngle = currentAngle,
                sweepAngle = carbsAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
        }
        
        // Draw center circle (white)
        drawCircle(
            color = Color.White,
            radius = radius - strokeWidth / 2,
            center = Offset(centerX, centerY)
        )
    }
}

@Composable
fun BJUCircularChartWithText(
    modifier: Modifier = Modifier,
    protein: Float,
    fats: Float,
    carbs: Float,
    chartSize: Float = 120f
) {
    val total = protein + fats + carbs
    
    Canvas(
        modifier = modifier
            .size(chartSize.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.width * 0.4f
        val strokeWidth = chartSize * 0.8f
        
        val startAngle = -90f
        
        // Calculate angles for each nutrient
        val proteinAngle = if (total > 0) (protein / total) * 360f else 0f
        val fatsAngle = if (total > 0) (fats / total) * 360f else 0f
        val carbsAngle = if (total > 0) (carbs / total) * 360f else 0f
        
        var currentAngle = startAngle
        
        // Draw protein segment
        if (proteinAngle > 0) {
            drawArc(
                color = ProteinColor,
                startAngle = currentAngle,
                sweepAngle = proteinAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            currentAngle += proteinAngle
        }
        
        // Draw fats segment
        if (fatsAngle > 0) {
            drawArc(
                color = FatColor,
                startAngle = currentAngle,
                sweepAngle = fatsAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            currentAngle += fatsAngle
        }
        
        // Draw carbs segment
        if (carbsAngle > 0) {
            drawArc(
                color = CarbColor,
                startAngle = currentAngle,
                sweepAngle = carbsAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
        }
        
        // Draw center circle (white)
        drawCircle(
            color = Color.White,
            radius = radius - strokeWidth / 2,
            center = Offset(centerX, centerY)
        )
        
        // Draw text in center
        drawContext.canvas.nativeCanvas.apply {
            val textPaint = AndroidPaint().apply {
                isAntiAlias = true
                textAlign = AndroidPaint.Align.CENTER
                color = AndroidColor.BLACK
                textSize = 24f
                isFakeBoldText = true
            }
            
            // Draw "БЖУ" text
            drawText("БЖУ", centerX, centerY + 8f, textPaint)
        }
    }
}

@Composable
fun BJUCircularChartWithCalories(
    modifier: Modifier = Modifier,
    protein: Float,
    fats: Float,
    carbs: Float,
    calories: Float,
    chartSize: Float = 180f
) {
    val total = protein + fats + carbs

    Canvas(
        modifier = modifier.size(chartSize.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.width * 0.4f
        val strokeWidth = chartSize * 0.8f

        val startAngle = -90f
        val proteinAngle = if (total > 0) (protein / total) * 360f else 0f
        val fatsAngle = if (total > 0) (fats / total) * 360f else 0f
        val carbsAngle = if (total > 0) (carbs / total) * 360f else 0f

        var currentAngle = startAngle

        if (proteinAngle > 0) {
            drawArc(
                color = ProteinColor,
                startAngle = currentAngle,
                sweepAngle = proteinAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            currentAngle += proteinAngle
        }
        if (fatsAngle > 0) {
            drawArc(
                color = FatColor,
                startAngle = currentAngle,
                sweepAngle = fatsAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            currentAngle += fatsAngle
        }
        if (carbsAngle > 0) {
            drawArc(
                color = CarbColor,
                startAngle = currentAngle,
                sweepAngle = carbsAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
        }

        drawCircle(
            color = Color.White,
            radius = radius - strokeWidth / 2,
            center = Offset(centerX, centerY)
        )

        drawContext.canvas.nativeCanvas.apply {
            val paint = AndroidPaint().apply {
                isAntiAlias = true
                textAlign = AndroidPaint.Align.CENTER
                color = AndroidColor.BLACK
                isFakeBoldText = true
            }
            paint.textSize = 52f
            drawText("${calories.toInt()}", centerX, centerY + 18f, paint)
            paint.textSize = 28f
            paint.isFakeBoldText = false
            paint.color = AndroidColor.GRAY
            drawText("кКал", centerX, centerY + 46f, paint)

            // White labels in the middle of each arc
            val arcLabelPaint = AndroidPaint().apply {
                isAntiAlias = true
                textAlign = AndroidPaint.Align.CENTER
                color = AndroidColor.WHITE
                isFakeBoldText = true
                textSize = chartSize * 0.3f
            }

            fun drawArcLabel(value: Float, startAngle: Float, sweepAngle: Float) {
                if (sweepAngle < 15f) return
                val midAngle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                val lx = centerX + radius * cos(midAngle).toFloat()
                val ly = centerY + radius * sin(midAngle).toFloat() + arcLabelPaint.textSize * 0.35f
                drawText("%.0f".format(value), lx, ly, arcLabelPaint)
            }

            var angle = startAngle
            drawArcLabel(protein, angle, proteinAngle); angle += proteinAngle
            drawArcLabel(fats, angle, fatsAngle); angle += fatsAngle
            drawArcLabel(carbs, angle, carbsAngle)
        }
    }
}
