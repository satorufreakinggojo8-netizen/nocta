package com.nocta.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.MessageRole
import com.nocta.app.ui.theme.*

@Composable
fun AIMessageBubble(
    message: AiMessage,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val isUser = message.role == MessageRole.USER
    val isError = !isUser && message.content.contains("couldn't reach", ignoreCase = true)

    val bubbleColor = when {
        isUser -> Color(0xFF7C3AED)
        isError -> Color(0xFF371B1E)
        else -> Color(0xFF161D30)
    }

    val textColor = when {
        isUser -> Color.White
        isError -> Color(0xFFFCA5A5)
        else -> Color(0xFFE5E7EB)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .border(
                    width = 1.dp,
                    color = when {
                        isUser -> Color(0xFFA78BFA)
                        isError -> Color(0xFFEF4444)
                        else -> Color(0xFF2A3555)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (!isUser && message.content.isEmpty() && message.isStreaming) {
                TypingDots()
            } else {
                Text(
                    text = message.content,
                    style = NoctaTypography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp),
                    color = textColor
                )

                if (isError && onRetry != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF7F1D1D))
                            .clickable { onRetry() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Retry",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Retry connection",
                            style = NoctaTypography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
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
                    .background(Color(0xFFA78BFA))
            )
        }
    }
}
