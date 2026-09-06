package com.nocta.app.ui.screens.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
            .background(NoctaBackground)
    ) {
        Text(
            "Vela",
            style = NoctaTypography.headlineMedium,
            color = NoctaTextPrimary,
            modifier = Modifier.padding(NoctaSpacing.md)
        )

        if (uiState.messages.isEmpty()) {
            EmptyCoachState(onPromptSelected = { viewModel.send(it) })
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = NoctaSpacing.md, vertical = NoctaSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
            ) {
                items(uiState.messages, key = { it.id + it.role }) { message ->
                    AIMessageBubble(message)
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

@Composable
private fun EmptyCoachState(onPromptSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(NoctaSpacing.md),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Ask me anything about your sleep — I'll base what I say on your logged data, and I'll say so when I'm not sure.",
            style = NoctaTypography.bodyLarge,
            color = NoctaTextSecondary
        )
        Spacer(Modifier.height(NoctaSpacing.lg))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)) {
            items(defaultSuggestedPrompts) { prompt ->
                SuggestionChip(
                    onClick = { onPromptSelected(prompt.prompt) },
                    label = { Text(prompt.label) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = NoctaSurface,
                        labelColor = NoctaTextPrimary
                    ),
                    border = null
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(NoctaSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NoctaSpacing.sm)
    ) {
        TextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier
                .fillmaxwidth()
                .clip(NoctaShapes.large),
            placeholder = { Text("Ask Vela about your sleep") },
            enabled = enabled,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = NoctaSurface,
                unfocusedContainerColor = NoctaSurface,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedTextColor = NoctaTextPrimary,
                unfocusedTextColor = NoctaTextPrimary
            )
        )
        IconButton(
            onClick = onSend,
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier
                .clip(NoctaShapes.small)
                .background(if (enabled && text.isNotBlank()) NoctaAccent else NoctaSurface)
        ) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = "Send", tint = NoctaBackground)
        }
    }
}
