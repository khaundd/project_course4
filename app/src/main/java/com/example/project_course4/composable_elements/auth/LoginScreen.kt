package com.example.project_course4.composable_elements.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.project_course4.Screen
import com.example.project_course4.utils.Validation
import androidx.compose.runtime.LaunchedEffect

@Composable
fun LoginScreen(navController: NavController) {
    val validation = remember { Validation() }

    // Мониторинг изменений поля email для мгновенной валидации
    LaunchedEffect(validation.email) {
        validation.validateEmail()
    }

    // Мониторинг изменений поля password для мгновенной валидации, только если поле не пустое
    LaunchedEffect(validation.password) {
        if (validation.password.isNotEmpty()) {
            validation.validatePassword()
        } else {
            validation.passwordError = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = validation.email,
            onValueChange = { newValue ->
                validation.email = newValue
            },
            label = { Text("Почта") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = validation.emailError.isNotEmpty()
        )
        if (validation.emailError.isNotEmpty()) {
            Text(
                text = validation.emailError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = validation.password,
            onValueChange = { newValue ->
                validation.password = newValue
            },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            isError = validation.passwordError.isNotEmpty(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        if (validation.passwordError.isNotEmpty()) {
            Text(
                text = validation.passwordError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                var isValid = true

                validation.validatePassword(isEmptyValid = true)
                if (!validation.isValid()) {
                    isValid = false
                }

                if (isValid) {
                    navController.navigate(Screen.Main.route)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Войти")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Нет аккаунта? ")
            val annotatedText = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("Зарегистрироваться")
                }
            }
            Text(
                text = annotatedText,
                modifier = Modifier.clickable { navController.navigate(Screen.Registration.route) }
            )
        }
    }
}