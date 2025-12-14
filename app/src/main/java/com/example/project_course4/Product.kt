package com.example.project_course4

import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.parcelize.Parcelize

@Serializable
@Parcelize
data class Product(
    @SerialName("product_name")
    val name: String,

    @SerialName("proteins")
    val protein: Float,

    val fats: Float,
    val carbs: Float,
    val calories: Float
): Parcelable