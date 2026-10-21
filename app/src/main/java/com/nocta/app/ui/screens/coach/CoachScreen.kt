package com.nocta.app.ui.screens.coach

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.R
import com.nocta.app.domain.model.defaultSuggestedPrompts
import com.nocta.app.ui.components.AIMessageBubble
import com.nocta.app.ui.theme.*

@Composable
fun CoachScreen(viewModel: CoachViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B0E17),
                        Color(0xFF121829),
                        Color(0xFF080A10)
                    )
                )
            )
    ) {
        // Top Header with Glowing Cinematic Coach Avatar
        CoachHeader(expression = uiState.expression)

        // Chat Conversation or Interactive Empty State
        if (uiState.messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyCoachState(onPromptSelected = { viewModel.send(it) })
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = NoctaSpacing.md, vertical = NoctaSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
            ) {
                items(uiState.messages, key = { it.id + it.role }) { message ->
                    AIMessageBubble(
                        message = message,
                        onRetry = { viewModel.retryLastMessage() }
                    )
                }
            }
        }

        // Quick suggested prompt chips bar when conversation is active
        if (uiState.messages.isNotEmpty() && !uiState.isSending) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NoctaSpacing.md, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(defaultSuggestedPrompts) { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E2640))
                            .border(1.dp, Color(0xFF3B4870), RoundedCornerShape(16.dp))
                            .clickable { viewModel.send(prompt.prompt) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = prompt.label,
                            style = NoctaTypography.bodySmall,
                            color = Color(0xFFC3CEEE)
                        )
                    }
                }
            }
        }

        // Input Field Bar
        CoachInputBar(
            text = uiState.inputText,
            enabled = !uiState.isSending,
            onTextChanged = viewModel::onInputChanged,
            onSend = { viewModel.send() }
        )
    }
}

@Composable
private fun CoachHeader(expression: CompanionExpression) {
    val avatarRes = when (expression) {
        CompanionExpression.IDLE -> R.drawable.coach_avatar_idle
        CompanionExpression.THINKING -> R.drawable.coach_avatar_thinking
        CompanionExpression.RESPONDING -> R.drawable.coach_avatar_responding
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val auraGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Surface(
        color = Color(0xFF0D1220),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NoctaSpacing.md, vertical = NoctaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp)
            ) {
                // Outer Shadow Aura Glow effect
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF8A5CFF).copy(alpha = auraGlowAlpha),
                                    Color(0xFF3B82F6).copy(alpha = auraGlowAlpha * 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Crossfade(
                    targetState = avatarRes,
                    animationSpec = tween(400),
                    label = "avatarCrossfade"
                ) { imageRes ->
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = "Nocta AI Coach",
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFF7C3AED), CircleShape)
                            .faceFocused(HeaderFramings[imageRes] ?: PortraitFraming)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Vela",
                        style = NoctaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = when (expression) {
                        CompanionExpression.IDLE -> "Shadow AI Coach • Ready"
                        CompanionExpression.THINKING -> "Analyzing sleep patterns..."
                        CompanionExpression.RESPONDING -> "Transmitting response..."
                    },
                    style = NoctaTypography.bodySmall,
                    color = when (expression) {
                        CompanionExpression.IDLE -> Color(0xFF9CA3AF)
                        CompanionExpression.THINKING -> Color(0xFFFBBF24)
                        CompanionExpression.RESPONDING -> Color(0xFFA78BFA)
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyCoachState(onPromptSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C3AED).copy(alpha = 0.5f),
                            Color(0xFF1E1B4B).copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.coach_avatar_portrait),
                contentDescription = "Vela Portrait",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFA78BFA), CircleShape)
                    .faceFocused(PortraitFraming)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "I am Vela, your Shadow Sleep Coach.",
            style = NoctaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Ask me anything regarding your sleep logs, circadian rhythm, or recovery targets.",
            style = NoctaTypography.bodyMedium,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(24.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(defaultSuggestedPrompts) { prompt ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A2138)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF6D28D9), Color(0xFF2563EB))
                        )
                    ),
                    modifier = Modifier.clickable { onPromptSelected(prompt.prompt) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = prompt.label,
                            style = NoctaTypography.bodyMedium.copy(fontSize = 13.sp),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachInputBar(
    text: String,
    enabled: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = Color(0xFF0D1220),
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NoctaSpacing.md, vertical = NoctaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = {
                    Text(
                        "Ask Vela about your sleep...",
                        color = Color(0xFF6B7280)
                    )
                },
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (enabled && text.isNotBlank()) onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF161D30),
                    unfocusedContainerColor = Color(0xFF161D30),
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color(0xFF2A3555),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            IconButton(
                onClick = onSend,
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled && text.isNotBlank()) {
                            Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF3B82F6)))
                        } else {
                            Brush.linearGradient(listOf(Color(0xFF1E2640), Color(0xFF1E2640)))
                        }
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = "Send",
                    tint = if (enabled && text.isNotBlank()) Color.White else Color(0xFF6B7280)
                )
            }
        }
    }
}

/**
 * Per-asset framing for the circular coach avatars.
 *
 * The expression artwork is a tall, full-body composition, so a plain
 * centered circular crop cuts the head off entirely. Each asset records where
 * the face sits (focusX / focusY as fractions of the source image) and how
 * much to zoom, so the circular avatar frames a natural head-and-shoulders
 * portrait without stretching or distorting the artwork.
 */
private class AvatarFraming(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val focusX: Float,
    val focusY: Float,
    val zoom: Float
)

private val HeaderFramings = mapOf(
    R.drawable.coach_avatar_idle to
        AvatarFraming(sourceWidth = 736, sourceHeight = 1280, focusX = 0.49f, focusY = 0.129f, zoom = 1.5f),
    R.drawable.coach_avatar_thinking to
        AvatarFraming(sourceWidth = 736, sourceHeight = 1635, focusX = 0.48f, focusY = 0.125f, zoom = 1.8f),
    R.drawable.coach_avatar_responding to
        AvatarFraming(sourceWidth = 736, sourceHeight = 1256, focusX = 0.48f, focusY = 0.223f, zoom = 1.15f)
)

private val PortraitFraming =
    AvatarFraming(sourceWidth = 736, sourceHeight = 736, focusX = 0.48f, focusY = 0.353f, zoom = 1.4f)

/**
 * Slides and zooms the ContentScale.Crop window so the face lands in the
 * middle of the circle. Must be applied *inside* a [androidx.compose.ui.draw.clip]
 * so the circular mask itself stays fixed while only the artwork moves.
 */
private fun Modifier.faceFocused(framing: AvatarFraming): Modifier =
    graphicsLayer {
        // Same scale ContentScale.Crop uses for a square target: width-limited
        // for the tall assets, exact for the square portrait.
        val cropScale = maxOf(
            size.width / framing.sourceWidth,
            size.height / framing.sourceHeight
        )
        val scaledHeight = framing.sourceHeight * cropScale
        val focusXPx = framing.focusX * framing.sourceWidth * cropScale
        val focusYPx = framing.focusY * scaledHeight

        // The crop window starts at the top of the artwork (TopCenter). Zooming
        // pushes part of the head above the window, so shift the crop down to
        // center the face — but never past the artwork's bottom edge, which
        // would expose empty space.
        val windowHeight = size.height / framing.zoom
        val desiredTop = (focusYPx - windowHeight / 2f)
            .coerceIn(0f, (scaledHeight - windowHeight).coerceAtLeast(0f))

        transformOrigin = TransformOrigin(
            focusXPx / size.width.coerceAtLeast(0.01f),
            focusYPx / size.height.coerceAtLeast(0.01f)
        )
        scaleX = framing.zoom
        scaleY = framing.zoom
        translationX = (size.width / 2f) - focusXPx
        translationY = -focusYPx - (desiredTop - focusYPx) * framing.zoom
    }
