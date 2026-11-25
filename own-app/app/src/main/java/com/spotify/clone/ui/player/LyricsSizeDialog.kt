package com.spotify.clone.ui.player

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.LocalAppTheme

enum class LyricsSizeTier(val label: String, val fontSizeSp: Float, val subtitle: String) {
    COMPACT("Compact", 17f, "Fits more lines on screen at once"),
    STANDARD("Standard", 22f, "Default balanced typography"),
    LARGE("Large", 27f, "Enhanced readability and emphasis"),
    EXTRA_LARGE("Extra Large", 33f, "Stage monitor karaoke mode")
}

/**
 * Lyrics Font Sizing & Typography Dialog for ECHO.
 * Directly adopted from LastWave-Native LyricsSizeDialog.
 */
@Composable
fun LyricsSizeDialog(
    currentSizeTier: LyricsSizeTier,
    onSizeSelected: (LyricsSizeTier) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppTheme.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.FormatSize,
                    contentDescription = null,
                    tint = activeTheme.primaryColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Lyrics Typography",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LyricsSizeTier.entries.forEach { tier ->
                    val isSelected = tier == currentSizeTier
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSizeSelected(tier) }
                            .background(if (isSelected) activeTheme.primaryColor.copy(alpha = 0.12f) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSizeSelected(tier) },
                            colors = RadioButtonDefaults.colors(selectedColor = activeTheme.primaryColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = tier.label,
                                color = if (isSelected) activeTheme.primaryColor else MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = "${tier.fontSizeSp.toInt()} sp • ${tier.subtitle}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Cause you were there for me",
                        color = Color.White,
                        fontSize = currentSizeTier.fontSizeSp.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = (currentSizeTier.fontSizeSp * 1.35f).sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = activeTheme.primaryColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}
