package com.example.project_course4.dialogs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProductOptionsDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    if (isVisible) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            modifier = Modifier.padding(8.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Редактировать") },
                onClick = {
                    onDismiss()
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Удалить") },
                onClick = {
                    onDismiss()
                    onDelete()
                }
            )
        }
    }
}