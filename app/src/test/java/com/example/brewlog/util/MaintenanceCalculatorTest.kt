package com.example.brewlog.util

import com.example.brewlog.data.EspressoMachine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class MaintenanceCalculatorTest {

    @Test
    fun testRemainingDaysCalculation() {
        val intervalCycles = 140
        val weeklyConsumption = 14 // 2 per day
        val lastDone = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10) // 10 days ago

        val dailyConsumption = weeklyConsumption / 7.0
        val intervalDays = (intervalCycles / dailyConsumption).toInt() // 70 days
        val daysSinceLast = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastDone).toInt()
        val remainingDays = intervalDays - daysSinceLast

        assertEquals(60, remainingDays)
    }

    @Test
    fun testOverdueDaysCalculation() {
        val intervalCycles = 70
        val weeklyConsumption = 7 // 1 per day
        val lastDone = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(80) // 80 days ago

        val dailyConsumption = weeklyConsumption / 7.0
        val intervalDays = (intervalCycles / dailyConsumption).toInt() // 70 days
        val daysSinceLast = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastDone).toInt()
        val remainingDays = intervalDays - daysSinceLast

        assertEquals(-10, remainingDays)
    }

    @Test
    fun testCalculateEffectiveIntervalDays() {
        // Capped by max days
        val capped = MaintenanceCalculator.calculateEffectiveIntervalDays(
            maxDays = 90,
            limitCycles = 300,
            weeklyConsumption = 7 // 1 per day -> 300 days > 90 days max
        )
        assertEquals(90, capped)

        // Driven by cycle limit
        val cycleDriven = MaintenanceCalculator.calculateEffectiveIntervalDays(
            maxDays = 90,
            limitCycles = 140,
            weeklyConsumption = 28 // 4 per day -> 140 / 4 = 35 days
        )
        assertEquals(35, cycleDriven)
    }

    @Test
    fun testUpdateWeeklyConsumptionRecalculatesIntervals() {
        val initialMachine = EspressoMachine(
            weeklyConsumption = 14,
            waterFilterIntervalDays = 90,
            waterFilterLimitCycles = 140, // 70 days
            descaleIntervalDays = 180,
            descaleLimitCycles = 280, // 140 days
            backflushIntervalDays = 30,
            backflushLimitCycles = 70 // 35 days
        )

        // Doubling consumption from 14 to 28 cups/week (4/day)
        val updatedMachine = MaintenanceCalculator.updateWeeklyConsumption(initialMachine, 28)

        val newWaterDays = MaintenanceCalculator.calculateEffectiveIntervalDays(
            updatedMachine.waterFilterIntervalDays,
            updatedMachine.waterFilterLimitCycles,
            updatedMachine.weeklyConsumption
        )
        val newDescaleDays = MaintenanceCalculator.calculateEffectiveIntervalDays(
            updatedMachine.descaleIntervalDays,
            updatedMachine.descaleLimitCycles,
            updatedMachine.weeklyConsumption
        )
        val newBackflushDays = MaintenanceCalculator.calculateEffectiveIntervalDays(
            updatedMachine.backflushIntervalDays,
            updatedMachine.backflushLimitCycles,
            updatedMachine.weeklyConsumption
        )

        assertEquals(35, newWaterDays) // 140 / 4 = 35 days
        assertEquals(70, newDescaleDays) // 280 / 4 = 70 days
        assertEquals(17, newBackflushDays) // 70 / 4 = 17 days
    }
}
