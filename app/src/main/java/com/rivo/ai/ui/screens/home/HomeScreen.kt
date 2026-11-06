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
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                tonalElevation = 2.dp,
                modifier = Modifier.clickable { onModelClick() }
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PurplePrimary))
                    Text(activeModel.take(18).ifBlank { "Select model" }, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DeepText, maxLines = 1)
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Top subtle badges like reference (Get Pro / close)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 1.dp, border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("✦", color = PurplePrimary, fontSize = 12.sp)
                    Text("Get Pro 15%", color = DeepText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = CircleShape, color = Color.White, shadowElevation = 1.dp) {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) { Text("◐", color = MutedText, fontSize = 14.sp) }
                }
                Surface(shape = CircleShape, color = Color.White, shadowElevation = 1.dp) {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) { Text("◑", color = MutedText, fontSize = 14.sp) }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        GlowingOrb(modifier = Modifier.padding(top = 12.dp, bottom = 16.dp), sizeDp = 195)

        Text(
            text = "Hello Rehan",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Light,
            fontSize = 30.sp,
            color = DeepText.copy(alpha = 0.95f),
            letterSpacing = 0.3.sp
        )
        Text(
            text = "How can i help",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            color = DeepText,
            lineHeight = 34.sp
        )
        Text(
            text = "you today",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            color = DeepText,
            lineHeight = 34.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "is simply dummy text of the printing and\ntypesetting industry. Lorem Ipsum",
            color = MutedText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        // Listening state hint like reference: "Speak your thoughts homie"
        if (activeApiName == null) {
            Text("Add an API to start", color = MutedText, fontSize = 13.sp)
        }

        Spacer(Modifier.weight(1f))

        // Suggestion chips like reference
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SuggestionChip(label = "Read me a book", icon = "📖", onClick = { onSuggestionClick("Read me a book") }, modifier = Modifier.weight(1f))
            SuggestionChip(label = "Personal therapy", icon = "✦", onClick = { onSuggestionClick("I need personal therapy advice") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        // Bottom hint row mimicking input placeholder style
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Brush.linearGradient(listOf(PurplePrimary, PinkAccent)))) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("◉", color = Color.White, fontSize = 10.sp)
                    }
                }
                Text("Tap on AI to start speaking...", color = MutedText, fontSize = 12.sp)
            }
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = MutedText)
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun SuggestionChip(label: String, icon: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(icon, fontSize = 13.sp)
            Text(label, fontSize = 11.sp, color = DeepText, lineHeight = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2)
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
                placeholder = { Text("Write down instead...", color = MutedText, fontSize = 13.sp) },
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
