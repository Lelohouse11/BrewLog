package com.example.brewlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Data model for a Coffee Brew Log entry.
 * Stores coffee characteristics, blend composition, and independent settings for Single and Double baskets.
 */
@Entity(tableName = "brew_logs")
@Serializable
data class BrewLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coffeeName: String,
    val roaster: String = "",
    val grindSize: Float,
    val roastLevel: String, // "Light", "Medium", "Dark"
    
    // Optional Sections
    val hasRating: Boolean = false,
    val rating: Int = 0, // 1-10 scale
    
    val hasSensoryProfile: Boolean = false,
    val sweetness: Int = 3,
    val acidity: Int = 3,
    val body: Int = 3,
    val bitterness: Int = 3,
    
    val hasFlavorTags: Boolean = false,
    val flavorTags: List<String> = emptyList(),
    
    // Blend Settings
    val hasBlendSettings: Boolean = false,
    val arabicaPercentage: Int = 0,
    val robustaPercentage: Int = 0,
    val excelsaPercentage: Int = 0,
    val libericaPercentage: Int = 0,
    
    // Independent Basket Settings
    val isSingleSelected: Boolean = false,
    val singleGrams: Double = 0.0,
    val isDoubleSelected: Boolean = false,
    val doubleGrams: Double = 0.0,
    
    val notes: String = ""
)
