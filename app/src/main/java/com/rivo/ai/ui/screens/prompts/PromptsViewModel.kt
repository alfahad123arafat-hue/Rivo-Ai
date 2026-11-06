package com.rivo.ai.ui.screens.prompts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivo.ai.data.local.SystemPromptEntity
import com.rivo.ai.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PromptsViewModel @Inject constructor(private val repo: ChatRepository) : ViewModel() {
    val prompts = repo.observeSystemPrompts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val apiConfigs = repo.observeApiConfigs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _msg = MutableStateFlow<String?>(null)
    val msg: StateFlow<String?> = _msg

    fun create(name: String, text: String, apiConfigId: Long?) {
        viewModelScope.launch {
            if (name.isBlank() || text.isBlank()) { _msg.value = "Name and prompt required"; return@launch }
            repo.createPrompt(name, text, apiConfigId)
            _msg.value = "Created"
        }
    }
    fun update(id: Long, name: String, text: String) {
        viewModelScope.launch { repo.updatePrompt(id, name, text); _msg.value = "Updated" }
    }
    fun delete(id: Long) { viewModelScope.launch { repo.deletePrompt(id); _msg.value = "Deleted" } }
    fun setActive(id: Long) { viewModelScope.launch { repo.setActivePrompt(id); _msg.value = "Set active" } }
    fun toggleEnabled(id: Long, enabled: Boolean) { viewModelScope.launch { repo.togglePromptEnabled(id, enabled) } }
    fun clear() { _msg.value = null }
}
