package com.nocta.app.ui.components.companion

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nocta.app.R
import kotlin.math.roundToInt

/**
 * The in-app floating companion ("SHADE").
 *
 * The original portrait is shown as-is: no circular mask, no border and no
 * background plate — ContentScale.Fit preserves the artwork's aspect ratio so
 * the face is never stretched or cropped. Dragging moves the companion and a
 * simple tap still opens the Coach.
 */
@Composable
fun NoctaCompanion(
    modifier: Modifier = Modifier,
    size: Dp = DefaultCompanionSize,
    portraitRes: Int = R.drawable.coach_avatar_portrait,
    onClick: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Keep the companion within the requested 64–80 dp range no matter what
    // the caller passes in.
    val companionSize = size.coerceIn(MinCompanionSize, MaxCompanionSize)

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    offsetX.roundToInt(),
                    offsetY.roundToInt()
                )
            }
            .size(companionSize)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()

                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = portraitRes),
            contentDescription = "Nocta AI Coach",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(companionSize)
        )
    }
}

private val DefaultCompanionSize = 72.dp
private val MinCompanionSize = 64.dp
private val MaxCompanionSize = 80.dp
