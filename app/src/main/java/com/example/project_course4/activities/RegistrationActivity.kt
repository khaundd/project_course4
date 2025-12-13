package com.example.project_course4.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.project_course4.ui.theme.Project_course4Theme

class RegistrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Project_course4Theme {

            }
        }
    }
}

@Composable
fun RegistrationElements(){
    Surface(
        Modifier.fillMaxSize(),
        contentColor = Color.Black,
        color = Color.White
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp)){
            Text("Регистрация", modifier = Modifier.padding(top = 50.dp, bottom = 50.dp))
            Column(){
                Text("Электронная почта", modifier = Modifier.padding(bottom = 5.dp))
                TextField("Email", onValueChange = {})
            }
            Column(){
                Text("Имя пользователя", modifier = Modifier.padding(bottom = 5.dp))
                TextField("Username", onValueChange = {})
            }
            Column(){
                Text("Пароль", modifier = Modifier.padding(bottom = 5.dp))
                TextField("Password", onValueChange = {})
            }
            Button(onClick = {}, modifier = Modifier.padding(top = 25.dp)){
                Text("Зарегистрироваться")
            }
        }
    }
}

@Composable
@Preview
fun RegistrationElementsPreview(){
    RegistrationElements()
}