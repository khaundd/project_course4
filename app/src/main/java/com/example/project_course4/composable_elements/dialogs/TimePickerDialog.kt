package com.example.project_course4.composable_elements.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.example.project_course4.composable_elements.pickers.NumberPicker

@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }
    
    // Создаем объект LocalTime только при необходимости
    val selectedTime by remember(selectedHour, selectedMinute) {
        mutableStateOf(initialTime.withHour(selectedHour).withMinute(selectedMinute))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Выберите время",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                // Часы
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NumberPicker(
                        value = selectedHour,
                        onValueChange = { hour ->
                            selectedHour = hour
                        },
                        range = 0..23,
                        label = { String.format("%02d", it) }
                    )
                }

                // Минуты
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NumberPicker(
                        value = selectedMinute,
                        onValueChange = { minute ->
                            selectedMinute = minute
                        },
                        range = 0..59,
                        label = { String.format("%02d", it) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTimeSelected(selectedTime)
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Отмена")
            }
        },

        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}