package com.example.project_course4.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.project_course4.Product
import com.example.project_course4.activities.ui.theme.Project_course4Theme
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContent {
            Project_course4Theme {
                MainView(){
                    lifecycleScope.launch {
                        Log.d("api_test", "Корутина запущена")
                        val products = getProducts()
                        val product1 = products[0].toString()
                        val product2 = products[1].toString()
                        println(product1)
                        Log.d("api_test", "$product1; $product2")
                        // Обработка списка products, например, передача в Composable
                    }
                    Log.d("api_test", "После запуска корутины")
                }
            }
        }

    }
}

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}
suspend fun getProducts(): List<Product> {
    Log.d("api_test", "функция getProducts() запущена")
    try {
        val response = client.get("http://10.0.2.2:5000/products")
        Log.d("api_test", "Запрос отправлен")
        val products = response.body<List<Product>>()
        Log.d("api_test", "Получено продуктов: ${products.size}")
        return products
    } catch (e: Exception) {
        Log.e("api_test", "Ошибка в getProducts: ${e.message}", e)
        return emptyList()
    }
}
//suspend fun getProducts(): List<Product> {
//    Log.d("api_test", "функция getProducts()")
//    return client.get("http://localhost:5000/products").body()
//}

@Composable
fun MainView(onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color.LightGray)
            )
            LazyColumn(
                Modifier
                    .padding(start = 10.dp, end = 5.dp, top = 15.dp, bottom = 25.dp)
            ) {
                item{
                    DishItem("Рис", 10f, 2f, 100f, 458f, 110)
                }
                item{
                    DishItem("Кура", 40f, 8f, 5f, 252f, 140)
                }
                item{
                    DishItem("Кола \"Черноголовка\" без сахара", 0f, 0f, 0f, 0f, 350)
                }
            }
            HorizontalDivider(
                color = Color.LightGray,
                thickness = 1.dp,
                modifier = Modifier
                    .padding(start = 5.dp, end = 5.dp)
            )
            Row(
                Modifier
                    .padding(top = 10.dp, start = 10.dp)
            ){
                Row(){
                    Text("50")
                    Spacer(Modifier.padding(start = 5.dp))
                    Text("10")
                    Spacer(Modifier.padding(start = 5.dp))
                    Text("105")
                }
                Spacer(Modifier.weight(1f))
                Text("710", Modifier.padding(end = 5.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onClick,
            modifier = Modifier
                .padding(bottom = 10.dp, end = 10.dp)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .size(60.dp)
                .align(Alignment.End),
            colors = ButtonColors(
                containerColor = Color.Blue,
                contentColor = Color.White,
                disabledContentColor = Color.Blue,
                disabledContainerColor = Color.White
            )
        ) {
            Text("+")
        }
    }
}

@Composable
fun DishItem(dishName: String, proteins: Float, fats: Float, carbs: Float, calories: Float, weight: Int){
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)){
        Column(){
            Text(text = dishName, fontSize = 15.sp)
            Row(){
                Text(
                    text = proteins.toString(),
                    fontSize = 12.sp
                )
                VerticalDivider(
                    color = Color.Black,
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 2.dp)
                )
                Text(
                    text = fats.toString(),
                    fontSize = 12.sp
                )
                VerticalDivider(
                    color = Color.Black,
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 2.dp)
                )
                Text(
                    text = carbs.toString(),
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End){
            Text(text = "$weight г.", fontSize = 15.sp)
            Text(text = "$calories ккал.", fontSize = 12.sp)
        }
    }
}

//@Composable
//@Preview
//fun MainViewPreview() {
//    MainView()
//}