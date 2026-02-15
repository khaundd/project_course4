package com.example.project_course4

data class SelectedProduct(
    val product: Product,
    val weight: Int, // вес в граммах
    val mealId: Int, // идентификатор приёма пищи
    val junctionId: Int? = null
)