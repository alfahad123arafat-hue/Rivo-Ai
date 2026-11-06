package com.rivo.ai.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlowingOrb(modifier: Modifier = Modifier, sizeDp: Int = 180) {
    val infinite = rememberInfiniteTransition(label = "orb")
    val scale by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale"
    )
    val shift by infinite.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "shift"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(modifier = modifier.size((sizeDp).dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        // outer soft glow
        Box(
            modifier = Modifier
                .fillMaxSize(1f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFDCD6FF).copy(alpha = 0.9f * glowPulse),
                            Color(0xFFFFD6E8).copy(alpha = 0.5f * glowPulse),
                            Color(0xFFEDE8FF).copy(alpha = 0.0f)
                        ),
                        radius = 320f
                    )
                )
                .blur(22.dp)
        )
        // secondary glow
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFB8A6FF).copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        radius = 240f
                    )
                )
                .blur(12.dp)
        )
        Canvas(modifier = Modifier.fillMaxSize(0.72f * scale)) {
            val c = size.width / 2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF8F6FF),
                        Color(0xFFD8CCFF),
                        Color(0xFF8B7BFF),
                        Color(0xFFB794F6),
                        Color(0xFFFFB5D8),
                        Color(0xFFF0E8FF)
                    ),
                    center = Offset(c + shift * 1.5f, c - shift),
                    radius = size.width * 0.75f
                ),
                radius = size.width / 2
            )
            // top highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.92f), Color.Transparent),
                    center = Offset(c - size.width * 0.20f, c - size.width * 0.28f),
                    radius = size.width * 0.30f
                ),
                radius = size.width / 2
            )
            // bottom subtle pink reflection
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFE4F2).copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(c + size.width * 0.15f, c + size.width * 0.22f),
                    radius = size.width * 0.40f
                ),
                radius = size.width / 2
            )
        }
        // glass overlay
        Box(
            modifier = Modifier
                .fillMaxSize(0.76f)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.38f), Color.Transparent, Color.White.copy(alpha = 0.08f))
                    )
                )
        )
    }
}
