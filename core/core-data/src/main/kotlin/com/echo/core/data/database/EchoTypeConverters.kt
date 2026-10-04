package com.echo.core.data.database

import androidx.room.TypeConverter

class EchoTypeConverters {
    @TypeConverter
    fun fromStringList(value: String?): List<String> =
        value?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    @TypeConverter
    fun toStringList(list: List<String>): String =
        list.joinToString(",")
}
