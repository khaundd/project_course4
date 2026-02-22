package com.example.project_course4.composable_elements.auth

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun TextButtonRedirect(
    modifier: Modifier = Modifier,
    text: String,
    textDecoration: TextDecoration? = null,
    normalColor: Color,
    textAlign: TextAlign? = null,
    pressedColor: Color,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Text(
        text = text,
        color = if (isPressed) pressedColor else normalColor,
        fontWeight = FontWeight.Bold,
        textDecoration = textDecoration,
        textAlign = textAlign,
        modifier = modifier.then(
            Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    try {
                        awaitRelease()
                    } catch (e: Exception) {
                        isPressed = false
                        return@detectTapGestures
                    }
                    isPressed = false
                    onClick()
                })
            }
        )
    )
}
