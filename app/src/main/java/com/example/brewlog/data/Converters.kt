package com.example.brewlog.data

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

/**
 * Room TypeConverters for handling non-primitive types using JSON serialization.
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return try {
            Json.decodeFromString<List<String>>(value)
        } catch (_: Exception) {
            // Fallback for legacy comma-separated values in existing database
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}
