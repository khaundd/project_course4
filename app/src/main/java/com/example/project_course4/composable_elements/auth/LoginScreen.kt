package com.example.project_course4.composable_elements.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.project_course4.Screen
import com.example.project_course4.utils.Validation
import com.example.project_course4.utils.NetworkUtils
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.example.project_course4.AuthViewModel
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.project_course4.R

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    val context = LocalContext.current
    val validation = remember { Validation() }
    var isLoading by remember { mutableStateOf(false) }
    var showNetworkError by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(validation.toastMessage) {
        validation.toastMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            validation.clearToastMessage()
        }
    }

    LaunchedEffect(validation.email) {
        if (validation.email.isNotEmpty()) validation.validateEmail()
    }
    LaunchedEffect(validation.password) {
        if (validation.password.isNotEmpty()) validation.validatePassword()
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation()
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

            CustomAuthButton(
                text = "Войти",
                isLoading = isLoading,
                onClick = {
                    keyboardController?.hide()

                    // Проверяем интернет-соединение перед входом
                    if (!NetworkUtils.isInternetAvailable(context)) {
                        showNetworkError = true
                        return@CustomAuthButton
                    }

                    // Принудительно валидируем все поля перед проверкой
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
                                if (NetworkUtils.isNetworkError(Exception(error))) {
                                    showNetworkError = true
                                } else {
                                    validation.toastMessage = error
                                }
                            }
                        )
                    } else {
                        validation.toastMessage = "Пожалуйста, исправьте ошибки в форме"
                    }
                },
                backgroundColor = colorResource(id = R.color.buttonColor),
                textColor = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Нет аккаунта? ")
                TextButtonRedirect(
                    text = "Зарегистрироваться",
                    normalColor = colorResource(id = R.color.textButtonRedirectColor),
                    pressedColor = colorResource(id = R.color.buttonColor),
                    onClick = { navController.navigate(Screen.Registration.route) },
                )
            }
        }
    }
    
    // AlertDialog для показа сообщения об отсутствии интернет-соединения
    if (showNetworkError) {
        AlertDialog(
            onDismissRequest = { showNetworkError = false },
            title = { Text("Ошибка сети") },
            text = { Text("Отсутствует интернет-соединение") },
            confirmButton = {
                TextButton(
                    onClick = { showNetworkError = false }
                ) {
                    Text("ОК")
                }
            }
        )
    }
}