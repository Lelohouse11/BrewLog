package com.example.brewlog.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.brewlog.data.BrewDatabase
import com.example.brewlog.data.EspressoMachine
import com.example.brewlog.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class MaintenanceReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = BrewDatabase.getDatabase(applicationContext)
        val machine = database.brewLogDao().getEspressoMachine().first() ?: return Result.success()

        checkMaintenance("Water Filter Replacement", machine, "water", 101)
        checkMaintenance("Descaling", machine, "descale", 102)
        checkMaintenance("Backflushing", machine, "backflush", 103)

        return Result.success()
    }

    private fun checkMaintenance(
        label: String,
        machine: EspressoMachine,
        type: String,
        notificationId: Int
    ) {
        val lastDone = when(type) {
            "water" -> machine.lastWaterFilterChange
            "descale" -> machine.lastDescaling
            else -> machine.lastBackflushing
        }
        
        val maxDays = when(type) {
            "water" -> machine.waterFilterIntervalDays
            "descale" -> machine.descaleIntervalDays
            else -> machine.backflushIntervalDays
        }
        
        val limitCycles = when(type) {
            "water" -> machine.waterFilterLimitCycles
            "descale" -> machine.descaleLimitCycles
            else -> machine.backflushLimitCycles
        }

        val daysSinceLast = if (lastDone != null) {
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastDone).toInt()
        } else {
            0
        }

        val consumptionIntervalDays = if (machine.weeklyConsumption > 0 && limitCycles > 0) {
            (limitCycles / (machine.weeklyConsumption / 7.0)).toInt()
        } else {
            Int.MAX_VALUE
        }

        val actualIntervalDays = minOf(maxDays, consumptionIntervalDays)
        val remainingDays = actualIntervalDays - daysSinceLast

        if (remainingDays == 1) {
            NotificationHelper.sendNotification(applicationContext, "Maintenance Reminder", "$label is due tomorrow!", notificationId)
        } else if (remainingDays == 0) {
            NotificationHelper.sendNotification(applicationContext, "Maintenance Reminder", "$label is due today!", notificationId + 1000)
        }
    }
}
