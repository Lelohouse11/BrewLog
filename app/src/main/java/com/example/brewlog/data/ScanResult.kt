package com.example.brewlog.data

import kotlinx.serialization.Serializable

/**
 * Data model for the structured JSON output from Gemini AI.
 * Includes optional sensory, flavor, and blend details extracted from labels.
 */
@Serializable
data class ScanResult(
    val coffeeName: String = "",
    val roaster: String = "",
    val roastLevel: String = "", // "Light", "Medium", or "Dark"
    
    // Optional details
    val hasSensoryProfile: Boolean = false,
    val sweetness: Int = 3,
    val acidity: Int = 3,
    val body: Int = 3,
    val bitterness: Int = 3,
    
    val hasFlavorTags: Boolean = false,
    val flavorTags: List<String> = emptyList(),
    
    // Optional Blend Info
    val hasBlendSettings: Boolean = false,
    val arabicaPercentage: Int = 0,
    val robustaPercentage: Int = 0,
    val excelsaPercentage: Int = 0,
    val libericaPercentage: Int = 0
)
