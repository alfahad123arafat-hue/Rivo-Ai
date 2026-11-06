package com.rivo.ai.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivo.ai.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val repo: ChatRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val darkKey = booleanPreferencesKey("dark_theme")

    val isDark = dataStore.data.map { it[darkKey] ?: false }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    var message by mutableStateOf<String?>(null)

    fun setDark(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[darkKey] = enabled } }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repo.clearAllHistory()
            message = "Chat history cleared"
            kotlinx.coroutines.delay(2000)
            message = null
        }
    }
}
