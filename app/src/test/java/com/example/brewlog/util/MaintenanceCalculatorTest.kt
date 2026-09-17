package com.example.brewlog.util

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
}
