package com.example.project_course4.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.util.Patterns

class Validation {
    var login by mutableStateOf("")
    var password by mutableStateOf("")
    var email by mutableStateOf("")
    var height by mutableStateOf("")
    var weight by mutableStateOf("")
    var age by mutableStateOf("")

    var loginError by mutableStateOf("")
    var passwordError by mutableStateOf("")
    var emailError by mutableStateOf("")
    var heightError by mutableStateOf("")
    var weightError by mutableStateOf("")
    var ageError by mutableStateOf("")

    private val loginRegex = Regex("^[a-zA-Zа-яА-Я0-9_.-]*$")

    fun validateLogin() {
        if (login.isNotEmpty()) {
            if (login.length > 32) {
                loginError = "Логин не должен превышать 32 символа"
            } else if (!login.matches(loginRegex)) {
                loginError = "Логин содержит недопустимые символы"
            } else {
                loginError = ""
            }
        } else {
            loginError = ""
        }
    }

    fun validateEmail() {
        if (email.isNotEmpty()) {
            if (email.contains(" ")) {
                emailError = "Почта не должна содержать пробелы"
            } else if (email.length > 60) {
                emailError = "Почта не должна превышать 60 символов"
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailError = "Некорректный формат почты"
            } else {
                emailError = ""
            }
        } else {
            emailError = ""
        }
    }

    fun validateHeight() {
        if (height.isNotEmpty()) {
            if (height.contains(" ")) {
                heightError = "Рост не должен содержать пробелы"
            } else if (height.contains(",")) {
                heightError = "Используйте точку для десятичных дробей"
            } else {
                try {
                    val heightValue = height.toDouble()
                    if (heightValue <= 0) {
                        heightError = "Рост должен быть больше 0"
                    } else {
                        heightError = ""
                    }
                } catch (e: NumberFormatException) {
                    heightError = "Введите корректное число"
                }
            }
        } else {
            heightError = ""
        }
    }

    fun validateWeight() {
        if (weight.isNotEmpty()) {
            if (weight.contains(" ")) {
                weightError = "Вес не должен содержать пробелы"
            } else if (weight.contains(",")) {
                weightError = "Используйте точку для десятичных дробей"
            } else {
                try {
                    val weightValue = weight.toDouble()
                    if (weightValue <= 0) {
                        weightError = "Вес должен быть больше 0"
                    } else {
                        weightError = ""
                    }
                } catch (e: NumberFormatException) {
                    weightError = "Введите корректное число"
                }
            }
        } else {
            weightError = ""
        }
    }

    fun validateAge() {
        if (age.isNotEmpty()) {
            if (age.contains(" ")) {
                ageError = "Возраст не должен содержать пробелы"
            } else {
                try {
                    val ageValue = age.toInt()
                    if (ageValue <= 0 || ageValue > 150) {
                        ageError = "Возраст должен быть от 1 до 150"
                    } else {
                        ageError = ""
                    }
                } catch (e: NumberFormatException) {
                    ageError = "Введите целое число"
                }
            }
        } else {
            ageError = ""
        }
    }

    fun validatePassword(isEmptyValid: Boolean = false) {
        if (password.isEmpty() && !isEmptyValid) {
            passwordError = "Пароль не может быть пустым"
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

    fun isValid(): Boolean {
        validateLogin()
        validatePassword()
        validateEmail()
        validateHeight()
        validateWeight()
        validateAge()
        
        return loginError.isEmpty() && 
               passwordError.isEmpty() && 
               emailError.isEmpty() && 
               heightError.isEmpty() && 
               weightError.isEmpty() && 
               ageError.isEmpty()
    }
}