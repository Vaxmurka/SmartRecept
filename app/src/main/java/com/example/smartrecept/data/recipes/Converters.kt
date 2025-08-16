package com.example.smartrecept.data.recipes

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromList(list: List<String>): String = list.joinToString("|")

    @TypeConverter
    fun toList(data: String): List<String> = if (data.isEmpty()) listOf() else data.split("|")
}
