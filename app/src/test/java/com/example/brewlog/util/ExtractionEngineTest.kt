package com.example.brewlog.util

import com.example.brewlog.data.ShotLog
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtractionEngineTest {

    @Test
    fun testPerfectShot() {
        val metrics = ExtractionEngine.ShotMetrics(
            doseIn = 18f,
            grindSize = 10f,
            timeSec = 27f,
            yieldOut = 36f,
            acidity = ExtractionEngine.AcidityEval.BALANCED,
            bitterness = ExtractionEngine.BitternessEval.SWEET,
            body = ExtractionEngine.BodyEval.OPTIMAL
        )
        val recommendation = ExtractionEngine.getRecommendation(metrics)
        assertEquals("Balanced Extraction", recommendation.diagnosis)
        assertEquals(0f, recommendation.suggestedGrindChange)
        assertEquals(0f, recommendation.suggestedYieldChange)
        assertEquals(false, recommendation.puckPrepWarning)
    }

    @Test
    fun testSevereUnderExtraction() {
        val metrics = ExtractionEngine.ShotMetrics(
            doseIn = 18f,
            grindSize = 10f,
            timeSec = 18f,
            yieldOut = 36f,
            acidity = ExtractionEngine.AcidityEval.TOOSOUR,
            bitterness = ExtractionEngine.BitternessEval.UNDER,
            body = ExtractionEngine.BodyEval.THIN
        )
        val recommendation = ExtractionEngine.getRecommendation(metrics)
        assertEquals("Severe Under-extraction", recommendation.diagnosis)
        assertEquals(-1.5f, recommendation.suggestedGrindChange)
    }

    @Test
    fun testSevereOverExtraction() {
        val metrics = ExtractionEngine.ShotMetrics(
            doseIn = 18f,
            grindSize = 10f,
            timeSec = 38f,
            yieldOut = 36f,
            acidity = ExtractionEngine.AcidityEval.FLAT,
            bitterness = ExtractionEngine.BitternessEval.BITTER,
            body = ExtractionEngine.BodyEval.HEAVY
        )
        val recommendation = ExtractionEngine.getRecommendation(metrics)
        assertEquals("Severe Over-extraction", recommendation.diagnosis)
        assertEquals(1.5f, recommendation.suggestedGrindChange)
    }

    @Test
    fun testChannelingScenario() {
        val metrics = ExtractionEngine.ShotMetrics(
            doseIn = 18f,
            grindSize = 10f,
            timeSec = 31f,
            yieldOut = 36f,
            acidity = ExtractionEngine.AcidityEval.TOOSOUR,
            bitterness = ExtractionEngine.BitternessEval.SWEET,
            body = ExtractionEngine.BodyEval.OPTIMAL
        )
        val recommendation = ExtractionEngine.getRecommendation(metrics)
        assertEquals("Suspected Channeling", recommendation.diagnosis)
        assertEquals(true, recommendation.puckPrepWarning)
        assertEquals(0f, recommendation.suggestedGrindChange)
    }

    @Test
    fun testHighStrengthTuning() {
        val metrics = ExtractionEngine.ShotMetrics(
            doseIn = 18f,
            grindSize = 10f,
            timeSec = 27f,
            yieldOut = 30f, // Ratio 1.66 < 1.8
            acidity = ExtractionEngine.AcidityEval.BALANCED,
            bitterness = ExtractionEngine.BitternessEval.SWEET,
            body = ExtractionEngine.BodyEval.HEAVY
        )
        val recommendation = ExtractionEngine.getRecommendation(metrics)
        assertEquals("High Strength / Overly Concentrated", recommendation.diagnosis)
        assertEquals(3.0f, recommendation.suggestedYieldChange)
    }

    @Test
    fun testLowStrengthTuning() {
        val metrics = ExtractionEngine.ShotMetrics(
            doseIn = 18f,
            grindSize = 10f,
            timeSec = 27f,
            yieldOut = 42f, // Ratio 2.33 > 2.2
            acidity = ExtractionEngine.AcidityEval.BALANCED,
            bitterness = ExtractionEngine.BitternessEval.SWEET,
            body = ExtractionEngine.BodyEval.THIN
        )
        val recommendation = ExtractionEngine.getRecommendation(metrics)
        assertEquals("Low Strength / Watery", recommendation.diagnosis)
        assertEquals(-3.0f, recommendation.suggestedYieldChange)
    }

    @Test
    fun testEdgeCaseZeroTime() {
        val metrics = ExtractionEngine.ShotMetrics(
            doseIn = 18f,
            grindSize = 10f,
            timeSec = 0f,
            yieldOut = 36f,
            acidity = ExtractionEngine.AcidityEval.BALANCED,
            bitterness = ExtractionEngine.BitternessEval.SWEET,
            body = ExtractionEngine.BodyEval.OPTIMAL
        )
        val recommendation = ExtractionEngine.getRecommendation(metrics)
        assertEquals("Inconclusive Shot", recommendation.diagnosis)
    }

    @Test
    fun testCalculateTargetFromHistory() {
        val historyShot = ShotLog(
            beanId = 1,
            basketType = "double",
            grindSize = 5f,
            doseIn = 18f,
            yieldOut = 37.5f,
            extractionTimeSec = 28.5f,
            acidityEval = 0,
            bitternessEval = 0,
            bodyEval = 0
        )
        val target = ExtractionEngine.calculateTarget(historyShot, 18f)
        assertEquals(37.5f, target.yieldOut)
        assertEquals(26.5f, target.timeSecRange.start)
        assertEquals(30.5f, target.timeSecRange.endInclusive)
        assertEquals(true, target.isFromHistory)
    }
}
