package com.example.project_course4.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    /**
     * Конвертирует mealTime (Long) и mealDate (Long) в строку формата "YYYY-MM-DD hh:mm:ss" для отправки на сервер
     * Конвертирует локальное время в UTC
     */
    fun combineDateTimeForServer(mealTime: Long, mealDate: Long): String {
        // mealDate - начало дня, mealTime - смещение от начала дня
        val fullLocalDateTime = mealDate + mealTime
        
        // Конвертируем локальное время в UTC
        val localCalendar = Calendar.getInstance()
        localCalendar.timeInMillis = fullLocalDateTime
        
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.set(
            localCalendar.get(Calendar.YEAR),
            localCalendar.get(Calendar.MONTH),
            localCalendar.get(Calendar.DAY_OF_MONTH),
            localCalendar.get(Calendar.HOUR_OF_DAY),
            localCalendar.get(Calendar.MINUTE),
            localCalendar.get(Calendar.SECOND)
        )
        utcCalendar.set(Calendar.MILLISECOND, localCalendar.get(Calendar.MILLISECOND))
        
        val utcTime = utcCalendar.timeInMillis
        val result = serverDateFormat.format(Date(utcTime))
        
        Log.d("DateUtils","DateUtils.combineDateTimeForServer: local=$fullLocalDateTime, UTC=$utcTime, result=$result")
        return result
    }

    /**
     * Конвертирует строку формата "YYYY-MM-DD hh:mm:ss" (уже в локальном времени сервера) в Long (миллисекунды).
     * Сервер возвращает время уже сконвертированным в локальную TZ, поэтому парсим как есть.
     */
    fun parseDateTimeFromServer(dateTimeString: String): Long {
        return try {
            // Парсим строку как локальное время (сервер уже конвертировал из UTC в локальную TZ)
            val localFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
            val localTime = localFormat.parse(dateTimeString)?.time ?: 0L
            Log.d("DateUtils", "DateUtils.parseDateTimeFromServer: server=$dateTimeString, Local ms=$localTime")
            localTime
        } catch (e: Exception) {
            Log.d("DateUtils", "DateUtils.parseDateTimeFromServer: Ошибка парсинга даты '$dateTimeString': ${e.message}")
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
        
        Log.d("DateUtils", "DateUtils.splitDateTime: fullDateTime=$fullDateTime, mealDate=$mealDate, mealTime=$mealTime")
        
        return Pair(mealTime, mealDate)
    }

}
