package com.rivo.ai.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivo.ai.data.local.ApiConfigEntity
import com.rivo.ai.data.local.MessageEntity
import com.rivo.ai.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: ChatRepository
) : ViewModel() {

    val apiConfigs: StateFlow<List<ApiConfigEntity>> = repo.observeApiConfigs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeApiConfig: StateFlow<ApiConfigEntity?> = repo.observeActiveApiConfig().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val conversations = repo.observeConversations().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val prompts = repo.observeSystemPrompts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConversationId = MutableStateFlow<Long?>(null)
    val currentConversationId: StateFlow<Long?> = _currentConversationId

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var streamingJob: Job? = null
    private var observeJob: Job? = null

    fun setInput(t: String) { _inputText.value = t }
    fun clearError() { _error.value = null }

    fun selectConversation(id: Long) {
        _currentConversationId.value = id
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repo.observeMessages(id).collect { _messages.value = it }
        }
    }

    fun newChat() {
        streamingJob?.cancel()
        observeJob?.cancel()
        _currentConversationId.value = null
        _messages.value = emptyList()
        _error.value = null
        _isGenerating.value = false
    }

    suspend fun ensureConversation(): Long {
        _currentConversationId.value?.let { return it }
        val active = repo.getActiveApiConfig()
        val activePrompt = repo.getActivePrompt()
        val id = repo.createConversation(
            title = "New Chat",
            apiConfigId = active?.id,
            modelId = active?.modelId,
            systemPromptId = activePrompt?.id
        )
        _currentConversationId.value = id
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repo.observeMessages(id).collect { _messages.value = it }
        }
        return id
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return
        if (_isGenerating.value) return
        viewModelScope.launch {
            try {
                var active = repo.getActiveApiConfig()
                // Prefer conversation's original API if it exists
                _currentConversationId.value?.let { cid ->
                    repo.getConversation(cid)?.apiConfigId?.let { storedId ->
                        repo.getApiConfigById(storedId)?.let { stored -> active = stored }
                    }
                }
                if (active == null) {
                    _error.value = "No active API configured. Please add one in API Settings."
                    return@launch
                }
                if (active.modelId.isBlank()) {
                    _error.value = "No model selected for active API. Please select a model."
                    return@launch
                }
                val convId = ensureConversation()
                _inputText.value = ""
                repo.addMessage(convId, "user", text)
                val history = repo.getMessages(convId).filterNot { it.isError }
                // Only include enabled prompts; getActivePrompt already respects isEnabled but double-check
                val activePrompt = repo.getActivePrompt()
                val systemPromptText = if (activePrompt?.isEnabled == true) activePrompt.promptText.takeIf { it.isNotBlank() } else null

                _isGenerating.value = true
                _error.value = null

                val assistantMsgId = repo.addMessage(convId, "assistant", "")

                val buffer = StringBuilder()
                streamingJob = launch {
                    try {
                        repo.streamChat(active, active.modelId, systemPromptText, history, convId).collect { chunk ->
                            buffer.append(chunk)
                            repo.updateMessage(assistantMsgId, buffer.toString())
                        }
                        if (buffer.isEmpty()) {
                            repo.updateMessage(assistantMsgId, "(Empty response)")
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        // Stopped by user
                        if (buffer.isNotEmpty()) repo.updateMessage(assistantMsgId, buffer.toString())
                        else repo.updateMessage(assistantMsgId, "Stopped")
                        throw e
                    } catch (e: Exception) {
                        val msg = when {
                            e.message?.contains("API key", true) == true -> "Invalid API key. Check API Settings."
                            e.message?.contains("429", true) == true || e.message?.contains("rate limit", true) == true -> "Rate limit exceeded. Try again later."
                            e.message?.contains("404", true) == true || e.message?.contains("model", true) == true -> "Model not found. Check model selection."
                            e.message?.contains("Network", true) == true -> "Network error. Check connection."
                            else -> e.message ?: "Unknown error"
                        }
                        if (buffer.isNotEmpty()) {
                            repo.updateMessage(assistantMsgId, buffer.toString())
                        } else {
                            repo.updateMessage(assistantMsgId, "Error: $msg")
                        }
                        _error.value = msg
                    } finally {
                        _isGenerating.value = false
                    }
                }
                streamingJob?.join()
                _isGenerating.value = false
            } catch (e: Exception) {
                _isGenerating.value = false
                _error.value = e.message ?: "Failed to send"
            }
        }
    }

    fun stopGeneration() {
        streamingJob?.cancel()
        _isGenerating.value = false
    }

    fun retryLast() {
        val lastUser = _messages.value.lastOrNull { it.role == "user" } ?: return
        _inputText.value = lastUser.content
        sendMessage()
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            repo.deleteConversation(id)
            if (_currentConversationId.value == id) newChat()
        }
    }
}
