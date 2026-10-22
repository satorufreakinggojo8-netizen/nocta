package com.nocta.app.ui.screens.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nocta.app.R
import com.nocta.app.domain.model.defaultSuggestedPrompts
import com.nocta.app.ui.components.AIMessageBubble
import com.nocta.app.ui.components.companion.VelaPortrait
import com.nocta.app.ui.theme.NoctaAccent
import com.nocta.app.ui.theme.NoctaAccentMuted
import com.nocta.app.ui.theme.NoctaBackground
import com.nocta.app.ui.theme.NoctaShapes
import com.nocta.app.ui.theme.NoctaSpacing
import com.nocta.app.ui.theme.NoctaSurface
import com.nocta.app.ui.theme.NoctaSurfaceBorder
import com.nocta.app.ui.theme.NoctaTextPrimary
import com.nocta.app.ui.theme.NoctaTextSecondary
import com.nocta.app.ui.theme.NoctaTextTertiary
import com.nocta.app.ui.theme.NoctaTypography

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
                        NoctaBackground,
                        NoctaBackground,
                        NoctaSurface.copy(alpha = 0.35f),
                        NoctaBackground
                    )
                )
            )
    ) {
        CoachHeader(expression = uiState.expression)

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

        if (uiState.messages.isNotEmpty() && !uiState.isSending) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NoctaSpacing.md, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(defaultSuggestedPrompts) { prompt ->
                    Text(
                        text = prompt.label,
                        style = NoctaTypography.labelSmall,
                        color = NoctaTextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(NoctaSurface)
                            .border(1.dp, NoctaSurfaceBorder, RoundedCornerShape(50))
                            .clickable { viewModel.send(prompt.prompt) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        CoachInputBar(
            text = uiState.inputText,
            enabled = !uiState.isSending,
            onTextChanged = viewModel::onInputChanged,
            onSend = { viewModel.send() }
        )
    }
}

/**
 * Header built around the transparent Vela portrait: no circle, border or glow
 * plate — the cutout itself sits on the dark background.
 */
@Composable
private fun CoachHeader(expression: CompanionExpression) {
    Column(modifier = Modifier.fillMaxWidth().background(NoctaBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NoctaSpacing.md, vertical = NoctaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VelaPortrait(height = 62.dp)

            Spacer(Modifier.width(NoctaSpacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Vela",
                        style = NoctaTypography.headlineMedium,
                        color = NoctaTextPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = NoctaAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = when (expression) {
                        CompanionExpression.IDLE -> "Shadow AI coach · ready"
                        CompanionExpression.THINKING -> "Reading your sleep data…"
                        CompanionExpression.RESPONDING -> "Replying"
                    },
                    style = NoctaTypography.labelSmall,
                    color = when (expression) {
                        CompanionExpression.IDLE -> NoctaTextTertiary
                        CompanionExpression.THINKING -> NoctaAccent
                        CompanionExpression.RESPONDING -> NoctaAccent
                    },
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NoctaSurfaceBorder)
        )
    }
}

@Composable
private fun EmptyCoachState(onPromptSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NoctaSpacing.lg, vertical = NoctaSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Full-body cutout, borderless, matching the dashboard companion.
        VelaPortrait(
            portraitRes = R.drawable.vela_companion,
            height = 210.dp
        )

        Spacer(Modifier.height(NoctaSpacing.md))

        Text(
            text = "I am Vela, your shadow sleep coach.",
            style = NoctaTypography.titleMedium,
            color = NoctaTextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(NoctaSpacing.sm))

        Text(
            text = "Ask about your own sleep records, your bedtime plan, or what to " +
                "change first. I only use what Nocta has actually recorded.",
            style = NoctaTypography.bodyMedium,
            color = NoctaTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(NoctaSpacing.lg))

        defaultSuggestedPrompts.forEach { prompt ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(NoctaShapes.medium)
                    .background(NoctaSurface)
                    .border(1.dp, NoctaSurfaceBorder, NoctaShapes.medium)
                    .clickable { onPromptSelected(prompt.prompt) }
                    .padding(horizontal = NoctaSpacing.md, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = NoctaAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(NoctaSpacing.sm))
                Text(
                    text = prompt.label,
                    style = NoctaTypography.bodyMedium,
                    color = NoctaTextPrimary,
                    maxLines = 1
                )
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
    Column(modifier = Modifier.fillMaxWidth().background(NoctaBackground)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NoctaSurfaceBorder)
        )
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
                        "Ask Vela about your sleep…",
                        color = NoctaTextTertiary,
                        style = NoctaTypography.bodyMedium
                    )
                },
                textStyle = NoctaTypography.bodyMedium,
                enabled = enabled,
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (enabled && text.isNotBlank()) onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NoctaSurface,
                    unfocusedContainerColor = NoctaSurface,
                    focusedBorderColor = NoctaAccent.copy(alpha = 0.6f),
                    unfocusedBorderColor = NoctaSurfaceBorder,
                    focusedTextColor = NoctaTextPrimary,
                    unfocusedTextColor = NoctaTextPrimary
                )
            )
            IconButton(
                onClick = onSend,
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled && text.isNotBlank()) NoctaAccent else NoctaAccentMuted
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = "Send",
                    tint = if (enabled && text.isNotBlank()) NoctaBackground else NoctaTextTertiary
                )
            }
        }
    }
}
