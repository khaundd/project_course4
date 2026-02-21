package com.example.project_course4.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    private val localDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
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
     * Конвертирует mealTime (Long) и mealDate (Long) в строку формата "YYYY-MM-DD hh:mm:ss" для локального использования
     */
    fun combineDateTimeLocal(mealTime: Long, mealDate: Long): String {
        val fullDateTime = mealDate + mealTime
        val result = localDateFormat.format(Date(fullDateTime))
        Log.d("DateUtils","DateUtils.combineDateTimeLocal: mealTime=$mealTime, mealDate=$mealDate, fullDateTime=$fullDateTime, result=$result")
        return result
    }

    /**
     * Конвертирует строку формата "YYYY-MM-DD hh:mm:ss" из UTC в Long (миллисекунды) локального времени
     */
    fun parseDateTimeFromServer(dateTimeString: String): Long {
        return try {
            // Сначала парсим как UTC
            val utcTime = serverDateFormat.parse(dateTimeString)?.time ?: 0L
            // Затем конвертируем в локальное время
            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utcCalendar.timeInMillis = utcTime
            
            val localCalendar = Calendar.getInstance()
            localCalendar.set(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH),
                utcCalendar.get(Calendar.HOUR_OF_DAY),
                utcCalendar.get(Calendar.MINUTE),
                utcCalendar.get(Calendar.SECOND)
            )
            localCalendar.set(Calendar.MILLISECOND, utcCalendar.get(Calendar.MILLISECOND))
            
            val localTime = localCalendar.timeInMillis
            Log.d("DateUtils", "DateUtils.parseDateTimeFromServer: UTC=$dateTimeString, UTC ms=$utcTime, Local ms=$localTime")
            localTime
        } catch (e: Exception) {
            Log.d("DateUtils", "DateUtils.parseDateTimeFromServer: Ошибка парсинга даты '$dateTimeString': ${e.message}")
            0L
        }
    }

    /**
     * Конвертирует строку формата "YYYY-MM-DD hh:mm:ss" локального времени в Long (миллисекунды)
     */
    fun parseLocalDateTime(dateTimeString: String): Long {
        return try {
            localDateFormat.parse(dateTimeString)?.time ?: 0L
        } catch (e: Exception) {
            Log.d("DateUtils", "DateUtils.parseLocalDateTime: Ошибка парсинга даты '$dateTimeString': ${e.message}")
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

    /**
     * Получает текущую дату и время в UTC для сервера
     */
    fun getCurrentServerDateTimeString(): String {
        return serverDateFormat.format(Date())
    }

    /**
     * Получает текущую дату и время в локальном часовом поясе
     */
    fun getCurrentLocalDateTimeString(): String {
        return localDateFormat.format(Date())
    }
}
