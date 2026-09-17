package com.example.brewlog.util

import com.example.brewlog.data.ShotLog
import kotlin.math.max

object ExtractionEngine {

    // --- Threshold Constants ---
    const val TIME_FAST = 23.0f
    const val TIME_SLOW = 32.0f
    const val CHANNELING_TIME_MIN = 27.0f
    const val RATIO_SHORT = 1.8f
    const val RATIO_LONG = 2.2f
    
    const val GRIND_ADJUST_LARGE = 1.5f
    const val GRIND_ADJUST_SMALL = 0.5f
    const val YIELD_ADJUST = 3.0f

    enum class AcidityEval { TOOSOUR, BALANCED, FLAT }
    enum class BitternessEval { UNDER, SWEET, BITTER }
    enum class BodyEval { THIN, OPTIMAL, HEAVY }

    data class ShotMetrics(
        val doseIn: Float,
        val grindSize: Float,
        val timeSec: Float,
        val yieldOut: Float,
        val acidity: AcidityEval,
        val bitterness: BitternessEval,
        val body: BodyEval
    )

    data class DialInRecommendation(
        val diagnosis: String,
        val puckPrepWarning: Boolean,
        val suggestedGrindChange: Float,
        val suggestedYieldChange: Float,
        val recommendedGrindSize: Float,
        val recommendedYieldOut: Float,
        val explanation: String
    )

    fun getRecommendation(metrics: ShotMetrics): DialInRecommendation {
        // Guard against invalid inputs
        if (metrics.doseIn <= 0f || metrics.timeSec <= 0f) {
            return DialInRecommendation(
                diagnosis = "Inconclusive Shot",
                puckPrepWarning = false,
                suggestedGrindChange = 0f,
                suggestedYieldChange = 0f,
                recommendedGrindSize = metrics.grindSize,
                recommendedYieldOut = metrics.yieldOut,
                explanation = "Parameters conflict with sensory feedback. Keep settings and pull a verification shot."
            )
        }

        val ratio = metrics.yieldOut / metrics.doseIn
        
        val diagnosis: String
        var puckPrepWarning = false
        var suggestedGrindChange = 0f
        var suggestedYieldChange = 0f
        val explanation: String

        when {
            // P0: Balanced / Sweet Spot
            metrics.acidity == AcidityEval.BALANCED && 
            metrics.bitterness == BitternessEval.SWEET && 
            metrics.body == BodyEval.OPTIMAL && 
            metrics.timeSec in TIME_FAST..TIME_SLOW -> {
                diagnosis = "Balanced Extraction"
                explanation = "Extraction is dialed in. Keep current parameters."
            }

            // P1: Channeling Detection (Physical Fact + Sensory Hint)
            metrics.timeSec >= CHANNELING_TIME_MIN && 
            (metrics.acidity == AcidityEval.TOOSOUR || (metrics.bitterness == BitternessEval.BITTER && metrics.body == BodyEval.THIN)) -> {
                diagnosis = "Suspected Channeling"
                puckPrepWarning = true
                explanation = "Flow indicates channel formation. Contact time was long (${"%.1f".format(metrics.timeSec)}s), but the shot is still sour/thin. Do not grind finer. Improve puck prep (WDT distribution, level tamp)."
            }

            // P2: Under-Extraction (Physical Fact: Fast Flow)
            metrics.timeSec < TIME_FAST || metrics.acidity == AcidityEval.TOOSOUR || metrics.bitterness == BitternessEval.UNDER -> {
                if (metrics.timeSec < 20f || (metrics.timeSec < TIME_FAST && metrics.acidity == AcidityEval.TOOSOUR)) {
                    diagnosis = "Severe Under-extraction"
                    suggestedGrindChange = -GRIND_ADJUST_LARGE
                    explanation = "Shot was too fast (${"%.1f".format(metrics.timeSec)}s). Grind significantly finer."
                } else {
                    diagnosis = "Mild Under-extraction"
                    suggestedGrindChange = -GRIND_ADJUST_SMALL
                    explanation = "Extraction was slightly fast or acidic. Grind slightly finer."
                }
            }

            // P3: Over-Extraction (Physical Fact: Slow Flow)
            metrics.timeSec > TIME_SLOW || metrics.bitterness == BitternessEval.BITTER || metrics.acidity == AcidityEval.FLAT -> {
                if (metrics.timeSec > 35f || (metrics.timeSec > TIME_SLOW && metrics.bitterness == BitternessEval.BITTER)) {
                    diagnosis = "Severe Over-extraction"
                    suggestedGrindChange = GRIND_ADJUST_LARGE
                    explanation = "Shot was choked/slow (${"%.1f".format(metrics.timeSec)}s). Grind significantly coarser."
                } else {
                    diagnosis = "Mild Over-extraction"
                    suggestedGrindChange = GRIND_ADJUST_SMALL
                    explanation = "Extraction was slightly slow or bitter. Grind slightly coarser."
                }
            }

            // P4: Strength / Concentration Tuning
            metrics.acidity == AcidityEval.BALANCED && metrics.bitterness == BitternessEval.SWEET -> {
                when {
                    metrics.body == BodyEval.HEAVY && ratio < RATIO_SHORT -> {
                        diagnosis = "High Strength / Overly Concentrated"
                        suggestedYieldChange = YIELD_ADJUST
                        explanation = "Taste is balanced but too intense. Increase yield to lengthen ratio."
                    }
                    metrics.body == BodyEval.THIN && ratio > RATIO_LONG -> {
                        diagnosis = "Low Strength / Watery"
                        suggestedYieldChange = -YIELD_ADJUST
                        explanation = "Taste is balanced but watery. Reduce yield to shorten ratio."
                    }
                    else -> {
                        diagnosis = "Balanced Extraction"
                        explanation = "Extraction is dialed in. Keep current parameters."
                    }
                }
            }

            // P5: Fallback
            else -> {
                diagnosis = "Inconclusive Shot"
                explanation = "Parameters conflict with sensory feedback. Keep settings and pull a verification shot."
            }
        }

        return DialInRecommendation(
            diagnosis = diagnosis,
            puckPrepWarning = puckPrepWarning,
            suggestedGrindChange = suggestedGrindChange,
            suggestedYieldChange = suggestedYieldChange,
            recommendedGrindSize = max(0f, metrics.grindSize + suggestedGrindChange),
            recommendedYieldOut = max(metrics.doseIn, metrics.yieldOut + suggestedYieldChange),
            explanation = explanation
        )
    }

    // --- Target Calculation for Dial-In Setup ---
    data class ShotTarget(
        val yieldOut: Float,
        val timeSecRange: ClosedRange<Float>,
        val isFromHistory: Boolean
    )

    fun calculateTarget(
        lastBalancedShot: ShotLog?,
        doseIn: Float
    ): ShotTarget {
        return if (lastBalancedShot != null) {
            ShotTarget(
                yieldOut = lastBalancedShot.yieldOut,
                timeSecRange = (lastBalancedShot.extractionTimeSec - 2f)..(lastBalancedShot.extractionTimeSec + 2f),
                isFromHistory = true
            )
        } else {
            val defaultYield = doseIn * 2f
            ShotTarget(
                yieldOut = defaultYield,
                timeSecRange = 25f..30f,
                isFromHistory = false
            )
        }
    }
}
