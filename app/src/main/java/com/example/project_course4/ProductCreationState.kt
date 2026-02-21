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
    val barcodeError: String? = null,
    val macrosError: String? = null // Ошибка суммы БЖУ
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

    fun validateMacros(protein: String, fats: String, carbs: String): ValidationResult {
        return try {
            val proteinValue = protein.toFloatOrNull() ?: 0f
            val fatsValue = fats.toFloatOrNull() ?: 0f
            val carbsValue = carbs.toFloatOrNull() ?: 0f
            val total = proteinValue + fatsValue + carbsValue
            
            when {
                total > 100f -> ValidationResult.Invalid("Сумма БЖУ не может превышать 100 граммов")
                else -> ValidationResult.Valid
            }
        } catch (e: Exception) {
            ValidationResult.Invalid("Ошибка расчета БЖУ")
        }
    }

    fun calculateCalories(protein: Float, fats: Float, carbs: Float): Float {
        return protein * 4 + fats * 9 + carbs * 4
    }
}
