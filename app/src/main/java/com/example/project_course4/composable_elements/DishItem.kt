package com.example.project_course4.composable_elements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_course4.R
import com.example.project_course4.dialogs.ProductOptionsDialog
import com.example.project_course4.ui.theme.CarbColor
import com.example.project_course4.ui.theme.FatColor
import com.example.project_course4.ui.theme.ProteinColor
import androidx.compose.foundation.shape.RoundedCornerShape


@Composable
fun DishItem(
    dishName: String,
    proteins: Float,
    fats: Float,
    carbs: Float,
    calories: Float,
    weight: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }
    
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .padding(bottom = 10.dp)
            .clickable(
                onClick = { showOptions = true },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .background(
                color = Color.LightGray.copy(alpha = 0.2f),
                shape = RoundedCornerShape(32.dp)
            )) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = dishName, fontSize = 15.sp)
            Row() {
                Text(
                    text = String.format("%.2f", proteins),
                    fontSize = 12.sp,
                    color = ProteinColor
                )
                VerticalDivider(
                    color = Color.Black,
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 12.dp)
                )
                Text(
                    text = String.format("%.2f", fats),
                    fontSize = 12.sp,
                    color = FatColor
                )
                VerticalDivider(
                    color = Color.Black,
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 12.dp)
                )
                Text(
                    text = String.format("%.2f", carbs),
                    fontSize = 12.sp,
                    color = CarbColor
                )
            }
        }
        Spacer(Modifier.padding(horizontal = 8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "$weight г.", fontSize = 15.sp)
                Spacer(Modifier.padding(start = 8.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_more_vert),
                    contentDescription = "Опции",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { showOptions = true }
                )
            }
            Text(text = "${String.format("%.2f", calories)} ккал.", fontSize = 12.sp)
        }
        
        ProductOptionsDialog(
            isVisible = showOptions,
            onDismiss = { showOptions = false },
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}