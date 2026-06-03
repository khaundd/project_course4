package com.example.project_course4.composable_elements.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val Green = Color(0xFF4CAF50)
private val GreenLight = Color(0xFFE8F5E9)

/**
 * A calendar dialog that lets the user pick a date range by tapping two dates.
 * The first tap sets the start, the second tap sets the end.
 * If the user taps end < start, the range is automatically swapped.
 * A visual highlight connects the two selected dates.
 */
@Composable
fun DateRangePickerDialog(
    initialFrom: LocalDate? = null,
    initialTo: LocalDate? = null,
    onDismiss: () -> Unit,
    onConfirm: (from: LocalDate, to: LocalDate) -> Unit
) {
    var displayMonth by remember { mutableStateOf(YearMonth.from(initialFrom ?: LocalDate.now())) }
    // Selection state: null = nothing selected, one date = first tap done, two dates = range complete
    var firstTap by remember { mutableStateOf(initialFrom) }
    var secondTap by remember { mutableStateOf(initialTo) }

    // Derived ordered range
    val rangeStart = remember(firstTap, secondTap) {
        if (firstTap != null && secondTap != null) minOf(firstTap!!, secondTap!!)
        else firstTap
    }
    val rangeEnd = remember(firstTap, secondTap) {
        if (firstTap != null && secondTap != null) maxOf(firstTap!!, secondTap!!)
        else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Text(
                    "Выбрать диапазон",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Selection hint
                val hintText = when {
                    firstTap == null -> "Выберите начальную дату"
                    secondTap == null -> "Выберите конечную дату"
                    else -> {
                        val fmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        "${rangeStart!!.format(fmt)} – ${rangeEnd!!.format(fmt)}"
                    }
                }
                Text(hintText, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))

                // Month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущий месяц")
                    }
                    val monthName = displayMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru"))
                        .replaceFirstChar { it.uppercase() }
                    Text(
                        "$monthName ${displayMonth.year}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    IconButton(onClick = { displayMonth = displayMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующий месяц")
                    }
                }

                // Day-of-week headers
                val dayHeaders = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayHeaders.forEach { d ->
                        Text(
                            d,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Calendar grid
                val firstDayOfMonth = displayMonth.atDay(1)
                // Monday = 1, so offset = dayOfWeek.value - 1 (Mon=0, Tue=1, ... Sun=6)
                val startOffset = (firstDayOfMonth.dayOfWeek.value - 1)
                val daysInMonth = displayMonth.lengthOfMonth()
                val totalCells = startOffset + daysInMonth
                val rows = (totalCells + 6) / 7

                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - startOffset + 1
                            if (dayNum < 1 || dayNum > daysInMonth) {
                                Spacer(Modifier.weight(1f).height(40.dp))
                            } else {
                                val date = displayMonth.atDay(dayNum)
                                val isStart = rangeStart == date
                                val isEnd = rangeEnd == date
                                val inRange = rangeStart != null && rangeEnd != null &&
                                        date > rangeStart && date < rangeEnd
                                val isSingleSelected = firstTap == date && secondTap == null

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Range background strip (between start and end)
                                    if (inRange) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(GreenLight)
                                        )
                                    }
                                    // Half-strip on start day (right half)
                                    if (isStart && rangeEnd != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(0.5f)
                                                .align(Alignment.CenterEnd)
                                                .background(GreenLight)
                                        )
                                    }
                                    // Half-strip on end day (left half)
                                    if (isEnd) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(0.5f)
                                                .align(Alignment.CenterStart)
                                                .background(GreenLight)
                                        )
                                    }
                                    // Circle for selected days
                                    val isSelected = isStart || isEnd || isSingleSelected
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> Green
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                when {
                                                    firstTap == null -> firstTap = date
                                                    secondTap == null -> {
                                                        if (date == firstTap) {
                                                            // Tap same day — deselect
                                                            firstTap = null
                                                        } else {
                                                            secondTap = date
                                                        }
                                                    }
                                                    else -> {
                                                        // Reset and start new selection
                                                        firstTap = date
                                                        secondTap = null
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            dayNum.toString(),
                                            fontSize = 14.sp,
                                            color = when {
                                                isSelected -> Color.White
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            firstTap = null
                            secondTap = null
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Сбросить") }

                    Button(
                        onClick = {
                            val s = rangeStart
                            val e = rangeEnd
                            if (s != null && e != null) {
                                onConfirm(s, e)
                            } else if (s != null) {
                                // Single day selected — use as both from and to
                                onConfirm(s, s)
                            }
                        },
                        enabled = firstTap != null,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) { Text("Применить", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}
