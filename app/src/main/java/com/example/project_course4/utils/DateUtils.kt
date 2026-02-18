package com.example.project_course4.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Конвертирует mealTime (Long) и mealDate (Long) в строку формата "YYYY-MM-DD hh:mm:ss"
     * mealDate - это начало дня в миллисекундах
     * mealTime - это время дня в миллисекундах от начала дня
     */
    fun combineDateTime(mealTime: Long, mealDate: Long): String {
        // mealDate - начало дня, mealTime - смещение от начала дня
        val fullDateTime = mealDate + mealTime
        val result = dateFormat.format(Date(fullDateTime))
        println("DateUtils.combineDateTime: mealTime=$mealTime, mealDate=$mealDate, fullDateTime=$fullDateTime, result=$result")
        return result
    }

    /**
     * Конвертирует строку формата "YYYY-MM-DD hh:mm:ss" в Long (миллисекунды)
     */
    fun parseDateTime(dateTimeString: String): Long {
        return try {
            dateFormat.parse(dateTimeString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Разделяет полную дату-время на mealDate и mealTime для локального хранения
     */
    fun splitDateTime(fullDateTime: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = fullDateTime
        
        // Получаем начало дня (mealDate)
        val startOfDayCalendar = Calendar.getInstance()
        startOfDayCalendar.set(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        startOfDayCalendar.set(Calendar.MILLISECOND, 0)
        
        val mealDate = startOfDayCalendar.timeInMillis
        val mealTime = fullDateTime - mealDate
        
        println("DateUtils.splitDateTime: fullDateTime=$fullDateTime, mealDate=$mealDate, mealTime=$mealTime")
        
        return Pair(mealTime, mealDate)
    }

    /**
     * Получает текущую дату и время в нужном формате
     */
    fun getCurrentDateTimeString(): String {
        return dateFormat.format(Date())
    }
}
