package com.rivo.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProviderType {
    GEMINI, OPENAI, CUSTOM
}

@Entity(tableName = "api_configs")
data class ApiConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val providerType: String,
    val baseUrl: String,
    val modelId: String,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_prompts")
data class SystemPromptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val apiConfigId: Long? = null, // null = global, otherwise tied to specific API
    val name: String,
    val promptText: String,
    val isEnabled: Boolean = true,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val apiConfigId: Long?,
    val modelId: String?,
    val systemPromptId: Long?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String, // user, assistant, system
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val isStreaming: Boolean = false
)

data class ConversationWithMessages(
    val conversation: ConversationEntity,
    val messages: List<MessageEntity>
)
