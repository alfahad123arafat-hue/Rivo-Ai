package com.rivo.ai.data.repository

import com.google.gson.Gson
import com.rivo.ai.data.local.*
import com.rivo.ai.data.remote.*
import com.rivo.ai.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import okhttp3.ResponseBody
import retrofit2.Response

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

@Singleton
class ChatRepository @Inject constructor(
    private val apiConfigDao: ApiConfigDao,
    private val systemPromptDao: SystemPromptDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val cryptoManager: CryptoManager,
    private val okHttpClient: OkHttpClient,
    private val retrofit: Retrofit,
    private val openAIService: OpenAIService,
    private val geminiService: GeminiService
) {
    private val gson = Gson()

    // API Configs
    fun observeApiConfigs(): Flow<List<ApiConfigEntity>> = apiConfigDao.observeAll()
    suspend fun getActiveApiConfig(): ApiConfigEntity? = apiConfigDao.getActive()
    suspend fun getApiConfigById(id: Long): ApiConfigEntity? = apiConfigDao.getById(id)
    suspend fun getAllApiConfigs(): List<ApiConfigEntity> = apiConfigDao.getAll()
    fun observeActiveApiConfig(): Flow<ApiConfigEntity?> = apiConfigDao.observeActive()
    suspend fun getConversation(id: Long): ConversationEntity? = conversationDao.getById(id)
    suspend fun getAllConversations(): List<ConversationEntity> = conversationDao.getAll()

    suspend fun createApiConfig(name: String, providerType: String, baseUrl: String, apiKey: String, modelId: String): Long {
        val entity = ApiConfigEntity(name = name, providerType = providerType, baseUrl = baseUrl.trimEnd('/'), modelId = modelId)
        val id = apiConfigDao.insert(entity)
        if (apiKey.isNotBlank()) cryptoManager.saveApiKey(id, apiKey)
        return id
    }

    suspend fun updateApiConfig(id: Long, name: String, providerType: String, baseUrl: String, apiKey: String?, modelId: String) {
        val existing = apiConfigDao.getById(id) ?: return
        val updated = existing.copy(name = name, providerType = providerType, baseUrl = baseUrl.trimEnd('/'), modelId = modelId, updatedAt = System.currentTimeMillis())
        apiConfigDao.update(updated)
        if (apiKey != null) {
            if (apiKey.isNotBlank()) cryptoManager.saveApiKey(id, apiKey) else cryptoManager.deleteApiKey(id)
        }
    }

    suspend fun deleteApiConfig(id: Long) {
        apiConfigDao.deleteById(id)
        cryptoManager.deleteApiKey(id)
    }

    suspend fun setActiveApiConfig(id: Long) {
        apiConfigDao.clearActive()
        apiConfigDao.setActive(id)
    }

    fun getApiKey(configId: Long): String? = cryptoManager.getApiKey(configId)

    suspend fun testConnection(config: ApiConfigEntity): Result<String> {
        return try {
            val apiKey = cryptoManager.getApiKey(config.id) ?: return Result.Error("API key not set")
            if (config.baseUrl.isBlank()) return Result.Error("Base URL is empty")
            val provider = config.providerType
            when (provider) {
                ProviderType.GEMINI.name -> {
                    val url = buildGeminiModelsUrl(config.baseUrl)
                    val resp = geminiService.listModels(url, apiKey)
                    if (resp.isSuccessful) Result.Success("Connected! Found ${resp.body()?.models?.size ?: 0} models")
                    else Result.Error(parseError(resp))
                }
                else -> {
                    val url = buildOpenAIModelsUrl(config.baseUrl)
                    val auth = "Bearer $apiKey"
                    val resp = openAIService.listModels(url, auth)
                    if (resp.isSuccessful) Result.Success("Connected! Found ${resp.body()?.data?.size ?: 0} models")
                    else {
                        // try without Bearer for custom
                        val resp2 = openAIService.listModelsCustom(url, auth)
                        if (resp2.isSuccessful) Result.Success("Connected! Found ${resp2.body()?.data?.size ?: 0} models")
                        else Result.Error(parseError(resp))
                    }
                }
            }
        } catch (e: IOException) {
            Result.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Result.Error("Error: ${e.message}")
        }
    }

    suspend fun fetchModels(config: ApiConfigEntity): Result<List<UnifiedModel>> {
        val apiKey = cryptoManager.getApiKey(config.id) ?: return Result.Error("API key missing")
        return fetchModelsDirect(config.providerType, config.baseUrl, apiKey)
    }

    suspend fun fetchModelsDirect(providerType: String, baseUrl: String, apiKey: String): Result<List<UnifiedModel>> {
        return try {
            when (providerType) {
                ProviderType.GEMINI.name -> {
                    val url = buildGeminiModelsUrl(baseUrl)
                    val resp = geminiService.listModels(url, apiKey)
                    if (!resp.isSuccessful) return Result.Error(parseError(resp))
                    val all = resp.body()?.models ?: emptyList()
                    // Only show models that support generateContent
                    val filtered = all.filter { m ->
                        val methods = m.supportedMethods ?: emptyList()
                        methods.isEmpty() || methods.any { it.contains("generateContent", ignoreCase = true) }
                    }
                    val models = (if (filtered.isNotEmpty()) filtered else all).map {
                        UnifiedModel(it.modelId, it.displayName ?: it.modelId, "Gemini")
                    }
                    Result.Success(models)
                }
                else -> {
                    val url = buildOpenAIModelsUrl(baseUrl)
                    val resp = openAIService.listModels(url, "Bearer $apiKey")
                    if (resp.isSuccessful) {
                        val models = resp.body()?.data?.map { UnifiedModel(it.id, it.id, "OpenAI") } ?: emptyList()
                        Result.Success(models)
                    } else {
                        Result.Error(parseError(resp))
                    }
                }
            }
        } catch (e: IOException) {
            Result.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    private fun buildOpenAIModelsUrl(baseUrl: String): String {
        return if (baseUrl.contains("/v1")) "${baseUrl.trimEnd('/')}/models"
        else "${baseUrl.trimEnd('/')}/v1/models"
    }

    private fun buildGeminiModelsUrl(baseUrl: String): String {
        val b = baseUrl.trimEnd('/')
        return if (b.contains("generativelanguage.googleapis.com")) "$b/v1beta/models"
        else "$b/v1beta/models"
    }

    private fun <T> parseError(resp: Response<T>): String {
        return try {
            val errBody = resp.errorBody()?.string()
            if (errBody != null) {
                val json = gson.fromJson(errBody, Map::class.java)
                (json["error"] as? Map<*, *>)?.get("message")?.toString() ?: json["message"]?.toString() ?: "HTTP ${resp.code()}: $errBody".take(300)
            } else "HTTP ${resp.code()}: ${resp.message()}"
        } catch (e: Exception) {
            "HTTP ${resp.code()}: ${resp.message()}"
        }
    }

    // System prompts
    fun observeSystemPrompts(): Flow<List<SystemPromptEntity>> = systemPromptDao.observeAll()
    fun observePromptsForApi(apiConfigId: Long): Flow<List<SystemPromptEntity>> = systemPromptDao.observeForApi(apiConfigId)
    suspend fun getActivePrompt(): SystemPromptEntity? = systemPromptDao.getActive()
    suspend fun createPrompt(name: String, text: String, apiConfigId: Long?): Long {
        val e = SystemPromptEntity(name = name, promptText = text, apiConfigId = apiConfigId)
        return systemPromptDao.insert(e)
    }
    suspend fun updatePrompt(id: Long, name: String, text: String) {
        val e = systemPromptDao.getById(id) ?: return
        systemPromptDao.update(e.copy(name = name, promptText = text))
    }
    suspend fun deletePrompt(id: Long) = systemPromptDao.deleteById(id)
    suspend fun setActivePrompt(id: Long) {
        systemPromptDao.clearActive()
        systemPromptDao.setActive(id)
    }
    suspend fun togglePromptEnabled(id: Long, enabled: Boolean) {
        val e = systemPromptDao.getById(id) ?: return
        systemPromptDao.update(e.copy(isEnabled = enabled))
    }

    // Conversations
    fun observeConversations(): Flow<List<ConversationEntity>> = conversationDao.observeAll()
    suspend fun createConversation(title: String, apiConfigId: Long?, modelId: String?, systemPromptId: Long?): Long {
        val e = ConversationEntity(title = title, apiConfigId = apiConfigId, modelId = modelId, systemPromptId = systemPromptId)
        return conversationDao.insert(e)
    }
    suspend fun deleteConversation(id: Long) {
        messageDao.deleteForConversation(id)
        conversationDao.deleteById(id)
    }
    suspend fun clearAllHistory() {
        val convs = conversationDao.getAll()
        convs.forEach { messageDao.deleteForConversation(it.id) }
        conversationDao.clearAll()
    }
    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> = messageDao.observeForConversation(conversationId)
    suspend fun getMessages(conversationId: Long) = messageDao.getForConversation(conversationId)
    suspend fun addMessage(conversationId: Long, role: String, content: String, isError: Boolean = false): Long {
        val m = MessageEntity(conversationId = conversationId, role = role, content = content, isError = isError)
        val id = messageDao.insert(m)
        conversationDao.touch(conversationId)
        // auto title
        if (role == "user") {
            val conv = conversationDao.getById(conversationId)
            if (conv != null && (conv.title == "New Chat" || conv.title.isBlank())) {
                val newTitle = content.take(40).trim().ifBlank { "New Chat" }
                conversationDao.update(conv.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
            }
        }
        return id
    }
    suspend fun updateMessage(id: Long, content: String) {
        val m = messageDao.getById(id) ?: return
        messageDao.update(m.copy(content = content))
    }

    // Chat streaming
    fun streamChat(
        apiConfig: ApiConfigEntity,
        modelId: String,
        systemPrompt: String?,
        messages: List<MessageEntity>,
        conversationId: Long
    ): Flow<String> = callbackFlow {
        val activeCallHolder = arrayOfNulls<okhttp3.Call>(1)
        try {
            val apiKey = cryptoManager.getApiKey(apiConfig.id)
            if (apiKey.isNullOrBlank()) {
                close(IllegalStateException("API key missing — configure key in API Settings"))
                return@callbackFlow
            }
            when (apiConfig.providerType) {
                ProviderType.GEMINI.name -> {
                    streamGemini(apiConfig, apiKey, modelId, systemPrompt, messages, conversationId)
                }
                else -> {
                    streamOpenAI(apiConfig, apiKey, modelId, systemPrompt, messages) { c -> activeCallHolder[0] = c }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            close(e)
        } catch (e: Exception) {
            // propagate as flow error — HomeViewModel will handle
            close(e)
        }
        awaitClose { activeCallHolder[0]?.cancel() }
    }

    private suspend fun kotlinx.coroutines.channels.ProducerScope<String>.streamOpenAI(
        apiConfig: ApiConfigEntity,
        apiKey: String,
        modelId: String,
        systemPrompt: String?,
        messages: List<MessageEntity>,
        onCallCreated: (okhttp3.Call) -> Unit
    ) {
        val base = apiConfig.baseUrl.trimEnd('/')
        val url = if (base.contains("/v1")) "$base/chat/completions" else "$base/v1/chat/completions"

        val openMessages = mutableListOf<OpenAIChatMessage>()
        if (!systemPrompt.isNullOrBlank()) openMessages.add(OpenAIChatMessage("system", systemPrompt))
        messages.forEach { m ->
            if (m.role != "system") openMessages.add(OpenAIChatMessage(m.role, m.content))
        }

        val requestBody = gson.toJson(OpenAIChatRequest(model = modelId, messages = openMessages, stream = true))

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody(mediaType))
            .build()

        val call = okHttpClient.newCall(request)
        onCallCreated(call)
        val response = withContext(Dispatchers.IO) { call.execute() }

        if (!response.isSuccessful) {
            val err = response.body?.string() ?: response.message
            response.close()
            throw IOException(parseOpenAIErrorString(response.code, err))
        }

        val source = response.body?.source() ?: throw IOException("Empty response")
        try {
            while (!source.exhausted()) {
                ensureActive()
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val chunk = gson.fromJson(data, OpenAIStreamChunk::class.java)
                        val delta = chunk.choices?.firstOrNull()?.delta?.content
                        if (!delta.isNullOrEmpty()) {
                            trySend(delta)
                        }
                        // also handle error in stream
                        if (chunk.choices == null) {
                            val errMap = gson.fromJson(data, Map::class.java)
                            if (errMap.containsKey("error")) {
                                throw IOException((errMap["error"] as? Map<*, *>)?.get("message")?.toString() ?: data)
                            }
                        }
                    } catch (e: IOException) { throw e } catch (e: Exception) {
                        // ignore parse errors for single chunk
                    }
                }
            }
        } finally {
            response.close()
        }
        close()
    }

    private suspend fun kotlinx.coroutines.channels.ProducerScope<String>.streamGemini(
        apiConfig: ApiConfigEntity,
        apiKey: String,
        modelId: String,
        systemPrompt: String?,
        messages: List<MessageEntity>,
        conversationId: Long
    ) {
        // Gemini non-streaming — split into chunks to simulate streaming
        val base = apiConfig.baseUrl.trimEnd('/')
        val cleanBase = if (base.contains("generativelanguage.googleapis.com")) base else "https://generativelanguage.googleapis.com"
        val url = "$cleanBase/v1beta/models/$modelId:generateContent"

        val contents = mutableListOf<GeminiContent>()
        messages.forEach { m ->
            val role = if (m.role == "assistant") "model" else "user"
            contents.add(GeminiContent(role = role, parts = listOf(GeminiPart(m.content))))
        }

        val systemInstr = if (!systemPrompt.isNullOrBlank()) GeminiContent(parts = listOf(GeminiPart(systemPrompt))) else null

        val req = GeminiGenerateRequest(contents = contents, systemInstruction = systemInstr)
        val resp = withContext(Dispatchers.IO) {
            geminiService.generateContent(url, apiKey, req)
        }
        if (!resp.isSuccessful) {
            val err = resp.errorBody()?.string() ?: resp.message()
            throw IOException(parseGeminiErrorString(resp.code(), err))
        }
        val text = resp.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IOException("Empty response from Gemini")
        // simulate streaming by sending in chunks — respects cancellation
        val chunkSize = 12
        var idx = 0
        while (idx < text.length) {
            ensureActive()
            val end = minOf(idx + chunkSize, text.length)
            trySend(text.substring(idx, end))
            kotlinx.coroutines.delay(18)
            idx = end
        }
        close()
    }

    private fun parseOpenAIErrorString(code: Int, body: String): String {
        return try {
            val map = gson.fromJson(body, Map::class.java)
            val err = map["error"] as? Map<*, *>
            err?.get("message")?.toString() ?: "HTTP $code: $body".take(500)
        } catch (e: Exception) {
            "HTTP $code: $body".take(500)
        }
    }

    private fun parseGeminiErrorString(code: Int, body: String): String {
        return try {
            val map = gson.fromJson(body, Map::class.java)
            val err = map["error"] as? Map<*, *>
            err?.get("message")?.toString() ?: "HTTP $code: $body".take(500)
        } catch (e: Exception) {
            "HTTP $code: $body".take(500)
        }
    }

    // Non-stream fallback
    suspend fun sendChatNonStreaming(
        apiConfig: ApiConfigEntity,
        modelId: String,
        systemPrompt: String?,
        messages: List<MessageEntity>
    ): Result<String> {
        return try {
            val apiKey = cryptoManager.getApiKey(apiConfig.id) ?: return Result.Error("API key missing")
            when (apiConfig.providerType) {
                ProviderType.GEMINI.name -> {
                    val base = apiConfig.baseUrl.trimEnd('/')
                    val cleanBase = if (base.contains("generativelanguage.googleapis.com")) base else "https://generativelanguage.googleapis.com"
                    val url = "$cleanBase/v1beta/models/$modelId:generateContent"
                    val contents = messages.filter { it.role != "system" }.map {
                        val role = if (it.role == "assistant") "model" else "user"
                        GeminiContent(role = role, parts = listOf(GeminiPart(it.content)))
                    }
                    val systemInstr = if (!systemPrompt.isNullOrBlank()) GeminiContent(parts = listOf(GeminiPart(systemPrompt))) else null
                    val req = GeminiGenerateRequest(contents = contents, systemInstruction = systemInstr)
                    val resp = geminiService.generateContent(url, apiKey, req)
                    if (!resp.isSuccessful) Result.Error(parseGeminiErrorString(resp.code(), resp.errorBody()?.string() ?: ""))
                    else {
                        val text = resp.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Empty response"
                        Result.Success(text)
                    }
                }
                else -> {
                    val base = apiConfig.baseUrl.trimEnd('/')
                    val url = if (base.contains("/v1")) "$base/chat/completions" else "$base/v1/chat/completions"
                    val openMessages = mutableListOf<OpenAIChatMessage>()
                    if (!systemPrompt.isNullOrBlank()) openMessages.add(OpenAIChatMessage("system", systemPrompt))
                    messages.forEach { openMessages.add(OpenAIChatMessage(it.role, it.content)) }
                    val req = OpenAIChatRequest(model = modelId, messages = openMessages, stream = false)
                    val resp = openAIService.chatCompletions(url, "Bearer $apiKey", request = req)
                    if (!resp.isSuccessful) Result.Error(parseError(resp))
                    else {
                        val text = resp.body()?.choices?.firstOrNull()?.message?.content ?: "Empty response"
                        Result.Success(text)
                    }
                }
            }
        } catch (e: IOException) {
            Result.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
}
