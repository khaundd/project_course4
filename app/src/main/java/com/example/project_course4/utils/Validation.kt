package com.example.project_course4.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.util.Patterns

class Validation {
    var login by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordConfirmation by mutableStateOf("")
    var email by mutableStateOf("")
    var height by mutableStateOf("")
    var weight by mutableStateOf("")
    var age by mutableStateOf("")
    var code by mutableStateOf("")

    var loginError by mutableStateOf("")
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
        if (login.isEmpty()) {
            loginError = "Логин не может быть пустым"
        }
        else if (login.length > 32) {
            loginError = "Логин не должен превышать 32 символа"
        }
        else if (!login.matches(loginRegex)) {
            loginError = "Логин содержит недопустимые символы"
        }
        else {
                loginError = ""
        }
    }

    fun validateEmail() {
        if (email.isEmpty()) {
            emailError = "Почта не может быть пустой"
        } else if (email.contains(" ")) {
            emailError = "Почта не должна содержать пробелы"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Некорректный формат почты"
        } else {
            emailError = ""
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
            if (h == null || h <= 0) {
                heightError = "Некорректный рост"
            } else if (h > 300) {
                heightError = "Рост не может быть больше 300 см"
            } else {
                heightError = ""
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
            if (w == null || w <= 0) {
                weightError = "Некорректный вес"
            } else if (w > 635) {
                weightError = "Вес не может быть больше 635 кг"
            } else {
                weightError = ""
            }
        }
    }

    fun validateAge() {
        if (age.isEmpty()) {
            ageError = "Укажите возраст"
        } else {
            val a = age.toIntOrNull()
            if (a == null || a <= 0 || a > 150) {
                ageError = "Возраст от 1 до 150"
            } else {
                ageError = ""
            }
        }
    }

    fun validatePassword(isEmptyValid: Boolean = false) {
        if (password.isEmpty() && !isEmptyValid) {
            passwordError = "Пароль не может быть пустым"
        } else if (password.length < 8) {
            passwordError = "Пароль должен содержать минимум 8 символов"
        } else if (password.length > 32) {
            passwordError = "Пароль не должен превышать 32 символа"
        } else if (password.contains(" ")) {
            passwordError = "Пароль не должен содержать пробелы"
        } else if (password.contains(Regex("[а-яА-Я]"))) {
            passwordError = "Пароль не должен содержать кириллицу"
        } else {
            passwordError = ""
        }
    }
    
    fun validatePasswordConfirmation() {
        if (passwordConfirmation.isEmpty()) {
            passwordConfirmationError = "Пароль не может быть пустым"
        } else if (passwordConfirmation != password) {
            passwordConfirmationError = "Пароли не совпадают"
        } else {
            passwordConfirmationError = ""
        }
    }

    fun validateCode() {
        if (code.isEmpty()) {
            codeError = "Код не может быть пустым"
        } else if (code.length != 6) {
            codeError = "Код должен содержать 6 цифр"
        } else if (!code.all { it.isDigit() }) {
            codeError = "Код должен содержать только цифры"
        } else {
            codeError = ""
        }
    }

    fun isValidForLogin(): Boolean {
        return emailError.isEmpty()&&passwordError.isEmpty()
    }

    fun isValidForRegistration(): Boolean {
        
        return loginError.isEmpty() && 
               passwordError.isEmpty() && 
               passwordConfirmationError.isEmpty() && 
               emailError.isEmpty() && 
               heightError.isEmpty() && 
               weightError.isEmpty() && 
               ageError.isEmpty()
    }
}