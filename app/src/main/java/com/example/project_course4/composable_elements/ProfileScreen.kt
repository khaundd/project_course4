package com.example.project_course4.composable_elements

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.project_course4.AuthViewModel
import com.example.project_course4.Screen
import com.example.project_course4.composable_elements.auth.TextButtonRedirect
import com.example.project_course4.utils.NetworkUtils
import com.example.project_course4.utils.Validation
import kotlinx.coroutines.launch
import com.example.project_course4.R

// Заглушки — в будущем будут браться из БД или настройки приложения
private const val PLACEHOLDER_EMAIL = "admin@admin.com"
private const val PLACEHOLDER_WEIGHT = "71 кг"
private const val PLACEHOLDER_HEIGHT = "178 см"
private const val PLACEHOLDER_AGE = "20 лет"
private const val PLACEHOLDER_GOAL = "Набрать"
private const val PLACEHOLDER_GENDER = "Мужчина"
private const val PLACEHOLDER_CALORIES = "2945 кКал"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val validation = remember { Validation() }
    val snackbarHostState = remember { SnackbarHostState() }

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
                    text = PLACEHOLDER_EMAIL,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                TextButtonRedirect(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    text = "Выйти",
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
                    normalColor = colorResource(id = R.color.textButtonRedirectColor),
                    pressedColor = colorResource(id = R.color.buttonColor),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProfileDataRow(label = "Вес", value = PLACEHOLDER_WEIGHT)
                HorizontalDivider()
                ProfileDataRow(label = "Рост", value = PLACEHOLDER_HEIGHT)
                HorizontalDivider()
                ProfileDataRow(label = "Возраст", value = PLACEHOLDER_AGE)
                HorizontalDivider()
                ProfileDataRow(label = "Цель", value = PLACEHOLDER_GOAL)
                HorizontalDivider()
                ProfileDataRow(label = "Пол", value = PLACEHOLDER_GENDER)

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = PLACEHOLDER_CALORIES,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { /* TODO: показать подсказку */ },
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
