package com.example.project_course4.composable_elements

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

// Функция для рисования анимации заполнения от центра
private fun DrawScope.drawFillAnimation(
    progress: Float,
    fillColor: Color,
    size: Size,
    borderColor: Color,
    cornerRadius: Dp
) {
    if (progress > 0f) {
        val maxRadius = sqrt(size.width * size.width + size.height * size.height) / 2f
        val currentRadius = maxRadius * progress
        val center = Offset(size.width / 2f, size.height / 2f)
        
        drawCircle(
            color = fillColor,
            radius = currentRadius,
            center = center,
            alpha = progress
        )
        
        // Рисуем рамку, когда кнопка почти полностью заполнена
        if (progress > 0.8f) {
            val borderAlpha = (progress - 0.8f) / 0.2f
            drawRoundRect(
                color = borderColor.copy(alpha = borderAlpha),
                size = size,
                topLeft = Offset.Zero,
                cornerRadius = CornerRadius(cornerRadius.toPx()),
                style = Stroke(
                    width = 2.dp.toPx()
                )
            )
        }
    }
}

@Composable
fun CustomButton(
    modifier: Modifier = Modifier,
    text: String = "",
    icon: (@Composable (tint: Color) -> Unit)? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color? = null,
    cornerRadius: Dp = 8.dp,
    fillMaxWidth: Boolean = true,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val buttonAnimationProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = EaseOutCubic),
        label = "buttonAnimation"
    )

    val baseModifier = if (fillMaxWidth) modifier.fillMaxWidth().height(48.dp) else modifier

    Box(
        modifier = baseModifier
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind {
                val fillColor = when {
                    isLoading -> Color.White
                    !enabled -> Color.Gray
                    else -> backgroundColor
                }
                drawRect(color = fillColor, size = size)
                // Static border for outlined style
                if (borderColor != null && !isLoading) {
                    drawRoundRect(
                        color = borderColor,
                        size = size,
                        topLeft = Offset.Zero,
                        cornerRadius = CornerRadius(cornerRadius.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                if (isLoading) {
                    drawRoundRect(
                        color = backgroundColor,
                        size = size,
                        topLeft = Offset.Zero,
                        cornerRadius = CornerRadius(cornerRadius.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                } else if (enabled) {
                    drawFillAnimation(
                        progress = buttonAnimationProgress,
                        fillColor = textColor,
                        size = size,
                        borderColor = borderColor ?: backgroundColor,
                        cornerRadius = cornerRadius
                    )
                }
            }
            .pointerInput(enabled, isLoading) {
                detectTapGestures(
                    onPress = {
                        if (!enabled || isLoading) return@detectTapGestures
                        isPressed = true
                        try { awaitRelease() } catch (_: Exception) { isPressed = false; return@detectTapGestures }
                        isPressed = false
                        currentOnClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val contentColor = if (buttonAnimationProgress > 0.5f) backgroundColor else textColor
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = backgroundColor, strokeWidth = 2.dp)
            icon != null -> icon(contentColor)
            else -> Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
