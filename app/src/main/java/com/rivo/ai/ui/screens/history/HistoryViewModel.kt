package com.rivo.ai.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivo.ai.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(private val repo: ChatRepository) : ViewModel() {
    val conversations = repo.observeConversations().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun delete(id: Long) { viewModelScope.launch { repo.deleteConversation(id) } }
    fun clearAll() { viewModelScope.launch { repo.clearAllHistory() } }
}
