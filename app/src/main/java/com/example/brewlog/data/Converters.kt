package com.example.brewlog.data

import androidx.room.TypeConverter

/**
 * Room TypeConverters for handling non-primitive types like Lists.
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
}
