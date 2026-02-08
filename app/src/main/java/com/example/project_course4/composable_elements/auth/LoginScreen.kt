package com.example.project_course4.composable_elements.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.project_course4.Screen
import com.example.project_course4.utils.Validation
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_course4.ViewModel
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun LoginScreen(navController: NavController) {
    val viewModel: ViewModel = viewModel()
    val validation = remember { Validation() }
    var isLoading by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 1. Состояние для Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // 2. Слушаем изменения toastMessage и показываем Snackbar
    LaunchedEffect(validation.toastMessage) {
        validation.toastMessage?.let { message ->
            // Показываем снекбар
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            // Очищаем сообщение в модели после показа
            validation.clearToastMessage()
        }
    }

    // Мониторинг валидации
    LaunchedEffect(validation.email) {
        if (validation.email.isNotEmpty()) validation.validateEmail()
    }
    LaunchedEffect(validation.password) {
        if (validation.password.isNotEmpty()) validation.validatePassword()
    }

    // 3. Используем Scaffold для правильного размещения Snackbar
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Важно использовать padding от Scaffold
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = validation.email,
                onValueChange = {
                    validation.email = it
                    validation.validateEmail()
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
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start).padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = validation.password,
                onValueChange = {
                    validation.password = it
                    validation.validatePassword()
                },
                label = { Text("Пароль") },
                modifier = Modifier.fillMaxWidth(),
                isError = validation.passwordError.isNotEmpty(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            if (validation.passwordError.isNotEmpty()) {
                Text(
                    text = validation.passwordError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start).padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    validation.validateEmail()
                    validation.validatePassword(isEmptyValid = false)

                    if (validation.isValidForLogin()) {
                        isLoading = true
                        viewModel.login(
                            email = validation.email,
                            password = validation.password,
                            onSuccess = {
                                isLoading = false
                                navController.navigate(Screen.Main.route) { popUpTo(0) }
                            },
                            onError = { error ->
                                isLoading = false
                                validation.toastMessage = error
                            }
                        )
                    } else {
                        validation.toastMessage = "Пожалуйста, исправьте ошибки в форме"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Войти")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Текст "Зарегистрироваться"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Нет аккаунта? ")
                Text(
                    text = "Зарегистрироваться",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { navController.navigate(Screen.Registration.route) }
                )
            }
        }
    }
}