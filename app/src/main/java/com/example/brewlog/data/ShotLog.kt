package com.example.brewlog.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity(
    tableName = "shot_logs",
    foreignKeys = [
        ForeignKey(
            entity = BrewLog::class,
            parentColumns = ["id"],
            childColumns = ["beanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["beanId"])]
)
@Serializable
data class ShotLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val beanId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val basketType: String, // "single" | "double"
    val grindSize: Float,
    val doseIn: Float,
    val yieldOut: Float,
    val extractionTimeSec: Float,
    // Sensory Delta Evaluations: -1, 0, +1
    val acidityEval: Int,
    val bitternessEval: Int,
    val bodyEval: Int,
    val notes: String = ""
)

data class ShotLogWithBean(
    @Embedded val shotLog: ShotLog,
    val beanName: String
)
