package com.example.project_course4.composable_elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DishItem(dishName: String, proteins: Float, fats: Float, carbs: Float, calories: Float, weight: Int){
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)){
        Column(
            modifier = Modifier.weight(1f)
        ){
            Text(text = dishName, fontSize = 15.sp)
            Row(){
                Text(
                    text = String.format("%.2f", proteins),
                    fontSize = 12.sp
                )
                VerticalDivider(
                    color = Color.Black,
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 2.dp)
                )
                Text(
                    text = String.format("%.2f", fats),
                    fontSize = 12.sp
                )
                VerticalDivider(
                    color = Color.Black,
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 2.dp)
                )
                Text(
                    text = String.format("%.2f", carbs),
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.padding(horizontal = 8.dp))
        Column(horizontalAlignment = Alignment.End){
            Text(text = "$weight г.", fontSize = 15.sp)
            Text(text = "${String.format("%.2f", calories)} ккал.", fontSize = 12.sp)
        }
    }
}