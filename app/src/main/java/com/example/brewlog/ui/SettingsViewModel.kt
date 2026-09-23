package com.example.brewlog.ui

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.brewlog.data.SettingsRepository
import com.example.brewlog.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val gramsStepSize: StateFlow<Float> = repository.gramsStepSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.5f
        )

    val grindStepSize: StateFlow<Float> = repository.grindStepSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.5f
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setGramsStepSize(stepSize: Float) {
        viewModelScope.launch {
            repository.setGramsStepSize(stepSize)
        }
    }

    fun setGrindStepSize(stepSize: Float) {
        viewModelScope.launch {
            repository.setGrindStepSize(stepSize)
        }
    }

    fun setLanguage(languageTag: String) {
        val appLocales = if (languageTag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(appLocales)
    }

    fun getCurrentLanguageTag(): String {
        return AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }
}
