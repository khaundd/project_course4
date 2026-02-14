package com.example.project_course4.local_db

import androidx.room.TypeConverter

class DatabaseConverters {
    @TypeConverter
    fun fromUShort(value: UShort): Int = value.toInt()

    @TypeConverter
    fun toUShort(value: Int): UShort = value.toUShort()

}