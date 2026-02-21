package com.example.project_course4.composable_elements.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.example.project_course4.Screen
import com.example.project_course4.utils.Validation
import com.example.project_course4.utils.NetworkUtils
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project_course4.AuthViewModel
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun RegistrationScreen(navController: NavController, viewModel: AuthViewModel) {
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

    LaunchedEffect(validation.login) {
        if (validation.login.isNotEmpty()) validation.validateLogin()
    }

    LaunchedEffect(validation.password) {
        if (validation.password.isNotEmpty()) validation.validatePassword()
        else validation.passwordError = ""
    }

    LaunchedEffect(validation.email) {
        if (validation.email.isNotEmpty()) validation.validateEmail()
    }
    LaunchedEffect(validation.height) { if (validation.height.isNotEmpty()) validation.validateHeight() }
    LaunchedEffect(validation.weight) { if (validation.weight.isNotEmpty()) validation.validateWeight() }
    LaunchedEffect(validation.age) { if (validation.age.isNotEmpty()) validation.validateAge() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    snackbarData = data
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            OutlinedTextField(
                value = validation.login,
                onValueChange = {
                    validation.login = it
                    validation.validateLogin()
                                },
                label = { Text("Логин") },
                modifier = Modifier.fillMaxWidth(),
                isError = validation.loginError.isNotEmpty()
            )
            if (validation.loginError.isNotEmpty()) {
                Text(
                    text = validation.loginError,
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
                    if (validation.passwordConfirmation.isNotEmpty()) {
                        validation.validatePasswordConfirmation()
                    }
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = validation.passwordConfirmation,
                onValueChange = {
                    validation.passwordConfirmation = it
                    validation.validatePasswordConfirmation()
                },
                label = { Text("Подтвердите пароль") },
                modifier = Modifier.fillMaxWidth(),
                isError = validation.passwordConfirmationError.isNotEmpty(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation()
            )
            if (validation.passwordConfirmationError.isNotEmpty()) {
                Text(
                    text = validation.passwordConfirmationError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start).padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                value = validation.height,
                onValueChange = {
                    validation.height = it
                    validation.validateHeight()
                },
                label = { Text("Рост") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = validation.heightError.isNotEmpty()
            )
            if (validation.heightError.isNotEmpty()) {
                Text(
                    text = validation.heightError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start).padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = validation.weight,
                onValueChange = {
                    validation.weight = it
                    validation.validateWeight()
                },
                label = { Text("Вес") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = validation.weightError.isNotEmpty()
            )
            if (validation.weightError.isNotEmpty()) {
                Text(
                    text = validation.weightError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start).padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = validation.age,
                onValueChange = {
                    validation.age = it
                    validation.validateAge()
                },
                label = { Text("Возраст") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = validation.ageError.isNotEmpty()
            )
            if (validation.ageError.isNotEmpty()) {
                Text(
                    text = validation.ageError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start).padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    
                    // Проверяем интернет-соединение перед регистрацией
                    if (!NetworkUtils.isInternetAvailable(context)) {
                        showNetworkError = true
                        return@Button
                    }
                    
                    // Принудительно валидируем все поля перед проверкой
                    validation.validateLogin()
                    validation.validatePassword()
                    validation.validatePasswordConfirmation()
                    validation.validateEmail()
                    validation.validateHeight()
                    validation.validateWeight()
                    validation.validateAge()
                    
                    if (validation.isValidForRegistration()) {
                        val h = validation.height.toFloatOrNull() ?: 0f
                        val w = validation.weight.toFloatOrNull() ?: 0f
                        val a = validation.age.toIntOrNull() ?: 0

                        isLoading = true
                        viewModel.register(
                            username = validation.login,
                            password = validation.password,
                            email = validation.email,
                            height = h,
                            bodyweight = w,
                            age = a,
                            onSuccess = {
                                isLoading = false
                                navController.navigate(Screen.Verification.route + "?email=${validation.email}")
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
                    Text("Зарегистрироваться")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Есть аккаунт? ")
                Text(
                    text = "Войти",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { navController.navigate(Screen.Login.route) }
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