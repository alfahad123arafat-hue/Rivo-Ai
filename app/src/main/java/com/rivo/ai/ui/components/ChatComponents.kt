package com.rivo.ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rivo.ai.ui.theme.*

fun copyToClipboard(context: Context, text: String, label: String = "Copied") {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Rivo", text))
    Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
}

@Composable
fun MarkdownText(text: String, isUser: Boolean) {
    if (isUser) {
        Text(text, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp)
        return
    }
    // Simple markdown rendering: split code blocks
    val parts = remember(text) { splitMarkdown(text) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        parts.forEach { part ->
            when (part) {
                is MdPart.Text -> {
                    Text(
                        part.content,
                        color = DeepText,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
                is MdPart.Code -> {
                    CodeBlock(code = part.content, language = part.language)
                }
            }
        }
    }
}

sealed class MdPart {
    data class Text(val content: String) : MdPart()
    data class Code(val content: String, val language: String?) : MdPart()
}

fun splitMarkdown(input: String): List<MdPart> {
    val result = mutableListOf<MdPart>()
    val regex = Regex("```(\\w*)\\n([\\s\\S]*?)```")
    var lastIndex = 0
    regex.findAll(input).forEach { match ->
        val start = match.range.first
        if (start > lastIndex) {
            val text = input.substring(lastIndex, start).trim()
            if (text.isNotEmpty()) result.add(MdPart.Text(text))
        }
        val lang = match.groupValues[1].ifBlank { null }
        val code = match.groupValues[2]
        result.add(MdPart.Code(code, lang))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < input.length) {
        val tail = input.substring(lastIndex).trim()
        if (tail.isNotEmpty()) result.add(MdPart.Text(tail))
    }
    if (result.isEmpty() && input.isNotBlank()) result.add(MdPart.Text(input))
    return result
}

@Composable
fun CodeBlock(code: String, language: String?) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1B2E))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(language ?: "code", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            TextButton(onClick = { copyToClipboard(context, code, "Code copied") }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy", color = Color.White, fontSize = 12.sp)
            }
        }
        Box(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)) {
            Text(
                code.trimEnd(),
                color = Color(0xFFE8E8FF),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun MessageBubble(
    role: String,
    content: String,
    isError: Boolean,
    onCopy: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    val isUser = role == "user"
    val bubbleColor = if (isUser) PurplePrimary else Color.White
    val shape = if (isUser) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(shape)
                .background(bubbleColor)
                .border(1.dp, if (isUser) Color.Transparent else BorderLight, shape)
                .padding(14.dp)
        ) {
            Column {
                if (isError) {
                    Text("⚠️ Error", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                }
                MarkdownText(content, isUser)
                if (!isUser) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = onCopy,
                            label = { Text("Copy", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = LightLavender)
                        )
                        if (onRetry != null) {
                            AssistChip(onClick = onRetry, label = { Text("Retry", fontSize = 11.sp) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PurplePrimary)
        Text("Rivo is thinking…", color = MutedText, fontSize = 13.sp)
    }
}
