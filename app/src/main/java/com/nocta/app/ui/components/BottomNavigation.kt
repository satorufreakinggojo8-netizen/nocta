package com.nocta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nocta.app.navigation.NoctaDestination
import com.nocta.app.ui.theme.NoctaAccent
import com.nocta.app.ui.theme.NoctaBackground
import com.nocta.app.ui.theme.NoctaTextTertiary
import com.nocta.app.ui.theme.NoctaTypography

@Composable
fun NoctaBottomNavigation(
    currentRoute: String?,
    onNavigate: (NoctaDestination) -> Unit
) {
    NavigationBar(
        containerColor = NoctaBackground,
        tonalElevation = 0.dp
    ) {
        NoctaDestination.bottomNavItems.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label, style = NoctaTypography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NoctaAccent,
                    selectedTextColor = NoctaAccent,
                    unselectedIconColor = NoctaTextTertiary,
                    unselectedTextColor = NoctaTextTertiary,
                    indicatorColor = NoctaBackground
                )
            )
        }
    }
}
