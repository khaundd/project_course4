package com.example.project_course4.composable_elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Delete
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
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.project_course4.AuthViewModel
import com.example.project_course4.Screen
import com.example.project_course4.SessionManager
import com.example.project_course4.composable_elements.auth.TextButtonRedirect
import com.example.project_course4.utils.NetworkUtils
import com.example.project_course4.utils.Validation
import com.example.project_course4.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import com.example.project_course4.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val validation = remember { Validation() }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val profileData by profileViewModel.profileData.collectAsState()
    val dailyCalories by profileViewModel.dailyCalories.collectAsState()
    val userEmail = profileViewModel.getUserEmail() ?: "Email не найден"
    
    var showInfoDialog by remember { mutableStateOf(false) }
    
    // Состояния для редактирования
    var isEditingWeight by remember { mutableStateOf(false) }
    var isEditingHeight by remember { mutableStateOf(false) }
    var tempWeight by remember { mutableStateOf(profileData.weight.toString()) }
    var tempHeight by remember { mutableStateOf(profileData.height.toString()) }
    
    var expandedGoal by remember { mutableStateOf(false) }
    var expandedGender by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(validation.toastMessage) {
        validation.toastMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            validation.clearToastMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Меню", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    label = { Text("Дневник питания") },
                    selected = currentRoute == Screen.Main.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                    label = { Text("Создать продукт") },
                    selected = currentRoute?.startsWith(Screen.ProductCreation.route) == true,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("productCreation?barcode=")
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text("Профиль") },
                    selected = currentRoute == Screen.Profile.route,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
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
                            tempWeight.toFloatOrNull()?.let { weight ->
                                profileViewModel.updateWeight(weight)
                            }
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
                            tempHeight.toFloatOrNull()?.let { height ->
                                profileViewModel.updateHeight(height)
                            }
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
                    options = NutritionGoal.values(),
                    onOptionSelected = { goal ->
                        profileViewModel.updateGoal(goal)
                        expandedGoal = false
                    },
                    displayText = { it.displayName }
                )
                HorizontalDivider()
                
                DropdownProfileDataRow(
                    label = "Пол",
                    value = profileData.gender.displayName,
                    expanded = expandedGender,
                    onExpandedChange = { expandedGender = it },
                    options = Gender.values(),
                    onOptionSelected = { gender ->
                        profileViewModel.updateGender(gender)
                        expandedGender = false
                    },
                    displayText = { it.displayName }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${dailyCalories.toInt()} кКал",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Информация",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                    }
                }

                Text(
                    text = "Рекомендуемое количество калорий",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Каждый организм уникален. Пробуйте и ищите свою оптимальную калорийность",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                // Кнопка выхода
                TextButton(
                    onClick = {
                        // Проверяем интернет-соединение перед выходом
                        if (!NetworkUtils.isInternetAvailable(context)) {
                            validation.toastMessage = "Отсутствует интернет-соединение"
                        } else {
                            authViewModel.logout(
                                onSuccess = { message ->
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Main.route) { inclusive = true }
                                    }
                                },
                                onError = { error ->
                                    validation.toastMessage = error
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = null,
                        tint = colorResource(id = R.color.textButtonRedirectColor),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Выйти", 
                        color = colorResource(id = R.color.textButtonRedirectColor)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { /* TODO: удаление учётной записи */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Удалить учётную запись", color = Color.Red)
                }
            }
        }
    }
    
    // Диалог с информацией о расчете калорий
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text("Информация")
            },
            text = {
                Text("Рекомендуемая норма вычислена по формуле Миффлина-Сан Жеора")
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false }
                ) {
                    Text("ОК")
                }
            }
        )
    }
}

@Composable
private fun ProfileDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
                    TextButton(onClick = onSave) {
                        Text("Сохранить")
                    }
                    TextButton(onClick = onCancel) {
                        Text("Отмена")
                    }
                }
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 0.dp, top = 4.dp)
                    )
                }
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clickable { onEditClick() }
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .width(200.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(displayText(option)) },
                        onClick = {
                            onOptionSelected(option)
                        }
                    )
                }
            }
        }
    }
}
