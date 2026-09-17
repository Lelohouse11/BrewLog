package com.example.brewlog.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
            // Only apply if log HAS a rating, OR if the filter is still at default (1-10)
            val isRatingFilterModified = filter.ratingRange.start > 1f || filter.ratingRange.endInclusive < 10f
            val matchesRating = if (isRatingFilterModified) {
                log.hasRating && log.rating.toFloat() in filter.ratingRange
            } else {
                true // Show everything if filter isn't specifically narrowed
            }

            // 3. Sensory Filters
            // Logic: If user narrowed a range (e.g. sweetness 4-5), only show logs with that profile enabled.
            // If range is default (1-5), don't filter out logs missing the profile.
            
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
            try {
                // If it's a content URI, try to take persistable permission
                if (uri.scheme == "content") {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("BrewViewModel", "Could not take persistable permission: ${e.message}")
            }
            
            val current = espressoMachine.value ?: EspressoMachine()
            brewLogDao.insertEspressoMachine(current.copy(photoUri = uri.toString()))
        }
    }

    /**
     * Export all brew logs to a JSON string.
     */
    suspend fun exportData(): String {
        val logs = _allLogs.first()
        return Json.encodeToString(logs)
    }

    /**
     * Import brew logs from a JSON file URI.
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
                
                if (jsonString != null) {
                    val importedLogs: List<BrewLog> = Json.decodeFromString(jsonString)
                    withContext(Dispatchers.IO) {
                        importedLogs.forEach { log ->
                            // Strip ID to avoid conflicts and let Room generate new ones
                            brewLogDao.insertLog(log.copy(id = 0))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
