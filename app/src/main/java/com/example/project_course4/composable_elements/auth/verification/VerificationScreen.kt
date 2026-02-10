package com.example.project_course4.composable_elements.auth.verification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.project_course4.Screen
import com.example.project_course4.ViewModel
import com.example.project_course4.utils.Validation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun VerificationScreen(navController: NavController, email: String, viewModel: ViewModel) {
    val validation = remember { Validation() }
    var isLoading by remember { mutableStateOf(false) }

    // Контроллер клавиатуры
    val keyboardController = LocalSoftwareKeyboardController.current
    // Состояние для Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Эффект для показа ошибок через Snackbar (если нужно)
    LaunchedEffect(validation.toastMessage) {
        validation.toastMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            validation.clearToastMessage()
        }
    }

    // Мониторинг изменений поля кода
    LaunchedEffect(validation.code) {
        if (validation.code.isNotEmpty()) {
            validation.validateCode()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Введите код из письма",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = validation.code,
                onValueChange = { newValue ->
                    if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                        validation.code = newValue
                        validation.validateCode()
                    }
                },
                label = { Text("Код подтверждения") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading, // Блокируем ввод при загрузке
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = validation.codeError.isNotEmpty()
            )

            if (validation.codeError.isNotEmpty()) {
                Text(
                    text = validation.codeError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start).padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    keyboardController?.hide() // Скрываем клавиатуру

                    if (validation.code.length != 6) {
                        validation.codeError = "Код должен содержать 6 цифр"
                        return@Button
                    }

                    isLoading = true
                    validation.codeError = "" // Очищаем текст ошибки перед запросом

                    viewModel.verifyEmail(
                        email = email,
                        code = validation.code,
                        onSuccess = { _ ->
                            isLoading = false
                            navController.navigate(Screen.Main.route) {
                                popUpTo(Screen.Registration.route) { inclusive = true }
                            }
                        },
                        onError = { errorMessage ->
                            isLoading = false
                            validation.codeError = errorMessage
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading // Кнопка выключена во время загрузки
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Подтвердить")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Текст под полем ввода (сделали более темным)
            Text(
                text = "Код был отправлен на $email",
                // Используем onSurface без прозрачности или с высокой альфой (0.8f - 1.0f)
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}