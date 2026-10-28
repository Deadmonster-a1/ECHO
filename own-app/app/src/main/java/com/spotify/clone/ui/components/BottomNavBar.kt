package com.spotify.clone.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable

enum class NavigationTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Outlined.Explore),
    SEARCH("search", "Search", Icons.Outlined.Search),
    LIBRARY("library", "Library", Icons.Outlined.GraphicEq)
}

/**
 * Signature ECHO Floating Bottom Navigation Bar
 * Fully responsive across compact mobile phones, foldables, and tablets:
 * - Fluid rounded pill container floating over linen canvas
 * - Width-constrained on tablets & desktop (max 520dp) for clean ergonomic reach
 * - Stable, fixed-height active indicator pill that prevents vertical reforming or layout shifts
 * - Icon and label with single-line guarantee so "Library" never wraps down on Y-axis
 * - Organic press feedback and edge-to-edge system insets
 */
@Composable
fun EchoBottomNavBar(
    currentRoute: String,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    val containerBg = if (isDarkMode) Color(0xFF16151E) else Color(0xFFFFFFFF)
    val containerBorder = if (isDarkMode) Color(0xFF282736) else Color(0xFFE5E0E8)
    val activePillBg = if (isDarkMode) activeTheme.primaryColor.copy(alpha = 0.22f) else activeTheme.primaryColor.copy(alpha = 0.16f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = if (isDarkMode) 4.dp else 10.dp,
                    shape = RoundedCornerShape(30.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(30.dp))
                .background(containerBg)
                .border(1.dp, containerBorder, RoundedCornerShape(30.dp))
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTab.entries.forEach { tab ->
                val isSelected = currentRoute == tab.route

                val animatedTextColor by animateColorAsState(
                    targetValue = if (isSelected) activeTheme.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(180),
                    label = "tab_text_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .echoPressable(scaleOnPress = 0.98f) {
                            onTabSelected(tab)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) activePillBg else Color.Transparent)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = animatedTextColor,
                                modifier = Modifier.size(19.dp)
                            )

                            Spacer(modifier = Modifier.width(5.dp))

                            Text(
                                text = tab.title,
                                color = animatedTextColor,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpotifyBottomNavBar(
    currentRoute: String,
    onTabSelected: (NavigationTab) -> Unit
) {
    EchoBottomNavBar(currentRoute = currentRoute, onTabSelected = onTabSelected)
}
