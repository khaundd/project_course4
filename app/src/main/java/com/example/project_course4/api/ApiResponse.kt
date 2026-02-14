package com.example.project_course4.api

import kotlinx.serialization.Serializable

// Класс для десериализации ответов API
@Serializable
data class ApiResponse(
    val message: String? = null,
    val error: String? = null,
    val token: String? = null,
    val user_id: Int? = null
)