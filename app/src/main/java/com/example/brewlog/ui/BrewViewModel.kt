package com.example.brewlog.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.brewlog.data.BackupData
import com.example.brewlog.data.BrewDatabase
import com.example.brewlog.data.BrewLog
import com.example.brewlog.data.EspressoMachine
import com.example.brewlog.data.ShotLog
import com.example.brewlog.data.ShotLogWithBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

enum class SortOption { NAME, RATING }

data class FilterState(
    val ratingRange: ClosedFloatingPointRange<Float> = 1f..10f,
    val sweetnessRange: ClosedFloatingPointRange<Float> = 1f..5f,
    val acidityRange: ClosedFloatingPointRange<Float> = 1f..5f,
    val bodyRange: ClosedFloatingPointRange<Float> = 1f..5f,
    val bitternessRange: ClosedFloatingPointRange<Float> = 1f..5f,
    val selectedRoasts: Set<String> = emptySet(),
    val selectedFlavorTags: Set<String> = emptySet()
)

class BrewViewModel(application: Application) : AndroidViewModel(application) {
    private val brewLogDao = BrewDatabase.getDatabase(application).brewLogDao()

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val sortOption = MutableStateFlow(SortOption.NAME)
    val filterState = MutableStateFlow(FilterState())
    val searchQuery = MutableStateFlow("")

    private val _allLogs = brewLogDao.getAllLogs()
    private val _allShotLogs = brewLogDao.getAllShotLogsWithBean()

    val espressoMachine = brewLogDao.getEspressoMachine()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val filteredLogs: StateFlow<List<BrewLog>> = combine(
        _allLogs, sortOption, filterState, searchQuery
    ) { logs, sort, filter, query ->
        logs.filter { log ->
            // 1. Search Query
            val matchesSearch = log.coffeeName.contains(query, ignoreCase = true) ||
                               log.roaster.contains(query, ignoreCase = true)
            
            // 2. Rating Filter
            val isRatingFilterModified = filter.ratingRange.start > 1f || filter.ratingRange.endInclusive < 10f
            val matchesRating = if (isRatingFilterModified) {
                log.hasRating && log.rating.toFloat() in filter.ratingRange
            } else {
                true
            }

            // 3. Sensory Filters
            fun matchesSensory(hasProfile: Boolean, value: Int, range: ClosedFloatingPointRange<Float>): Boolean {
                val isRangeModified = range.start > 1f || range.endInclusive < 5f
                return if (isRangeModified) {
                    hasProfile && value.toFloat() in range
                } else {
                    true
                }
            }

            val matchesSweetness = matchesSensory(log.hasSensoryProfile, log.sweetness, filter.sweetnessRange)
            val matchesAcidity = matchesSensory(log.hasSensoryProfile, log.acidity, filter.acidityRange)
            val matchesBody = matchesSensory(log.hasSensoryProfile, log.body, filter.bodyRange)
            val matchesBitterness = matchesSensory(log.hasSensoryProfile, log.bitterness, filter.bitternessRange)

            // 4. Categorical Filters
            val matchesRoast = filter.selectedRoasts.isEmpty() || filter.selectedRoasts.contains(log.roastLevel)
            val matchesFlavor = filter.selectedFlavorTags.isEmpty() || 
                               (log.hasFlavorTags && log.flavorTags.any { filter.selectedFlavorTags.contains(it) })

            matchesSearch && matchesRating && matchesSweetness && matchesAcidity && 
            matchesBody && matchesBitterness && matchesRoast && matchesFlavor
        }.sortedWith { a, b ->
            when (sort) {
                SortOption.NAME -> a.coffeeName.compareTo(b.coffeeName, ignoreCase = true)
                SortOption.RATING -> b.rating.compareTo(a.rating) // Descending
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allLogs: StateFlow<List<BrewLog>> = _allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allShotLogs: StateFlow<List<ShotLogWithBean>> = _allShotLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addLog(log: BrewLog) {
        viewModelScope.launch {
            brewLogDao.insertLog(log)
        }
    }

    fun deleteLog(log: BrewLog) {
        viewModelScope.launch {
            brewLogDao.deleteLog(log)
        }
    }

    fun updateLog(log: BrewLog) {
        viewModelScope.launch {
            brewLogDao.updateLog(log)
        }
    }

    fun addShotLog(shotLog: ShotLog, updateBean: Boolean = false) {
        viewModelScope.launch {
            brewLogDao.insertShotLog(shotLog)
            if (updateBean) {
                updateBeanSettings(
                    beanId = shotLog.beanId,
                    grindSize = shotLog.grindSize,
                    basketType = shotLog.basketType,
                    dose = shotLog.doseIn.toDouble()
                )
            }
        }
    }

    fun updateBeanFromRecommendation(
        beanId: Int,
        basketType: String,
        recommendedGrind: Float,
        recommendedDose: Double,
        @Suppress("UNUSED_PARAMETER") recommendedYield: Double
    ) {
        viewModelScope.launch {
            updateBeanSettings(
                beanId = beanId,
                grindSize = recommendedGrind,
                basketType = basketType,
                dose = recommendedDose
            )
        }
    }

    private suspend fun updateBeanSettings(
        beanId: Int,
        grindSize: Float,
        basketType: String,
        dose: Double
    ) {
        val bean = _allLogs.first().find { it.id == beanId }
        bean?.let {
            val updatedBean = it.copy(
                grindSize = grindSize,
                singleGrams = if (basketType == "single") dose else it.singleGrams,
                doubleGrams = if (basketType == "double") dose else it.doubleGrams
            )
            brewLogDao.updateLog(updatedBean)
        }
    }

    suspend fun getLatestBalancedShot(beanId: Int, basketType: String): ShotLog? {
        return brewLogDao.getLatestBalancedShot(beanId, basketType)
    }

    fun updateSort(option: SortOption) {
        sortOption.value = option
    }

    fun updateFilter(filter: FilterState) {
        filterState.value = filter
    }

    fun resetFilters() {
        filterState.value = FilterState()
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateEspressoMachine(machine: EspressoMachine) {
        viewModelScope.launch {
            brewLogDao.insertEspressoMachine(machine)
        }
    }

    fun deleteEspressoMachine() {
        viewModelScope.launch {
            brewLogDao.clearEspressoMachine()
        }
    }

    fun saveMachinePhoto(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val savedUriString = withContext(Dispatchers.IO) {
                try {
                    val photoDir = File(context.filesDir, "machine_photos")
                    if (!photoDir.exists()) {
                        photoDir.mkdirs()
                    }
                    val destinationFile = File(photoDir, "machine_photo_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destinationFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    destinationFile.toURI().toString()
                } catch (e: Exception) {
                    Log.e("BrewViewModel", "Error copying photo file: ${e.message}", e)
                    uri.toString()
                }
            }
            
            val current = espressoMachine.value ?: EspressoMachine()
            brewLogDao.insertEspressoMachine(current.copy(photoUri = savedUriString))
        }
    }

    /**
     * Export all app data (BrewLogs, ShotLogs, EspressoMachine) to a JSON string.
     */
    suspend fun exportData(): String = withContext(Dispatchers.IO) {
        val logs = _allLogs.first()
        val shots = brewLogDao.getAllShotLogsRaw()
        val machine = brewLogDao.getEspressoMachineOnce()
        val backup = BackupData(
            version = 1,
            brewLogs = logs,
            shotLogs = shots,
            espressoMachine = machine
        )
        jsonFormat.encodeToString(backup)
    }

    /**
     * Import app data from a JSON file URI.
     * Supports full BackupData objects as well as legacy List<BrewLog> JSON files.
     */
    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val jsonString = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    }
                }

                if (!jsonString.isNullOrBlank()) {
                    val content = jsonString
                    withContext(Dispatchers.IO) {
                        try {
                            val backup = jsonFormat.decodeFromString<BackupData>(content)
                            val oldToNewBeanIds = mutableMapOf<Int, Int>()

                            backup.brewLogs.forEach { log ->
                                val oldId = log.id
                                val newId = brewLogDao.insertLog(log.copy(id = 0)).toInt()
                                if (oldId != 0) {
                                    oldToNewBeanIds[oldId] = newId
                                }
                            }

                            backup.shotLogs.forEach { shot ->
                                val mappedBeanId = oldToNewBeanIds[shot.beanId] ?: shot.beanId
                                brewLogDao.insertShotLog(shot.copy(beanId = mappedBeanId))
                            }

                            backup.espressoMachine?.let { machine ->
                                brewLogDao.insertEspressoMachine(machine)
                            }
                        } catch (_: Exception) {
                            // Fallback for legacy backups containing only List<BrewLog>
                            val legacyLogs: List<BrewLog> = jsonFormat.decodeFromString(content)
                            legacyLogs.forEach { log ->
                                brewLogDao.insertLog(log.copy(id = 0))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BrewViewModel", "Error importing data: ${e.message}", e)
            }
        }
    }
}
