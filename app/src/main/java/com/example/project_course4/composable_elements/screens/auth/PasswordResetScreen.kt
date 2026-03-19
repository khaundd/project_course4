package com.example.project_course4.composable_elements.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.project_course4.viewmodel.AuthViewModel
import com.example.project_course4.R
import com.example.project_course4.Screen
import com.example.project_course4.composable_elements.CustomButton
import com.example.project_course4.utils.NetworkUtils

private enum class ResetStep { EMAIL, CODE, NEW_PASSWORD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordResetScreen(navController: NavController, viewModel: AuthViewModel) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(ResetStep.EMAIL) }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Восстановление пароля") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "reset_step"
            ) { currentStep ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (currentStep) {
                        ResetStep.EMAIL -> {
                            Text(
                                "Введите email, на который зарегистрирован аккаунт",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it; errorMessage = null },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                isError = errorMessage != null
                            )
                            if (errorMessage != null) {
                                Text(errorMessage!!, color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            CustomButton(
                                text = "Далее",
                                isLoading = isLoading,
                                backgroundColor = colorResource(id = R.color.buttonColor),
                                textColor = Color.White,
                                onClick = {
                                    if (email.isBlank()) { errorMessage = "Введите email"; return@CustomButton }
                                    if (!NetworkUtils.isInternetAvailable(context)) { errorMessage = "Нет интернет-соединения"; return@CustomButton }
                                    isLoading = true
                                    viewModel.requestPasswordReset(
                                        email = email.trim(),
                                        onSuccess = { isLoading = false; step = ResetStep.CODE },
                                        onError = { isLoading = false; errorMessage = it }
                                    )
                                }
                            )
                        }

                        ResetStep.CODE -> {
                            Text(
                                "Введите код, отправленный на $email",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it; errorMessage = null },
                                label = { Text("Код подтверждения") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = errorMessage != null
                            )
                            if (errorMessage != null) {
                                Text(errorMessage!!, color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            CustomButton(
                                text = "Проверить код",
                                isLoading = isLoading,
                                backgroundColor = colorResource(id = R.color.buttonColor),
                                textColor = Color.White,
                                onClick = {
                                    if (code.isBlank()) { errorMessage = "Введите код"; return@CustomButton }
                                    isLoading = true
                                    viewModel.verifyPasswordResetCode(
                                        email = email.trim(),
                                        code = code.trim(),
                                        onSuccess = { isLoading = false; step = ResetStep.NEW_PASSWORD },
                                        onError = { isLoading = false; errorMessage = it }
                                    )
                                }
                            )
                        }

                        ResetStep.NEW_PASSWORD -> {
                            Text(
                                "Введите новый пароль",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it; errorMessage = null },
                                label = { Text("Новый пароль") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                isError = errorMessage != null
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMessage = null },
                                label = { Text("Подтвердите пароль") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                isError = errorMessage != null
                            )
                            if (errorMessage != null) {
                                Text(errorMessage!!, color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            CustomButton(
                                text = "Подтвердить",
                                isLoading = isLoading,
                                backgroundColor = colorResource(id = R.color.buttonColor),
                                textColor = Color.White,
                                onClick = {
                                    if (newPassword.isBlank()) { errorMessage = "Введите пароль"; return@CustomButton }
                                    if (newPassword.length < 6) { errorMessage = "Пароль должен быть не менее 6 символов"; return@CustomButton }
                                    if (newPassword != confirmPassword) { errorMessage = "Пароли не совпадают"; return@CustomButton }
                                    isLoading = true
                                    viewModel.confirmPasswordReset(
                                        email = email.trim(),
                                        code = code.trim(),
                                        newPassword = newPassword,
                                        onSuccess = {
                                            isLoading = false
                                            navController.navigate(Screen.Main.route) { popUpTo(0) }
                                        },
                                        onError = { isLoading = false; errorMessage = it }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
