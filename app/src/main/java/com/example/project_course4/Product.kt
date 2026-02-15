package com.example.project_course4

import android.os.Parcelable
import com.example.project_course4.local_db.entities.Products
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.parcelize.Parcelize

@Serializable
@Parcelize
data class Product(
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("product_name") val name: String,
    @SerialName("proteins") val protein: Float,
    @SerialName("fats") val fats: Float,
    @SerialName("carbs") val carbs: Float,
    val calories: Float,
    val barcode: Long = 0,
    val isDish: Boolean = false,
    val createdBy: Int? = null
): Parcelable
fun Product.toEntity(isSavedLocally: Boolean, currentUserId: Int): Products {
    return Products(
        productId = this.productId,
        productName = this.name,
        protein = this.protein,
        fat = this.fats,
        carbs = this.carbs,
        barcode = this.barcode,
        isDish = this.isDish,
        createdBy = this.createdBy ?: currentUserId, // если с сервера не пришел ID, ставим текущий
        isSavedLocally = isSavedLocally
    )
}
fun Products.toUiModel(): Product {
    return Product(
        productId = this.productId,
        name = this.productName,
        protein = this.protein,
        fats = this.fat,
        carbs = this.carbs,
        calories = this.calories,
        barcode = this.barcode,
        isDish = this.isDish,
        createdBy = this.createdBy
    )
}