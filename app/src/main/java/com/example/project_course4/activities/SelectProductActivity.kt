package com.example.project_course4.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.project_course4.Product
import com.example.project_course4.activities.ui.theme.Project_course4Theme
import java.util.ArrayList
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

var productsList: ArrayList<Product>? = ArrayList()

class SelectProductActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Project_course4Theme {
                SelectProduct()
            }
        }
        productsList = intent.getParcelableArrayListExtra<Product>("key", Product::class.java)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectProduct(){
    val selectedProductNames = remember {
        mutableStateOf(setOf<String>())
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Продукты")
                },
                navigationIcon = {
                    Button(
                        onClick = { /* Назад */ },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("X")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                ),
            )
        }
    ) { paddingValues ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(paddingValues)
        ) {
            items(productsList!!.toList()) { product ->
                ProductElement(
                    product,
                    isSelected = selectedProductNames.value.contains(product.name)
                ) {
                    val currentSet = selectedProductNames.value.toMutableSet()
                    if (currentSet.contains(product.name)) {
                        currentSet.remove(product.name)
                    } else {
                        currentSet.add(product.name)
                    }
                    Log.d("api_test", currentSet.toString())
                    selectedProductNames.value = currentSet
                }
            }
        }
    }
}

//@Preview(showBackground = true)
@Composable
fun SelectProductPreview() {
    SelectProduct()
}

@Preview(showBackground = true)
@Composable
fun ProductElementPreview(){
    ProductElement(Product("Филе", 10f, 2f, 1f, 62f), false) {}
}

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
                        text = product.protein.toString(),
                        fontSize = 12.sp,
                        color = textColor,
                    )
                    VerticalDivider(
                        color = Color.Black,
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                    )
                    Text(
                        text = product.fats.toString(),
                        fontSize = 12.sp,
                        color = textColor,
                    )
                    VerticalDivider(
                        color = Color.Black,
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = 2.dp, end = 2.dp)
                    )
                    Text(
                        text = product.carbs.toString(),
                        fontSize = 12.sp,
                        color = textColor,
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
                    text = product.calories.toString(),
                    fontSize = 12.sp,
                    color = textColor,
                )
            }
        }
    }