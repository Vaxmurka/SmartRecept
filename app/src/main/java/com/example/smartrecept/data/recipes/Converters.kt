package com.example.smartrecept.data.recipes

import androidx.room.TypeConverter
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson

class Converters {
    @TypeConverter
    fun fromList(list: List<String>): String = list.joinToString("|")

    @TypeConverter
    fun toList(data: String): List<String> = if (data.isEmpty()) listOf() else data.split("|")

    // Для List<String?> (более сложно без Gson)
//    @TypeConverter
//    fun fromNullableStringList(list: List<String?>): String {
//        return list.joinToString("|") { it ?: "NULL" }
//    }
//
//    @TypeConverter
//    fun toNullableStringList(value: String): List<String?> {
//        return if (value.isEmpty()) emptyList()
//        else value.split("|").map { if (it == "NULL") null else it }
//    }
}
