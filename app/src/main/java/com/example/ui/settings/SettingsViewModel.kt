package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PulseBreakApp
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PulseBreakApp
    private val preferencesRepository = app.preferencesRepository
    private val repository = app.repository

    val userSettings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            val current = userSettings.value
            val updated = transform(current)
            preferencesRepository.saveUserSettings(updated)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDynamicColor(enabled)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
}
