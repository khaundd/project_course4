package com.example.project_course4.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.project_course4.composable_elements.NavigationApp
import com.example.project_course4.activities.ui.theme.Project_course4Theme

class MainActivity : ComponentActivity() {

    private val pendingIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingIntent.value = intent
        enableEdgeToEdge()
        setContent {
            Project_course4Theme {
                NavigationApp(intentState = pendingIntent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingIntent.value = intent
    }
}