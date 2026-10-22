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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocta.app.domain.model.AiMessage
import com.nocta.app.domain.model.MessageRole
import com.nocta.app.ui.theme.NoctaAccent
import com.nocta.app.ui.theme.NoctaAccentGradientEnd
import com.nocta.app.ui.theme.NoctaAlert
import com.nocta.app.ui.theme.NoctaBackground
import com.nocta.app.ui.theme.NoctaSurface
import com.nocta.app.ui.theme.NoctaSurfaceBorder
import com.nocta.app.ui.theme.NoctaTextPrimary
import com.nocta.app.ui.theme.NoctaTextSecondary
import com.nocta.app.ui.theme.NoctaTypography

/**
 * Chat bubble in Nocta's own palette — violet for the user, charcoal for Vela —
 * so the Coach screen reads as the same product as the rest of the app.
 */
@Composable
fun AIMessageBubble(
    message: AiMessage,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val isUser = message.role == MessageRole.USER
    val isError = !isUser && message.content.contains("couldn't reach", ignoreCase = true)

    val bubbleColor = when {
        isUser -> NoctaAccentGradientEnd
        isError -> NoctaBackground
        else -> NoctaSurface
    }

    val textColor = when {
        isUser -> NoctaTextPrimary
        isError -> NoctaAlert
        else -> NoctaTextPrimary
    }

    val borderColor = when {
        isUser -> NoctaAccent.copy(alpha = 0.55f)
        isError -> NoctaAlert.copy(alpha = 0.55f)
        else -> NoctaSurfaceBorder
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(bubbleColor)
                .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (!isUser && message.content.isEmpty() && message.isStreaming) {
                TypingDots()
            } else {
                Text(
                    text = message.content,
                    style = NoctaTypography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 21.sp),
                    color = textColor
                )

                if (isError && onRetry != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NoctaAlert.copy(alpha = 0.18f))
                            .clickable { onRetry() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Retry",
                            tint = NoctaAlert,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Retry",
                            style = NoctaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NoctaAlert
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
                    .background(NoctaAccent)
            )
        }
    }
}
