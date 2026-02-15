package com.example.project_course4.composable_elements

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.project_course4.ProductCreationValidator
import com.example.project_course4.ValidationResult
import com.example.project_course4.viewmodel.ProductViewModel

@Composable
fun ProductCreationScreen(
    navController: NavController,
    viewModel: ProductViewModel
) {
    val state by viewModel.productCreationState.collectAsState()
    val validator = remember { ProductCreationValidator() }
    var calories by rememberSaveable { mutableStateOf(0f) }

    // обновляем калорийность при изменении БЖУ
    LaunchedEffect(state.protein, state.fats, state.carbs) {
        val protein = state.protein.toFloatOrNull() ?: 0f
        val fats = state.fats.toFloatOrNull() ?: 0f
        val carbs = state.carbs.toFloatOrNull() ?: 0f
        calories = validator.calculateCalories(protein, fats, carbs)
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
                TextField(
                    value = state.barcode,
                    onValueChange = { barcode ->
                        val newState = state.copy(barcode = barcode)
                        viewModel.updateProductCreationState(newState)
                    },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.hideProductCreationScreen() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }

            Button(
                onClick = {
                    val nameValidation = validator.validateName(state.name)
                    val proteinValidation = validator.validateFloatValue(state.protein, "Белки")
                    val fatsValidation = validator.validateFloatValue(state.fats, "Жиры")
                    val carbsValidation = validator.validateFloatValue(state.carbs, "Углеводы")

                    val updatedState = state.copy(
                        nameError = if (nameValidation is ValidationResult.Invalid) nameValidation.errorMessage else null,
                        proteinError = if (proteinValidation is ValidationResult.Invalid) proteinValidation.errorMessage else null,
                        fatsError = if (fatsValidation is ValidationResult.Invalid) fatsValidation.errorMessage else null,
                        carbsError = if (carbsValidation is ValidationResult.Invalid) carbsValidation.errorMessage else null,
                        barcodeError = null // Сбрасываем ошибку штрих-кода при валидации
                    )

                    viewModel.updateProductCreationState(updatedState)

                    if (updatedState.nameError == null &&
                        updatedState.proteinError == null && 
                        updatedState.fatsError == null && 
                        updatedState.carbsError == null) {
                        // здесь будет вызов функции сохранения продукта в БД
                        viewModel.hideProductCreationScreen()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                )
            ) {
                Text("Сохранить")
            }
        }
    }
}