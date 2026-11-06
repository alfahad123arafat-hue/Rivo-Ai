package com.rivo.ai.ui.screens.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivo.ai.data.local.ApiConfigEntity
import com.rivo.ai.data.local.ProviderType
import com.rivo.ai.data.remote.UnifiedModel
import com.rivo.ai.data.repository.ChatRepository
import com.rivo.ai.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiSettingsViewModel @Inject constructor(
    private val repo: ChatRepository
) : ViewModel() {

    val apiConfigs = repo.observeApiConfigs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _models = MutableStateFlow<List<UnifiedModel>>(emptyList())
    val models: StateFlow<List<UnifiedModel>> = _models

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun clearMessages() { _message.value = null; _error.value = null }

    fun createConfig(name: String, provider: String, baseUrl: String, apiKey: String, modelId: String, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            if (name.isBlank() || baseUrl.isBlank()) { _error.value = "Name and Base URL required"; return@launch }
            if (!isValidUrl(baseUrl)) { _error.value = "Invalid URL: must start with https://"; return@launch }
            val id = repo.createApiConfig(name, provider, baseUrl, apiKey, modelId)
            _message.value = "Configuration saved"
            onDone(id)
        }
    }

    fun updateConfig(id: Long, name: String, provider: String, baseUrl: String, apiKey: String?, modelId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            if (!isValidUrl(baseUrl)) { _error.value = "Invalid URL"; return@launch }
            repo.updateApiConfig(id, name, provider, baseUrl, apiKey, modelId)
            _message.value = "Updated"
            onDone()
        }
    }

    fun deleteConfig(id: Long) {
        viewModelScope.launch {
            repo.deleteApiConfig(id)
            _message.value = "Deleted"
        }
    }

    fun setActive(id: Long) {
        viewModelScope.launch {
            repo.setActiveApiConfig(id)
            _message.value = "Set as active"
        }
    }

    fun testConnection(config: ApiConfigEntity) {
        viewModelScope.launch {
            _isLoadingModels.value = true
            _error.value = null
            when (val res = repo.testConnection(config)) {
                is Result.Success -> _message.value = res.data
                is Result.Error -> _error.value = res.message
                else -> {}
            }
            _isLoadingModels.value = false
        }
    }

    fun fetchModels(config: ApiConfigEntity) {
        viewModelScope.launch {
            _isLoadingModels.value = true
            _error.value = null
            when (val res = repo.fetchModels(config)) {
                is Result.Success -> {
                    _models.value = res.data
                    if (res.data.isEmpty()) _error.value = "No models returned"
                    else _message.value = "Found ${res.data.size} models"
                }
                is Result.Error -> _error.value = res.message
                else -> {}
            }
            _isLoadingModels.value = false
        }
    }

    fun fetchModelsForNew(provider: String, baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            if (apiKey.isBlank() || baseUrl.isBlank()) { _error.value = "API Key and Base URL required to fetch"; return@launch }
            // Create temp config for fetching
            val temp = ApiConfigEntity(id = -1, name = "temp", providerType = provider, baseUrl = baseUrl, modelId = "")
            // Save temp key to crypto with temp id -1 for fetching
            // Instead we directly try via repo logic: create a fake entity and use crypto manager via repo.getApiKey hack
            // We will call repo.testConnection style but we need apiKey. For fetch we need to bypass storage.
            // We'll do manual fetch via repository method that reads from crypto: so temporarily store
            // Workaround: create real temp entity in DB then fetch then delete
            val tempId = repo.createApiConfig("temp_fetch", provider, baseUrl, apiKey, "")
            val tempConfig = ApiConfigEntity(id = tempId, name = "temp", providerType = provider, baseUrl = baseUrl, modelId = "")
            when (val res = repo.fetchModels(tempConfig)) {
                is Result.Success -> {
                    _models.value = res.data
                    _message.value = "Found ${res.data.size} models"
                }
                is Result.Error -> _error.value = res.message
                else -> {}
            }
            repo.deleteApiConfig(tempId)
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("https://") || url.startsWith("http://")
    }

    fun getApiKey(id: Long): String? = repo.getApiKey(id)
}
