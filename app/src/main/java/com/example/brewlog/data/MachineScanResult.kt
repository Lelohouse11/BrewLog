package com.example.brewlog.data

import kotlinx.serialization.Serializable

/**
 * Data model for the structured JSON output from Gemini AI for machine information.
 */
@Serializable
data class MachineScanResult(
    val machineFound: Boolean = false,
    val portafilterDiameter: Double = 0.0,
    val hasIntegratedGrinder: Boolean = false,
    val hasSteamWand: Boolean = false,
    val waterFilterMaxDays: Int = 90,
    val descaleMaxDays: Int = 180,
    val backflushMaxDays: Int = 30,
    val waterFilterLimitCycles: Int = 0,
    val descaleLimitCycles: Int = 0,
    val backflushLimitCycles: Int = 0
)
