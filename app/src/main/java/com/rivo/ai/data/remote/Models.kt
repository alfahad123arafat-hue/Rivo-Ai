package com.rivo.ai.data.remote

import com.google.gson.annotations.SerializedName

// OpenAI models
data class OpenAIModelListResponse(
    @SerializedName("data") val data: List<OpenAIModel>?,
    @SerializedName("object") val objectType: String? = null
)

data class OpenAIModel(
    @SerializedName("id") val id: String,
    @SerializedName("object") val objectType: String? = null,
    @SerializedName("created") val created: Long? = null,
    @SerializedName("owned_by") val ownedBy: String? = null
)

data class OpenAIChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<OpenAIChatMessage>,
    @SerializedName("stream") val stream: Boolean = false,
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("max_tokens") val maxTokens: Int? = null
)

data class OpenAIChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class OpenAIChatResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<OpenAIChoice>?,
    @SerializedName("error") val error: OpenAIError?
)

data class OpenAIChoice(
    @SerializedName("message") val message: OpenAIChatMessage?,
    @SerializedName("delta") val delta: OpenAIChatMessage?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class OpenAIError(
    @SerializedName("message") val message: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("code") val code: String?
)

data class OpenAIStreamChunk(
    @SerializedName("choices") val choices: List<OpenAIChoice>?
)

// Gemini models
data class GeminiModelListResponse(
    @SerializedName("models") val models: List<GeminiModel>?
)

data class GeminiModel(
    @SerializedName("name") val name: String,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("supportedGenerationMethods") val supportedMethods: List<String>?
) {
    val modelId: String get() = name.removePrefix("models/")
}

data class GeminiGenerateRequest(
    @SerializedName("contents") val contents: List<GeminiContent>,
    @SerializedName("systemInstruction") val systemInstruction: GeminiContent? = null,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    @SerializedName("role") val role: String? = null,
    @SerializedName("parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text") val text: String
)

data class GeminiGenerationConfig(
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int? = null
)

data class GeminiGenerateResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate>?,
    @SerializedName("error") val error: GeminiError?
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiContent?,
    @SerializedName("finishReason") val finishReason: String?
)

data class GeminiError(
    @SerializedName("message") val message: String?,
    @SerializedName("code") val code: Int?
)

// Unified
data class UnifiedModel(
    val id: String,
    val displayName: String,
    val provider: String
)
