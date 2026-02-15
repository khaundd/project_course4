package com.example.project_course4.composable_elements.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: (Int) -> String = { it.toString() },
    modifier: Modifier = Modifier,
    wheelModeEnabled: Boolean = true
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // автоматическая прокрутка к текущему значению при инициализации
    LaunchedEffect(value) {
        if (wheelModeEnabled) {
            val targetIndex = value - range.first
            coroutineScope.launch {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }
    val items = remember(range) { range.toList() }
    
    Box(
        modifier = modifier
            .width(80.dp)
            .height(120.dp)
    ) {


        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items) { number ->
                Text(
                    text = label(number),
                    fontSize = 18.sp,
                    color = if (number == value) Color.Black else Color.Gray,
                    fontWeight = if (number == value) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                    modifier = Modifier
                        .clickable {
                            onValueChange(number)
                            coroutineScope.launch {
                                listState.animateScrollToItem(items.indexOf(number))
                            }
                        }
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
        
        // невидимый элемент для фокусировки
        LaunchedEffect(Unit) {
            listState.scrollToItem(value - range.first)
        }
        
            // эффект для обновления значения при изменении позиции прокрутки
        LaunchedEffect(listState.isScrollInProgress) {
            if (!listState.isScrollInProgress) {
                // небольшая задержка для завершения анимации
                delay(100)
                
                // получаем текущую позицию прокрутки
                val visibleItems = listState.layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    // находим элемент, который ближе всего к центру
                    val centerPosition = 60f
                    var closestItem = visibleItems[0]
                    var minDistance = kotlin.math.abs(visibleItems[0].offset.toFloat() + visibleItems[0].size / 2 - centerPosition)
                    
                    for (item in visibleItems) {
                        val distance = kotlin.math.abs(item.offset.toFloat() + item.size / 2 - centerPosition)
                        if (distance < minDistance) {
                            minDistance = distance
                            closestItem = item
                        }
                    }
                    
                    val newValue = items[closestItem.index]
                    if (newValue != value) {
                        onValueChange(newValue)
                    }
                    
                    // прокручиваем к ближайшему элементу, если еще не на нем
                    if (closestItem.index != listState.firstVisibleItemIndex) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(closestItem.index)
                        }
                    }
                }
            }
        }
    }
}