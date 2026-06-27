package com.example.project_course4.composable_elements.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.viewmodel.AuthViewModel
import com.example.project_course4.Screen
import com.example.project_course4.UserRole
import com.example.project_course4.utils.NetworkUtils
import com.example.project_course4.utils.Validation
import com.example.project_course4.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import com.example.project_course4.R
import com.example.project_course4.composable_elements.Gender
import com.example.project_course4.composable_elements.NutritionGoal


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    userRole: UserRole = UserRole.USER,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val validation = remember { Validation() }
    val snackbarHostState = remember { SnackbarHostState() }

    val profileData by profileViewModel.profileData.collectAsState()
    val dailyCalories by profileViewModel.dailyCalories.collectAsState()
    val userEmail = profileViewModel.getUserEmail() ?: "Email не найден"
    val useCustomCalories by profileViewModel.useCustomCalories.collectAsState()
    val customCalories by profileViewModel.customCalories.collectAsState()

    var showInfoDialog by remember { mutableStateOf(false) }
    var isEditingCustomCalories by remember { mutableStateOf(false) }
    var tempCustomCalories by remember { mutableStateOf("") }

    var isEditingWeight by remember { mutableStateOf(false) }
    var isEditingHeight by remember { mutableStateOf(false) }
    var tempWeight by remember { mutableStateOf(profileData.weight.toString()) }
    var tempHeight by remember { mutableStateOf(profileData.height.toString()) }

    var expandedGoal by remember { mutableStateOf(false) }
    var expandedGender by remember { mutableStateOf(false) }
    var isLogoutInProgress by remember { mutableStateOf(false) }
    var showRoleMenu by remember { mutableStateOf(false) }

    LaunchedEffect(validation.toastMessage) {
        validation.toastMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            validation.clearToastMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header row with title and role menu button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Профиль",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp
                )
                if (userRole.isTrainerOrAbove) {
                    Box {
                        IconButton(onClick = { showRoleMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Меню роли"
                            )
                        }
                        DropdownMenu(
                            expanded = showRoleMenu,
                            onDismissRequest = { showRoleMenu = false }
                        ) {
                            if (userRole.isTrainerOrAbove) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Person,
                                                contentDescription = null
                                            )
                                            Text("Панель тренера")
                                        }
                                    },
                                    onClick = {
                                        showRoleMenu = false
                                        navController.navigate("roleFeature/${UserRole.TRAINER.id}")
                                    }
                                )
                            }
                            if (userRole.isAdmin) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AdminPanelSettings,
                                                contentDescription = null
                                            )
                                            Text("Панель администратора")
                                        }
                                    },
                                    onClick = {
                                        showRoleMenu = false
                                        navController.navigate("roleFeature/${UserRole.ADMIN.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }

                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                EditableProfileDataRow(
                    label = "Вес",
                    value = "${profileData.weight.toInt()} кг",
                    isEditing = isEditingWeight,
                    tempValue = tempWeight,
                    errorMessage = validation.weightError,
                    onEditClick = {
                        isEditingWeight = true
                        tempWeight = profileData.weight.toString()
                        validation.weight = ""
                        validation.weightError = ""
                    },
                    onValueChange = { tempWeight = it },
                    onSave = {
                        validation.weight = tempWeight
                        validation.validateWeight()
                        if (validation.weightError.isEmpty()) {
                            tempWeight.toFloatOrNull()?.let { weight -> profileViewModel.updateWeight(weight) }
                            isEditingWeight = false
                        }
                    },
                    onCancel = { isEditingWeight = false }
                )
                HorizontalDivider()

                EditableProfileDataRow(
                    label = "Рост",
                    value = "${profileData.height.toInt()} см",
                    isEditing = isEditingHeight,
                    tempValue = tempHeight,
                    errorMessage = validation.heightError,
                    onEditClick = {
                        isEditingHeight = true
                        tempHeight = profileData.height.toString()
                        validation.height = ""
                        validation.heightError = ""
                    },
                    onValueChange = { tempHeight = it },
                    onSave = {
                        validation.height = tempHeight
                        validation.validateHeight()
                        if (validation.heightError.isEmpty()) {
                            tempHeight.toFloatOrNull()?.let { height -> profileViewModel.updateHeight(height) }
                            isEditingHeight = false
                        }
                    },
                    onCancel = { isEditingHeight = false }
                )
                HorizontalDivider()

                ProfileDataRow(label = "Возраст", value = "${profileData.age} лет")
                HorizontalDivider()

                DropdownProfileDataRow(
                    label = "Цель",
                    value = profileData.goal.displayName,
                    expanded = expandedGoal,
                    onExpandedChange = { expandedGoal = it },
                    options = NutritionGoal.entries.toTypedArray(),
                    onOptionSelected = { goal -> profileViewModel.updateGoal(goal); expandedGoal = false },
                    displayText = { it.displayName }
                )
                HorizontalDivider()

                DropdownProfileDataRow(
                    label = "Пол",
                    value = profileData.gender.displayName,
                    expanded = expandedGender,
                    onExpandedChange = { expandedGender = it },
                    options = Gender.entries.toTypedArray(),
                    onOptionSelected = { gender -> profileViewModel.updateGender(gender); expandedGender = false },
                    displayText = { it.displayName }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditingCustomCalories) {
                        TextField(
                            value = tempCustomCalories,
                            onValueChange = { tempCustomCalories = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(120.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            suffix = { Text(" кКал") }
                        )
                        TextButton(onClick = {
                            tempCustomCalories.toFloatOrNull()?.let { profileViewModel.setCustomCalories(it) }
                            isEditingCustomCalories = false
                        }) { Text("Сохранить") }
                        TextButton(onClick = { isEditingCustomCalories = false }) { Text("Отмена") }
                    } else {
                        Text(
                            text = "${dailyCalories.toInt()} кКал",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = if (useCustomCalories) Modifier.clickable {
                                tempCustomCalories = customCalories.toInt().toString()
                                isEditingCustomCalories = true
                            } else Modifier
                        )
                        IconButton(onClick = { showInfoDialog = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "Информация", modifier = Modifier.size(20.dp), tint = Color.Gray)
                        }
                    }
                }

                if (!useCustomCalories) {
                    Text(
                        text = "Рекомендуемое количество калорий",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Своя норма калорий", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = useCustomCalories,
                        onCheckedChange = { checked ->
                            profileViewModel.setUseCustomCalories(checked)
                            if (checked && customCalories <= 0f) profileViewModel.setCustomCalories(dailyCalories)
                        }
                    )
                }

                Text(
                    text = if (useCustomCalories) "Нажмите на значение калорий чтобы изменить"
                           else "Каждый организм уникален. Пробуйте и ищите свою оптимальную калорийность",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Кнопка «Найти тренера» только для обычных пользователей
                if (userRole == UserRole.USER) {
                    OutlinedButton(
                        onClick = { navController.navigate(Screen.SelectTrainer.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Найти тренера")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                TextButton(
                    onClick = {
                        if (isLogoutInProgress) return@TextButton
                        if (!NetworkUtils.isInternetAvailable(context)) {
                            validation.toastMessage = "Отсутствует интернет-соединение"
                        } else {
                            isLogoutInProgress = true
                            authViewModel.logout(
                                onSuccess = { _ ->
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Main.route) { inclusive = true }
                                    }
                                },
                                onError = { error ->
                                    validation.toastMessage = error
                                    isLogoutInProgress = false
                                }
                            )
                        }
                    },
                    enabled = !isLogoutInProgress,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = colorResource(id = R.color.textButtonRedirectColor), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isLogoutInProgress) "Выход..." else "Выйти", color = colorResource(id = R.color.textButtonRedirectColor))
                }

                Spacer(modifier = Modifier.height(8.dp))

            }
        }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Информация") },
            text = { Text("Рекомендуемая норма вычислена по формуле Миффлина-Сан Жеора") },
            confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("ОК") } }
        )
    }
}

@Composable
private fun ProfileDataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EditableProfileDataRow(
    label: String,
    value: String,
    isEditing: Boolean,
    tempValue: String,
    errorMessage: String = "",
    onEditClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        if (isEditing) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = tempValue,
                        onValueChange = onValueChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        isError = errorMessage.isNotEmpty()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onSave) { Text("Сохранить") }
                    TextButton(onClick = onCancel) { Text("Отмена") }
                }
                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            Text(text = value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.clickable { onEditClick() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownProfileDataRow(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: Array<T>,
    onOptionSelected: (T) -> Unit,
    displayText: (T) -> String = { it.toString() }
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
            OutlinedTextField(
                value = value,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().width(200.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(displayText(option)) }, onClick = { onOptionSelected(option) })
                }
            }
        }
    }
}
