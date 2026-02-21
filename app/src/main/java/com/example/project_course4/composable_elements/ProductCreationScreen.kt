package com.example.project_course4.composable_elements

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import com.example.project_course4.R.drawable.barcode_scanner_24px
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.project_course4.Product
import com.example.project_course4.ProductCreationValidator
import com.example.project_course4.ValidationResult
import com.example.project_course4.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@Composable
fun ProductCreationScreen(
    navController: NavController,
    viewModel: ProductViewModel,
    onBarcodeScan: (String) -> Unit,
    initialBarcode: String? = null
) {
    val state by viewModel.productCreationState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
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
        val macrosError = if (macrosValidation is ValidationResult.Invalid) macrosValidation.errorMessage else null
        // Обновляем только macrosError, если он изменился
        if (state.macrosError != macrosError) {
            viewModel.updateProductCreationState(state.copy(macrosError = macrosError))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Добавление нового продукта",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Название",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                TextField(
                    value = state.name,
                    onValueChange = { name ->
                        val newState = state.copy(name = name)
                        val validationResult = validator.validateName(name)
                        val nameError = if (validationResult is ValidationResult.Invalid) validationResult.errorMessage else null
                        viewModel.updateProductCreationState(newState.copy(nameError = nameError))
                    },
                    isError = state.nameError != null,
                    supportingText = {
                        if (state.nameError != null) {
                            Text(
                                text = state.nameError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Белки (г)",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                TextField(
                    value = state.protein,
                    onValueChange = { protein ->
                        val newState = state.copy(protein = protein)
                        val validationResult = validator.validateFloatValue(protein, "Белки")
                        val proteinError = if (validationResult is ValidationResult.Invalid) validationResult.errorMessage else null
                        viewModel.updateProductCreationState(newState.copy(proteinError = proteinError))
                    },
                    isError = state.proteinError != null,
                    supportingText = {
                        if (state.proteinError != null) {
                            Text(
                                text = state.proteinError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Жиры (г)",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                TextField(
                    value = state.fats,
                    onValueChange = { fats ->
                        val newState = state.copy(fats = fats)
                        val validationResult = validator.validateFloatValue(fats, "Жиры")
                        val fatsError = if (validationResult is ValidationResult.Invalid) validationResult.errorMessage else null
                        viewModel.updateProductCreationState(newState.copy(fatsError = fatsError))
                    },
                    isError = state.fatsError != null,
                    supportingText = {
                        if (state.fatsError != null) {
                            Text(
                                text = state.fatsError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Углеводы (г)",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                TextField(
                    value = state.carbs,
                    onValueChange = { carbs ->
                        val newState = state.copy(carbs = carbs)
                        val validationResult = validator.validateFloatValue(carbs, "Углеводы")
                        val carbsError = if (validationResult is ValidationResult.Invalid) validationResult.errorMessage else null
                        viewModel.updateProductCreationState(newState.copy(carbsError = carbsError))
                    },
                    isError = state.carbsError != null,
                    supportingText = {
                        if (state.carbsError != null) {
                            Text(
                                text = state.carbsError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Калорийность",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                TextField(
                    value = "${"%.2f".format(calories)} ккал",
                    onValueChange = {},
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
            }
            
            // Отображение ошибки БЖУ
            if (state.macrosError != null) {
                Text(
                    text = state.macrosError!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Штрих-код",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Row(
                    modifier = Modifier.weight(2f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = state.barcode,
                        onValueChange = { barcode ->
                            Log.d("ProductCreation", "TextField изменен, новое значение: $barcode")
                            val newState = state.copy(barcode = barcode)
                            viewModel.updateProductCreationState(newState)
                        },
                        singleLine = true,
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
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }

            Button(
                onClick = {
                    // Устанавливаем состояние загрузки немедленно
                    viewModel.setLoading(true)
                    
                    val nameValidation = validator.validateName(state.name)
                    val proteinValidation = validator.validateFloatValue(state.protein, "Белки")
                    val fatsValidation = validator.validateFloatValue(state.fats, "Жиры")
                    val carbsValidation = validator.validateFloatValue(state.carbs, "Углеводы")
                    val macrosValidation = validator.validateMacros(state.protein, state.fats, state.carbs)

                    val updatedState = state.copy(
                        nameError = if (nameValidation is ValidationResult.Invalid) nameValidation.errorMessage else null,
                        proteinError = if (proteinValidation is ValidationResult.Invalid) proteinValidation.errorMessage else null,
                        fatsError = if (fatsValidation is ValidationResult.Invalid) fatsValidation.errorMessage else null,
                        carbsError = if (carbsValidation is ValidationResult.Invalid) carbsValidation.errorMessage else null,
                        macrosError = if (macrosValidation is ValidationResult.Invalid) macrosValidation.errorMessage else null,
                        barcodeError = null // cбрасываем ошибку штрих-кода при валидации
                    )

                    viewModel.updateProductCreationState(updatedState)

                    if (updatedState.nameError == null &&
                        updatedState.proteinError == null && 
                        updatedState.fatsError == null && 
                        updatedState.carbsError == null &&
                        updatedState.macrosError == null) {
                        
                        // Проверяем уникальность названия и сохраняем продукт
                        viewModel.viewModelScope.launch {
                            try {
                                val checkResult = viewModel.checkProductNameExists(state.name)
                                checkResult.fold(
                                    onSuccess = { exists ->
                                        if (exists) {
                                            // Название уже существует - сбрасываем загрузку
                                            viewModel.setLoading(false)
                                            val nameExistsState = updatedState.copy(
                                                nameError = "Продукт с таким названием уже существует"
                                            )
                                            viewModel.updateProductCreationState(nameExistsState)
                                        } else {
                                            // Создаем и сохраняем продукт
                                            val protein = state.protein.toFloatOrNull() ?: 0f
                                            val fats = state.fats.toFloatOrNull() ?: 0f
                                            val carbs = state.carbs.toFloatOrNull() ?: 0f
                                            
                                            val newProduct = Product(
                                                productId = 0, // Временный ID, сервер присвоит свой
                                                name = state.name,
                                                protein = protein,
                                                fats = fats,
                                                carbs = carbs,
                                                calories = calories,
                                                barcode = state.barcode.takeIf { it.isNotBlank() }
                                            )
                                            
                                            val saveResult = viewModel.addNewProduct(newProduct)
                                            saveResult.fold(
                                                onSuccess = { savedProduct ->
                                                    Log.d("ProductCreation", "Продукт успешно сохранен: ${savedProduct.name}")
                                                    viewModel.hideProductCreationScreen()
                                                    navController.popBackStack()
                                                },
                                                onFailure = { error ->
                                                    val errorState = updatedState.copy(
                                                        nameError = "Ошибка сохранения: ${error.message}"
                                                    )
                                                    viewModel.updateProductCreationState(errorState)
                                                }
                                            )
                                        }
                                    },
                                    onFailure = { error ->
                                        // Ошибка проверки названия - сбрасываем загрузку
                                        viewModel.setLoading(false)
                                        val errorState = updatedState.copy(
                                            nameError = "Ошибка проверки названия: ${error.message}"
                                        )
                                        viewModel.updateProductCreationState(errorState)
                                    }
                                )
                            } catch (e: Exception) {
                                // Исключение - сбрасываем загрузку
                                viewModel.setLoading(false)
                                val errorState = updatedState.copy(
                                    nameError = "Ошибка: ${e.message}"
                                )
                                viewModel.updateProductCreationState(errorState)
                            }
                        }
                    } else {
                        // Ошибка валидации - сбрасываем загрузку
                        viewModel.setLoading(false)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Сохранить")
                }
            }
        }
    }
}