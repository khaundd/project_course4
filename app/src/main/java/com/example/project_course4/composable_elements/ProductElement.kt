package com.example.project_course4.composable_elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_course4.Product
import com.example.project_course4.ui.theme.CarbColor
import com.example.project_course4.ui.theme.FatColor
import com.example.project_course4.ui.theme.ProteinColor

@Composable
fun ProductElement(
    product: Product,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val backgroundColor = if (isSelected) Color.Green.copy(alpha = 0.3f) else Color.White
    val textColor = Color.Black
    Row(
        modifier = Modifier
            .padding(start = 10.dp, end = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClick = onSelect)
            .background(backgroundColor)
            .border(shape = RoundedCornerShape(15.dp), border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = Color.LightGray
            )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ){
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1.5f)
        ) {
            Text(
                text = product.name,
                fontSize = 15.sp,
                color = textColor,
            )
            Row(){
                Text(
                    text = String.format("%.1f", product.protein),
                    fontSize = 12.sp,
                    color = ProteinColor,
                )
                VerticalDivider(
                    color = Color.Black,
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp)
                )
                Text(
                    text = String.format("%.1f", product.fats),
                    fontSize = 12.sp,
                    color = FatColor,
                )
                VerticalDivider(
                    color = Color.Black,
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp)
                )
                Text(
                    text = String.format("%.1f", product.carbs),
                    fontSize = 12.sp,
                    color = CarbColor,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Column (
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(end = 5.dp)
        ) {
            Text(
                text = String.format("%.1f", product.calories),
                fontSize = 12.sp,
                color = textColor,
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ProductElementPreview(){
    ProductElement(Product(1,"Филе", 10.56f, 2.34f, 1.78f, 62.5f), false) {}
}