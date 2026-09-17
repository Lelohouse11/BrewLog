package com.example.brewlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Data model for the user's Espresso Machine.
 */
@Entity(tableName = "espresso_machine")
@Serializable
data class EspressoMachine(
    @PrimaryKey val id: Int = 1, // Single record approach
    val brand: String = "",
    val model: String = "",
    val portafilterDiameter: Double = 0.0,
    val hasIntegratedGrinder: Boolean = false,
    val hasSteamWand: Boolean = false,
    val photoUri: String? = null,
    val photoAlignment: Int = 0, // 0: Center, 1: Top/Start, 2: Bottom/End
    
    val weeklyConsumption: Int = 0, // Average cups per week
    
    // Maintenance Intervals (in days)
    val waterFilterIntervalDays: Int = 90, // Default to 90 days
    val descaleIntervalDays: Int = 180,    // Default to 6 months
    val backflushIntervalDays: Int = 30,    // Default to 1 month
    
    // AI provided limit in cycles (optional, 0 if not provided)
    val waterFilterLimitCycles: Int = 0,
    val descaleLimitCycles: Int = 0,
    val backflushLimitCycles: Int = 0,
    
    // Last Maintenance Dates (stored as Epoch Millis)
    val lastWaterFilterChange: Long? = null,
    val lastDescaling: Long? = null,
    val lastBackflushing: Long? = null
)
