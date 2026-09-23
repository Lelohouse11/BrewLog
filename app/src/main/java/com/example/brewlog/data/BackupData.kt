package com.example.brewlog.data

import kotlinx.serialization.Serializable

/**
 * Data model for exporting and importing complete application backups.
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val brewLogs: List<BrewLog> = emptyList(),
    val shotLogs: List<ShotLog> = emptyList(),
    val espressoMachine: EspressoMachine? = null
)
