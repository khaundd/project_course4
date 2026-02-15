package com.example.project_course4

data class ProductCreationState(
    val name: String = "",
    val protein: String = "",
    val fats: String = "",
    val carbs: String = "",
    val barcode: String = "",
    val nameError: String? = null,
    val proteinError: String? = null,
    val fatsError: String? = null,
    val carbsError: String? = null,
    val barcodeError: String? = null
)

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errorMessage: String) : ValidationResult()
}

class ProductCreationValidator {
    
    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Invalid("Название не может быть пустым")
            name.length > 75 -> ValidationResult.Invalid("Название не может быть длиннее 75 символов")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateFloatValue(value: String, fieldName: String): ValidationResult {
        return when {
            value.isBlank() -> ValidationResult.Invalid("$fieldName не может быть пустым")
            !value.matches(Regex("^\\d*([\\.]\\d+)?")) -> ValidationResult.Invalid("Некорректное значение $fieldName")
            else -> ValidationResult.Valid
        }
    }

    fun calculateCalories(protein: Float, fats: Float, carbs: Float): Float {
        return protein * 4 + fats * 9 + carbs * 4
    }
}
