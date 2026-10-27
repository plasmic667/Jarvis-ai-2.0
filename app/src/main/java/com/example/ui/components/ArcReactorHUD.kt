package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCyan

@Composable
fun ArcReactorHUD(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    isListening: Boolean = false,
    isThinking: Boolean = false,
    isSpeaking: Boolean = false,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorTransition")

    // Rotation for outer ring
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isThinking) 2500 else 9000, easing = LinearEasing)
        ),
        label = "outerRotation"
    )

    // Reverse rotation for inner ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isThinking) 2000 else 7000, easing = LinearEasing)
        ),
        label = "innerRotation"
    )

    // Pulsing core scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val coreColor = when {
        isListening -> Color(0xFFFF5252)
        isThinking -> JarvisCyan
        isSpeaking -> Color(0xFF00E676)
        else -> JarvisCyan
    }

    val glowColor = when {
        isListening -> Color(0x66FF5252)
        isThinking -> Color(0x7700F0FF)
        isSpeaking -> Color(0x6600E676)
        else -> Color(0x4400F0FF)
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("arc_reactor_hud")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = this.size.minDimension / 2f

            // 1. Ambient outer glow circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = maxRadius * pulseScale
                ),
                radius = maxRadius * pulseScale,
                center = center
            )

            // 2. Outermost dashed orbital ring
            rotate(outerRotation, pivot = center) {
                drawCircle(
                    color = JarvisCyan.copy(alpha = 0.45f),
                    radius = maxRadius * 0.90f,
                    center = center,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f, 10f, 20f), 0f)
                    )
                )
            }

            // 3. Middle high-tech segmented ring
            rotate(innerRotation, pivot = center) {
                drawCircle(
                    color = JarvisBlue.copy(alpha = 0.6f),
                    radius = maxRadius * 0.72f,
                    center = center,
                    style = Stroke(
                        width = 3.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                )
            }

            // 4. Inner metallic casing ring
            drawCircle(
                color = Color(0xFF1B2A4A),
                radius = maxRadius * 0.52f,
                center = center,
                style = Stroke(width = 4f)
            )

            // 5. Bright inner glow ring
            drawCircle(
                color = coreColor.copy(alpha = 0.8f),
                radius = maxRadius * 0.45f * pulseScale,
                center = center,
                style = Stroke(width = 2f)
            )

            // 6. Central Arc Reactor Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, coreColor, Color.Transparent),
                    center = center,
                    radius = maxRadius * 0.32f * pulseScale
                ),
                radius = maxRadius * 0.32f * pulseScale,
                center = center
            )

            // 7. Micro core spark
            drawCircle(
                color = Color.White,
                radius = maxRadius * 0.12f,
                center = center
            )
        }
    }
}
