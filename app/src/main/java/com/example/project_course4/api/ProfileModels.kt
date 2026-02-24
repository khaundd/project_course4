package com.example.project_course4.api

import kotlinx.serialization.Serializable

@Serializable
data class ProfileData(
    val height: Float,
    val bodyweight: Float,
    val age: Int,
    val goal: String = "MAINTAIN",
    val gender: String = "MALE"
)

@Serializable
data class ProfileUpdateRequest(
    val height: Float,
    val bodyweight: Float,
    val age: Int,
    val goal: String? = null,
    val gender: String? = null
)

@Serializable
data class ProfileResponse(
    val success: Boolean,
    val message: String? = null,
    val profile: ProfileData? = null
)
