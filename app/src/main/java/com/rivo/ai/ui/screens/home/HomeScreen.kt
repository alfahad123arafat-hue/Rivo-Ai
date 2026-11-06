package com.rivo.ai.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivo.ai.ui.components.*
import com.rivo.ai.ui.theme.*

@Composable
fun HomeScreen(
    vm: HomeViewModel = hiltViewModel(),
    onMenuClick: () -> Unit,
    onModelClick: () -> Unit
) {
    val messages by vm.messages.collectAsState()
    val input by vm.inputText.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    val error by vm.error.collectAsState()
    val activeApi by vm.activeApiConfig.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        containerColor = SoftWhite,
        topBar = {
            TopBar(
                activeModel = activeApi?.modelId ?: "No model",
                activeApiName = activeApi?.name ?: "No API",
                onMenuClick = onMenuClick,
                onNewChat = { vm.newChat() },
                onModelClick = onModelClick
            )
        },
        bottomBar = {
            InputBar(
                text = input,
                isGenerating = isGenerating,
                onTextChange = { vm.setInput(it) },
                onSend = { vm.sendMessage() },
                onStop = { vm.stopGeneration() }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(SoftWhite)) {
            if (messages.isEmpty()) {
                EmptyState(
                    onSuggestionClick = { vm.setInput(it) },
                    activeApiName = activeApi?.name
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            role = msg.role,
                            content = msg.content.ifBlank { "..." },
                            isError = msg.isError,
                            onCopy = { copyToClipboard(context, msg.content, "Copied") },
                            onRetry = if (msg.isError) ({ vm.retryLast() }) else null
                        )
                    }
                    if (isGenerating && messages.lastOrNull()?.content?.isEmpty() == true) {
                        item { TypingIndicator() }
                    }
                }
            }

            error?.let { err ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { vm.clearError() }) { Text("Dismiss") }
                    }
                ) { Text(err, maxLines = 3) }
            }
        }
    }
}

@Composable
fun TopBar(
    activeModel: String,
    activeApiName: String,
    onMenuClick: () -> Unit,
    onNewChat: () -> Unit,
    onModelClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SoftWhite).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onMenuClick, modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).border(1.dp, BorderLight, CircleShape)) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = DeepText, modifier = Modifier.size(18.dp))
            }
            Column {
                Text("Rivo AI", fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = DeepText)
                Text(activeApiName, fontSize = 11.sp, color = MutedText)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Model selector chip
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                tonalElevation = 2.dp,
                modifier = Modifier.clickable { onModelClick() }
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PurplePrimary))
                    Text(activeModel.take(18), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DeepText, maxLines = 1)
                    Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(16.dp), tint = MutedText)
                }
            }
            IconButton(onClick = onNewChat, modifier = Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(PurplePrimary, PinkAccent)))) {
                Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White)
            }
        }
    }
}

@Composable
fun EmptyState(onSuggestionClick: (String) -> Unit, activeApiName: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlowingOrb(modifier = Modifier.padding(bottom = 24.dp), sizeDp = 180)
        Text("Hello, I'm Rivo", fontFamily = FontFamily.Serif, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = DeepText)
        Spacer(Modifier.height(8.dp))
        Text(
            if (activeApiName == null) "Add an API configuration to start chatting" else "How can I help you today?",
            color = MutedText, fontSize = 14.sp
        )
        Spacer(Modifier.height(28.dp))
        val suggestions = listOf("Explain quantum computing", "Write a Python script for API calls", "Help me plan a trip to Kyoto", "Summarize a complex topic")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            suggestions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { s ->
                        SuggestionChip(label = s, onClick = { onSuggestionClick(s) }, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ChatBubble, null, modifier = Modifier.size(16.dp), tint = PurplePrimary)
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, color = DeepText, maxLines = 2)
        }
    }
}

@Composable
fun InputBar(
    text: String,
    isGenerating: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Rivo anything…", color = MutedText, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = PurplePrimary
                ),
                maxLines = 5,
                minLines = 1
            )
            if (isGenerating) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFFFE8EE))
                ) {
                    Icon(Icons.Default.Stop, null, tint = ErrorRed)
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(
                        if (text.isNotBlank()) Brush.linearGradient(listOf(PurplePrimary, PinkAccent)) else Brush.linearGradient(listOf(Color(0xFFE8E8FF), Color(0xFFE8E8FF)))
                    )
                ) {
                    Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
