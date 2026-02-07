package com.example.project_course4.composable_elements.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.example.project_course4.Screen
import com.example.project_course4.utils.Validation
import androidx.compose.runtime.LaunchedEffect

@Composable
fun RegistrationScreen(navController: NavController) {
    val validation = remember { Validation() }

    // Мониторинг изменений полей для мгновенной валидации
    LaunchedEffect(validation.login) {
        validation.validateLogin()
    }

    // Мониторинг изменений поля password для мгновенной валидации, только если поле не пустое
    LaunchedEffect(validation.password) {
        if (validation.password.isNotEmpty()) {
            validation.validatePassword()
        } else {
            validation.passwordError = ""
        }
    }

    LaunchedEffect(validation.email) {
        validation.validateEmail()
    }

    LaunchedEffect(validation.height) {
        validation.validateHeight()
    }

    LaunchedEffect(validation.weight) {
        validation.validateWeight()
    }

    LaunchedEffect(validation.age) {
        validation.validateAge()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = validation.login,
            onValueChange = { newValue ->
                validation.login = newValue
            },
            label = { Text("Логин") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            isError = validation.loginError.isNotEmpty()
        )
        if (validation.loginError.isNotEmpty()) {
            Text(
                text = validation.loginError,
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
            isError = validation.passwordError.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (validation.passwordError.isNotEmpty()) {
            Text(
                text = validation.passwordError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = validation.email,
            onValueChange = { newValue ->
                validation.email = newValue
            },
            label = { Text("Почта") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
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
            value = validation.height,
            onValueChange = { newValue ->
                validation.height = newValue
            },
            label = { Text("Рост") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        if (validation.heightError.isNotEmpty()) {
            Text(
                text = validation.heightError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = validation.weight,
            onValueChange = { newValue ->
                validation.weight = newValue
            },
            label = { Text("Вес") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        if (validation.weightError.isNotEmpty()) {
            Text(
                text = validation.weightError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = validation.age,
            onValueChange = { newValue ->
                validation.age = newValue
            },
            label = { Text("Возраст") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        if (validation.ageError.isNotEmpty()) {
            Text(
                text = validation.ageError,
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
            Text("Зарегистрироваться")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Есть аккаунт? ")
            val annotatedText = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("Войти")
                }
            }
            Text(
                text = annotatedText,
                modifier = Modifier.clickable { navController.navigate(Screen.Login.route) }
            )
        }
    }
}