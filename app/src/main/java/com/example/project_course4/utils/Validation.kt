package com.example.project_course4.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.util.Patterns

class Validation {
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordConfirmation by mutableStateOf("")
    var email by mutableStateOf("")
    var height by mutableStateOf("")
    var weight by mutableStateOf("")
    var age by mutableStateOf("")
    var code by mutableStateOf("")

    var usernameError by mutableStateOf("")
    var passwordError by mutableStateOf("")
    var passwordConfirmationError by mutableStateOf("")
    var emailError by mutableStateOf("")
    var heightError by mutableStateOf("")
    var weightError by mutableStateOf("")
    var ageError by mutableStateOf("")
    var codeError by mutableStateOf("")

    // для хранения сообщений для всплывающих уведомлений
    var toastMessage by mutableStateOf<String?>(null)

    fun clearToastMessage() {
        toastMessage = null
    }

    private val loginRegex = Regex("^[a-zA-Zа-яА-Я0-9_.-]*$")

    fun validateLogin() {
        usernameError = if (username.isEmpty()) {
            "Логин не может быть пустым"
        } else if (username.length > 32) {
            "Логин не должен превышать 32 символа"
        } else if (!username.matches(loginRegex)) {
            "Логин содержит недопустимые символы"
        } else {
            ""
        }
    }

    fun validateEmail() {
        emailError = if (email.isEmpty()) {
            "Почта не может быть пустой"
        } else if (email.contains(" ")) {
            "Почта не должна содержать пробелы"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            "Некорректный формат почты"
        } else {
            ""
        }
    }

    fun validateHeight() {
        if (height.isEmpty()) {
            heightError = "Укажите рост"
        } else if (height.contains(",")) {
            heightError = "Используйте точку (.)"
        } else if (height.endsWith(".")) {
            heightError = "Введите число после точки или удалите точку"
        } else {
            // проверка количества знаков после точки только если есть десятичная часть
            if (height.contains(".")) {
                val decimalPart = height.substringAfter(".")
                if (decimalPart.length > 2) {
                    heightError = "Максимум 2 знака после точки"
                    return
                }
            }
            
            val h = height.toFloatOrNull()
            heightError = if (h == null || h <= 0 ) {
                "Некорректный рост"
            } else if (h > 300) {
                "Рост не может быть больше 300 см"
            } else if (h < 80) {
                "Рост не может быть меньше 80 см"
            } else {
                ""
            }
        }
    }

    fun validateWeight() {
        if (weight.isEmpty()) {
            weightError = "Укажите вес"
        } else if (weight.contains(",")) {
            weightError = "Используйте точку (.)"
        } else if (weight.endsWith(".")) {
            weightError = "Введите число после точки или удалите точку"
        } else {
            // проверка количества знаков после точки только если есть десятичная часть
            if (weight.contains(".")) {
                val decimalPart = weight.substringAfter(".")
                if (decimalPart.length > 2) {
                    weightError = "Максимум 2 знака после точки"
                    return
                }
            }
            
            val w = weight.toFloatOrNull()
            weightError = if (w == null || w <= 0) {
                "Некорректный вес"
            } else if (w > 635) {
                "Вес не может быть больше 635 кг"
            } else if (w < 25) {
                "Вес не может быть меньше 25 кг"
            } else {
                ""
            }
        }
    }

    fun validateAge() {
        ageError = if (age.isEmpty()) {
            "Укажите возраст"
        } else {
            val a = age.toIntOrNull()
            if (a == null || a <= 15 || a > 150) {
                "Возраст от 16 до 150"
            } else {
                ""
            }
        }
    }

    fun validatePassword(isEmptyValid: Boolean = false) {
        passwordError = if (password.isEmpty() && !isEmptyValid) {
            "Пароль не может быть пустым"
        } else if (password.length < 8) {
            "Пароль должен содержать минимум 8 символов"
        } else if (password.length > 32) {
            "Пароль не должен превышать 32 символа"
        } else if (password.contains(" ")) {
            "Пароль не должен содержать пробелы"
        } else if (password.contains(Regex("[а-яА-Я]"))) {
            "Пароль не должен содержать кириллицу"
        } else {
            ""
        }
    }
    
    fun validatePasswordConfirmation() {
        passwordConfirmationError = if (passwordConfirmation.isEmpty()) {
            "Пароль не может быть пустым"
        } else if (passwordConfirmation != password) {
            "Пароли не совпадают"
        } else {
            ""
        }
    }

    fun validateCode() {
        codeError = if (code.isEmpty()) {
            "Код не может быть пустым"
        } else if (code.length != 6) {
            "Код должен содержать 6 цифр"
        } else if (!code.all { it.isDigit() }) {
            "Код должен содержать только цифры"
        } else {
            ""
        }
    }

    fun isValidForLogin(): Boolean {
        return emailError.isEmpty()&&passwordError.isEmpty()
    }

    fun isValidForRegistration(): Boolean {
        
        return usernameError.isEmpty() &&
               passwordError.isEmpty() && 
               passwordConfirmationError.isEmpty() && 
               emailError.isEmpty() && 
               heightError.isEmpty() && 
               weightError.isEmpty() && 
               ageError.isEmpty()
    }
}