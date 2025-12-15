package com.example.project_course4.composable_elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.example.project_course4.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectProductScreen(
    navController: NavController,
    viewModel: ProductViewModel
){
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentSelection by viewModel.currentSelection.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Продукты")
                },
                navigationIcon = {
                    Button(
                        onClick = {
                            viewModel.clearCurrentSelection()
                            navController.popBackStack()
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("X")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveCurrentSelection()
                            navController.popBackStack()
                        },
                        enabled = currentSelection.isNotEmpty(),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("+")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                ),
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(paddingValues)
            ) {
                items(products) { product ->
                    ProductElement(
                        product = product,
                        isSelected = currentSelection.contains(product)
                    ) {
                        viewModel.toggleCurrentSelection(product)
                    }
                }
            }
        }
    }
}