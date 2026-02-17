package com.example.project_course4

sealed class Screen(val route: String){
    object Main: Screen("main")
    object SelectProduct: Screen("selectProduct")
    object ProductCreation: Screen("productCreation")
    object Login: Screen("login")
    object Registration: Screen("registration")
    object Verification: Screen("verification")
    object Profile: Screen("profile")
}