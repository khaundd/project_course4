package com.example.project_course4.composable_elements

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import com.example.project_course4.R.drawable.barcode_scanner_24px
import com.example.project_course4.R.drawable.ic_close_24px
import com.example.project_course4.R.drawable.ic_check_24px
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.project_course4.Product
import com.example.project_course4.ProductCreationValidator
import com.example.project_course4.ValidationResult
import com.example.project_course4.viewmodel.ProductViewModel
import com.example.project_course4.utils.NetworkUtils
import com.example.project_course4.utils.ErrorHandler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCreationScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    onBarcodeScan: (String) -> Unit,
    initialBarcode: String? = null
) {
    val context = LocalContext.current
    val state by viewModel.productCreationState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showNetworkError by remember { mutableStateOf(false) }
    var isNavigatingBack by remember { mutableStateOf(false) }
    Log.d("ProductCreation", "Текущее состояние barcode в UI: '${state.barcode}'")
    Log.d("ProductCreation", "initialBarcode: $initialBarcode")

    // подставляем отсканированный штрих-код в поле
    LaunchedEffect(initialBarcode) {
        Log.d("ProductCreation", "LaunchedEffect сработал с initialBarcode: $initialBarcode")
        initialBarcode?.takeIf { it.isNotBlank() }?.let { barcode ->
            Log.d("ProductCreation", "Обновляем состояние с barcode: $barcode")
            viewModel.updateProductCreationState(state.copy(barcode = barcode))
        }
    }

    // Очищаем ошибки при выходе с экрана
    DisposableEffect(Unit) {
        onDispose {
            val clearedState = state.copy(
                nameError = null,
                proteinError = null,
                fatsError = null,
                carbsError = null,
                macrosError = null,
                barcodeError = null
            )
            viewModel.updateProductCreationState(clearedState)
        }
    }
    val validator = remember { ProductCreationValidator() }
    var calories by rememberSaveable { mutableStateOf(0f) }

    // обновляем калорийность при изменении БЖУ
    LaunchedEffect(state.protein, state.fats, state.carbs) {
        val protein = state.protein.toFloatOrNull() ?: 0f
        val fats = state.fats.toFloatOrNull() ?: 0f
        val carbs = state.carbs.toFloatOrNull() ?: 0f
        calories = validator.calculateCalories(protein, fats, carbs)

        // Валидация суммы БЖУ
        val macrosValidation = validator.validateMacros(state.protein, state.fats, state.carbs)
        val macrosError =
            if (macrosValidation is ValidationResult.Invalid) macrosValidation.errorMessage else null
        // Обновляем только macrosError, если он изменился
        if (state.macrosError != macrosError) {
            viewModel.updateProductCreationState(state.copy(macrosError = macrosError))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (state.name.isNotBlank()) state.name else "Новый продукт",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isNavigatingBack) {
                                isNavigatingBack = true
                                navController.popBackStack()
                            }
                        },
                        enabled = !isNavigatingBack
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(ic_close_24px),
                            contentDescription = "Закрыть"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Проверяем интернет-соединение перед сохранением
                            if (!NetworkUtils.isInternetAvailable(context)) {
                                viewModel.setLoading(false)
                                showNetworkError = true
                                return@IconButton
                            }

                            // Устанавливаем состояние загрузки немедленно
                            viewModel.setLoading(true)

                            val nameValidation = validator.validateName(state.name)
                            val proteinValidation =
                                validator.validateFloatValue(state.protein, "Белки")
                            val fatsValidation = validator.validateFloatValue(state.fats, "Жиры")
                            val carbsValidation =
                                validator.validateFloatValue(state.carbs, "Углеводы")
                            val macrosValidation =
                                validator.validateMacros(state.protein, state.fats, state.carbs)

                            val updatedState = state.copy(
                                nameError = if (nameValidation is ValidationResult.Invalid) nameValidation.errorMessage else null,
                                proteinError = if (proteinValidation is ValidationResult.Invalid) proteinValidation.errorMessage else null,
                                fatsError = if (fatsValidation is ValidationResult.Invalid) fatsValidation.errorMessage else null,
                                carbsError = if (carbsValidation is ValidationResult.Invalid) carbsValidation.errorMessage else null,
                                macrosError = if (macrosValidation is ValidationResult.Invalid) macrosValidation.errorMessage else null,
                                barcodeError = null
                            )

                            viewModel.updateProductCreationState(updatedState)

                            if (updatedState.nameError == null &&
                                updatedState.proteinError == null &&
                                updatedState.fatsError == null &&
                                updatedState.carbsError == null &&
                                updatedState.macrosError == null
                            ) {

                                // Проверяем уникальность названия и сохраняем продукт
                                viewModel.viewModelScope.launch {
                                    try {
                                        val checkResult =
                                            viewModel.checkProductNameExists(state.name)
                                        checkResult.fold(
                                            onSuccess = { exists ->
                                                if (exists) {
                                                    viewModel.setLoading(false)
                                                    val nameExistsState = updatedState.copy(
                                                        nameError = "Продукт с таким названием уже существует"
                                                    )
                                                    viewModel.updateProductCreationState(
                                                        nameExistsState
                                                    )
                                                } else {
                                                    val protein =
                                                        state.protein.toFloatOrNull() ?: 0f
                                                    val fats = state.fats.toFloatOrNull() ?: 0f
                                                    val carbs = state.carbs.toFloatOrNull() ?: 0f

                                                    val newProduct = Product(
                                                        productId = 0,
                                                        name = state.name,
                                                        protein = protein,
                                                        fats = fats,
                                                        carbs = carbs,
                                                        calories = calories,
                                                        barcode = state.barcode.takeIf { it.isNotBlank() }
                                                    )

                                                    val saveResult =
                                                        viewModel.addNewProduct(newProduct)
                                                    saveResult.fold(
                                                        onSuccess = { savedProduct ->
                                                            Log.d(
                                                                "ProductCreation",
                                                                "Продукт успешно сохранен: ${savedProduct.name}"
                                                            )
                                                            // Сначала выполняем навигацию, затем очищаем состояние
                                                            navController.popBackStack()
                                                            viewModel.hideProductCreationScreen()
                                                        },
                                                        onFailure = { error ->
                                                            val errorMessage = ErrorHandler.handleNetworkException(error)
                                                            if (NetworkUtils.isNetworkError(error)) {
                                                                viewModel.setLoading(false)
                                                                showNetworkError = true
                                                            } else {
                                                                val errorState = updatedState.copy(
                                                                    nameError = "Ошибка сохранения: $errorMessage"
                                                                )
                                                                viewModel.updateProductCreationState(
                                                                    errorState
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            },
                                            onFailure = { error ->
                                                viewModel.setLoading(false)
                                                val errorMessage = ErrorHandler.handleNetworkException(error)
                                                if (NetworkUtils.isNetworkError(error)) {
                                                    showNetworkError = true
                                                } else {
                                                    val errorState = updatedState.copy(
                                                        nameError = "Ошибка проверки названия: $errorMessage"
                                                    )
                                                    viewModel.updateProductCreationState(errorState)
                                                }
                                            }
                                        )
                                    } catch (e: Exception) {
                                        viewModel.setLoading(false)
                                        val errorMessage = ErrorHandler.handleNetworkException(e)
                                        if (NetworkUtils.isNetworkError(e)) {
                                            showNetworkError = true
                                        } else {
                                            val errorState = updatedState.copy(
                                                nameError = "Ошибка: $errorMessage"
                                            )
                                            viewModel.updateProductCreationState(errorState)
                                        }
                                    }
                                }
                            } else {
                                viewModel.setLoading(false)
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = ImageVector.vectorResource(ic_check_24px),
                                contentDescription = "Сохранить"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Поле для названия продукта по центру
            TransparentTextField(
                value = state.name,
                onValueChange = { name ->
                    val newState = state.copy(name = name)
                    val validationResult = validator.validateName(name)
                    val nameError = if (validationResult is ValidationResult.Invalid) validationResult.errorMessage else null
                    viewModel.updateProductCreationState(newState.copy(nameError = nameError))
                },
                placeholder = "Название",
                isError = state.nameError != null,
                errorMessage = state.nameError,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 20.sp,
                hintColor = Color.Red
            )

            // Остальные поля ввода
            LabeledTransparentTextField(
                label = "Белки, на 100 г.",
                unit = "г.",
                value = state.protein,
                onValueChange = { protein ->
                    val newState = state.copy(protein = protein)
                    val validationResult = validator.validateFloatValue(protein, "Белки")
                    val proteinError = if (validationResult is ValidationResult.Invalid) validationResult.errorMessage else null
                    viewModel.updateProductCreationState(newState.copy(proteinError = proteinError))
                },
                isError = state.proteinError != null,
                errorMessage = state.proteinError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = "0"
            )

            LabeledTransparentTextField(
                label = "Жиры, на 100 г.",
                unit = "г.",
                value = state.fats,
                onValueChange = { fats ->
                    val newState = state.copy(fats = fats)
                    val validationResult = validator.validateFloatValue(fats, "Жиры")
                    val fatsError = if (validationResult is ValidationResult.Invalid) validationResult.errorMessage else null
                    viewModel.updateProductCreationState(newState.copy(fatsError = fatsError))
                },
                isError = state.fatsError != null,
                errorMessage = state.fatsError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = "0"
            )

            LabeledTransparentTextField(
                label = "Углеводы, на 100 г.",
                unit = "г.",
                value = state.carbs,
                onValueChange = { carbs ->
                    val newState = state.copy(carbs = carbs)
                    val validationResult = validator.validateFloatValue(carbs, "Углеводы")
                    val carbsError = if (validationResult is ValidationResult.Invalid) validationResult.errorMessage else null
                    viewModel.updateProductCreationState(newState.copy(carbsError = carbsError))
                },
                isError = state.carbsError != null,
                errorMessage = state.carbsError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = "0"
            )

            LabeledTransparentTextField(
                label = "Калории кКал, на 100 г.",
                unit = "",
                value = "${"%.2f".format(calories)}",
                onValueChange = {},
                enabled = false
            )

            // Отображение ошибки БЖУ
            if (state.macrosError != null) {
                Text(
                    text = state.macrosError!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Поле для штрих-кода с кнопкой сканера
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabeledTransparentTextField(
                    label = "Штрих-код",
                    unit = "",
                    value = state.barcode,
                    onValueChange = { barcode ->
                        Log.d("ProductCreation", "TextField изменен, новое значение: $barcode")
                        val newState = state.copy(barcode = barcode)
                        viewModel.updateProductCreationState(newState)
                    },
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { onBarcodeScan("OPEN_SCANNER") },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(barcode_scanner_24px),
                        contentDescription = "Сканировать штрих-код"
                    )
                }
            }
        }

        // AlertDialog для показа сообщения об отсутствии интернет-соединения
        if (showNetworkError) {
            AlertDialog(
                onDismissRequest = {
                    showNetworkError = false
                    // Очищаем все состояния ошибок при закрытии диалога
                    val clearedState = state.copy(
                        nameError = null,
                        proteinError = null,
                        fatsError = null,
                        carbsError = null,
                        macrosError = null,
                        barcodeError = null
                    )
                    viewModel.updateProductCreationState(clearedState)
                },
                title = { Text("Ошибка сети") },
                text = { Text("Отсутствует интернет-соединение") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showNetworkError = false
                            // Очищаем все состояния ошибок при нажатии на ОК
                            val clearedState = state.copy(
                                nameError = null,
                                proteinError = null,
                                fatsError = null,
                                carbsError = null,
                                macrosError = null,
                                barcodeError = null
                            )
                            viewModel.updateProductCreationState(clearedState)
                        }
                    ) {
                        Text("ОК")
                    }
                }
            )
        }
    }
}
