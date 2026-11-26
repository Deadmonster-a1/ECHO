package com.spotify.clone.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoMarqueeText
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.LivingEqualizer
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.SwipeableTrackRow
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.model.Track

@Composable
fun TrackRowItem(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    onSwipeQueue: (() -> Unit)? = null,
    onSwipeFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppTheme.current
    val titleColor by animateColorAsState(
        targetValue = if (isPlaying) activeTheme.primaryColor else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
        label = "track_row_title_color"
    )
    val rowHighlight by animateColorAsState(
        targetValue = if (isPlaying) activeTheme.primaryColor.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(EchoMotion.DurationStandard),
        label = "track_row_bg"
    )

    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(rowHighlight)
                .echoPressable(scaleOnPress = 1.0f, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = track.coverArtUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(
                        visible = isPlaying,
                        enter = fadeIn(tween(160)) + expandHorizontally(tween(180, easing = EchoMotion.EaseOutStrong)),
                        exit = fadeOut(tween(120)) + shrinkHorizontally(tween(160))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LivingEqualizer(
                                isPlaying = true,
                                barCount = 3,
                                barColor = activeTheme.primaryColor,
                                maxHeight = 13.dp,
                                barWidth = 2.5.dp,
                                barSpacing = 2.dp
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                        }
                    }
                    EchoMarqueeText(
                        text = track.title,
                        color = titleColor,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.5.sp,
                            fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        enabled = isPlaying
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.isExplicit) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f))
                                .padding(horizontal = 4.dp, vertical = 0.5.dp)
                        ) {
                            Text(
                                text = "E",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    val subtitle = if (track.albumName.isNotBlank() && !track.albumName.equals("Single", ignoreCase = true)) {
                        "${track.artistName} • ${track.albumName}"
                    } else {
                        track.artistName
                    }
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onMoreClick != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .echoPressable(scaleOnPress = 0.88f, onClick = onMoreClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (onSwipeQueue != null) {
        SwipeableTrackRow(
            isFavorite = track.isFavorite,
            onSwipeQueue = onSwipeQueue,
            onSwipeFavorite = onSwipeFavorite,
            modifier = modifier
        ) {
            rowContent()
        }
    } else {
        Box(modifier = modifier) {
            rowContent()
        }
    }
}
