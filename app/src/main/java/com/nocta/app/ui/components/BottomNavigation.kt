package com.nocta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocta.app.navigation.NoctaDestination
import com.nocta.app.navigation.bottomNavDestinations
import com.nocta.app.ui.theme.NoctaAccent
import com.nocta.app.ui.theme.NoctaAccentMuted
import com.nocta.app.ui.theme.NoctaBackground
import com.nocta.app.ui.theme.NoctaSurfaceBorder
import com.nocta.app.ui.theme.NoctaTextTertiary
import com.nocta.app.ui.theme.NoctaTypography

/**
 * Six-destination bottom bar.
 *
 * Material's [androidx.compose.material3.NavigationBar] gives every item a fixed
 * width and lets labels wrap, which split "Optimize" onto two lines on a 360 dp
 * phone. This bar lays the destinations out with [Row] weights instead: each
 * item gets exactly 1/6 of the width, the label is a single non-wrapping line at
 * a size that fits the narrowest supported width (320 dp → 53 dp per item), and
 * the icon/label block is centred. No truncation, no hidden letters.
 */
@Composable
fun NoctaBottomNavigation(
    currentRoute: String?,
    onNavigate: (NoctaDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoctaBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Hairline top edge instead of a material elevation shadow.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NoctaSurfaceBorder)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 2.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (destination in bottomNavDestinations) {
                BottomNavItem(
                    destination = destination,
                    selected = currentRoute == destination.route,
                    onClick = { onNavigate(destination) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    destination: NoctaDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) NoctaAccent else NoctaTextTertiary

    Column(
        modifier = modifier
            .height(64.dp)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) NoctaAccentMuted else NoctaBackground)
                .padding(horizontal = 10.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.height(3.dp))

        Text(
            text = destination.label,
            style = NoctaTypography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            // "Optimize" is ~46 dp at 11 sp and every item is at least 53 dp on
            // a 320 dp phone, so the label stays whole without wrapping.
            modifier = Modifier.fillMaxWidth()
        )
    }
}
