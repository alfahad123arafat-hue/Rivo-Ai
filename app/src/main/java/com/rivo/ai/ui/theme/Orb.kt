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

    Box(modifier = modifier.size((sizeDp).dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        // outer glow
        Box(
            modifier = Modifier
                .fillMaxSize(0.95f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFDCD6FF), Color(0xFFFFD6E8).copy(alpha = 0.6f), Color.Transparent),
                        radius = 300f
                    )
                )
                .blur(18.dp)
        )
        Canvas(modifier = Modifier.fillMaxSize(0.78f * scale)) {
            val c = size.width / 2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8B7BFF), Color(0xFFC9A8FF), Color(0xFFFFB5D8), Color(0xFFF0E8FF)),
                    center = Offset(c + shift * 2, c - shift),
                    radius = size.width * 0.7f
                ),
                radius = size.width / 2
            )
            // highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.9f), Color.Transparent),
                    center = Offset(c - size.width * 0.18f, c - size.width * 0.22f),
                    radius = size.width * 0.35f
                ),
                radius = size.width / 2
            )
        }
        // inner soft ring
        Box(
            modifier = Modifier
                .fillMaxSize(0.82f)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )
    }
}
