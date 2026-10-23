package com.nocta.app.ui.guide

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nocta.app.ui.components.companion.NoctaCompanionOverlay
import com.nocta.app.ui.theme.NoctaAccent
import com.nocta.app.ui.theme.NoctaBackground
import com.nocta.app.ui.theme.NoctaSurfaceElevated
import com.nocta.app.ui.theme.NoctaSurfaceBorder
import com.nocta.app.ui.theme.NoctaTextPrimary
import com.nocta.app.ui.theme.NoctaTextSecondary
import com.nocta.app.ui.theme.NoctaTextTertiary
import com.nocta.app.ui.theme.NoctaTypography

/**
 * The floating companion plus its speech bubble, drawn once at the app shell
 * level so the bubble follows the user between tabs.
 *
 * The bubble sits directly above the character's head (never over its face)
 * and the lists beneath reserve [ShadeLayout.bottomInset] of space, so the
 * last card rests above the bubble instead of behind it.
 */
@Composable
fun ShadeStage(
    baseRoute: String?,
    tip: ShadeTip?,
    guideEnabled: Boolean,
    detailExpanded: Boolean,
    aiAvailable: Boolean,
    onActivate: () -> Unit,
    onMute: () -> Unit,
    onOpenCoach: () -> Unit,
    modifier: Modifier = Modifier
) {
    val characterHeight = ShadeLayout.companionHeight(baseRoute)

    Box(modifier = modifier.fillMaxSize()) {
        // The soft aura belongs to the dashboard composition only.
        if (baseRoute == ShadeGuide.ROUTE_HOME) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(width = 260.dp, height = 320.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NoctaAccent.copy(alpha = 0.13f),
                                NoctaAccent.copy(alpha = 0.04f),
                                NoctaBackground.copy(alpha = 0f)
                            )
                        )
                    )
            )
        }

        if (guideEnabled && tip != null) {
            ShadeBubble(
                tip = tip,
                aiAvailable = aiAvailable,
                expanded = detailExpanded,
                onActivate = onActivate,
                onMute = onMute,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = characterHeight + 6.dp)
            )
        }

        NoctaCompanionOverlay(
            onClick = onOpenCoach,
            characterHeight = characterHeight
        )
    }
}

/**
 * A compact speech bubble above the character.
 *
 * - Tapping it asks the coach when a backend exists; otherwise it reveals the
 *   app's own guidance for this screen (the detail text never goes dead).
 * - The mute control is a separate 48 dp touch target with its own label, so
 *   muting never requires hitting the bubble.
 * - The tip text is a polite live region, so a screen change is announced to
 *   screen readers rather than silently rewritten.
 */
@Composable
fun ShadeBubble(
    tip: ShadeTip,
    aiAvailable: Boolean,
    expanded: Boolean,
    onActivate: () -> Unit,
    onMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier.widthIn(max = ShadeLayout.BubbleMaxWidth),
        horizontalAlignment = Alignment.End
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(NoctaSurfaceElevated)
                .border(1.dp, NoctaSurfaceBorder, shape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (aiAvailable) "Ask Vela" else "Show guidance",
                    onClick = onActivate
                )
                .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 2.dp)
        ) {
            Text(
                text = tip.message,
                style = NoctaTypography.bodyMedium,
                color = NoctaTextPrimary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )

            if (expanded) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tip.detail,
                    style = NoctaTypography.labelSmall,
                    color = NoctaTextSecondary,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .padding(bottom = 4.dp)
                )
                if (!aiAvailable) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Coach replies aren't available in this build — this guidance " +
                            "comes from the app itself.",
                        style = NoctaTypography.labelSmall,
                        color = NoctaTextTertiary,
                        modifier = Modifier.padding(end = 8.dp, bottom = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        aiAvailable -> "Ask Vela"
                        expanded -> "Less"
                        else -> "More"
                    },
                    style = NoctaTypography.labelSmall,
                    color = NoctaAccent,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                IconButton(
                    onClick = onMute,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsOff,
                        contentDescription = "Mute Vela's tips",
                        tint = NoctaTextTertiary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // Tail pointing down toward the character's head.
        Canvas(
            modifier = Modifier
                .padding(end = 26.dp)
                .size(width = 16.dp, height = 8.dp)
        ) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width / 2f, size.height)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(path = path, color = NoctaSurfaceElevated)
            drawRect(
                color = NoctaSurfaceElevated,
                topLeft = Offset(0f, -1f),
                size = Size(size.width, 1f)
            )
        }
    }
}
