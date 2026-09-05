package com.nocta.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.MessageRole
import com.nocta.app.ui.theme.*

@Composable
fun AIMessageBubble(message: AiMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == MessageRole.USER
    val bubbleColor = if (isUser) NoctaAccent else NoctaSurfaceElevated
    val textColor = if (isUser) NoctaBackground else NoctaTextPrimary

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(NoctaShapes.medium)
                .background(bubbleColor)
                .padding(horizontal = NoctaSpacing.md, vertical = NoctaSpacing.sm)
        ) {
            if (!isUser && message.content.isEmpty() && message.isStreaming) {
                TypingDots()
            } else {
                Text(message.content, style = NoctaTypography.bodyLarge, color = textColor)
            }
        }
    }
}

/** Three-dot pulse shown the instant a request is sent, before the first token arrives. */
@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 150, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                Modifier
                    .size(6.dp)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(50))
                    .background(NoctaTextSecondary)
            )
        }
    }
}
