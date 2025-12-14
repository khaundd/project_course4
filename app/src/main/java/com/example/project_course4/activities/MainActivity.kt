package com.example.project_course4.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.project_course4.composable_elements.NavigationApp
import com.example.project_course4.activities.ui.theme.Project_course4Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Project_course4Theme {
                NavigationApp()
            }
        }
    }
}