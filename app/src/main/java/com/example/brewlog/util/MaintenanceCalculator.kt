package com.example.brewlog.util

import com.example.brewlog.data.EspressoMachine

/**
 * Helper utility for calculating maintenance and care intervals based on machine specs
 * and weekly coffee consumption.
 */
object MaintenanceCalculator {

    /**
     * Calculates the effective maintenance interval in days given max days, limit cycles (cups),
     * and weekly consumption.
     */
    fun calculateEffectiveIntervalDays(
        maxDays: Int,
        limitCycles: Int,
        weeklyConsumption: Int
    ): Int {
        if (weeklyConsumption <= 0) return maxDays
        val dailyConsumption = weeklyConsumption / 7.0
        val consumptionDays = if (limitCycles > 0 && dailyConsumption > 0) {
            (limitCycles / dailyConsumption).toInt()
        } else {
            maxDays
        }
        return minOf(maxDays, consumptionDays).coerceAtLeast(1)
    }

    /**
     * Recalculates an [EspressoMachine] data model with a updated weekly consumption.
     * Guarantees limit cycles are initialized if they were 0, ensuring that changing
     * consumption correctly adjusts maintenance and care intervals proportionately.
     */
    fun updateWeeklyConsumption(
        machine: EspressoMachine,
        newWeeklyConsumption: Int
    ): EspressoMachine {
        val oldWeekly = machine.weeklyConsumption
        val targetWeekly = if (oldWeekly > 0) oldWeekly else if (newWeeklyConsumption > 0) newWeeklyConsumption else 7

        val waterLimit = if (machine.waterFilterLimitCycles > 0) {
            machine.waterFilterLimitCycles
        } else {
            (machine.waterFilterIntervalDays * (targetWeekly / 7.0)).toInt()
        }

        val descaleLimit = if (machine.descaleLimitCycles > 0) {
            machine.descaleLimitCycles
        } else {
            (machine.descaleIntervalDays * (targetWeekly / 7.0)).toInt()
        }

        val backflushLimit = if (machine.backflushLimitCycles > 0) {
            machine.backflushLimitCycles
        } else {
            (machine.backflushIntervalDays * (targetWeekly / 7.0)).toInt()
        }

        return machine.copy(
            weeklyConsumption = newWeeklyConsumption,
            waterFilterLimitCycles = waterLimit,
            descaleLimitCycles = descaleLimit,
            backflushLimitCycles = backflushLimit
        )
    }
}
