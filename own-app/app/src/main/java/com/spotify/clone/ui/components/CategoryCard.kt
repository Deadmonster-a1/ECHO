package com.spotify.clone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.echoPressable

data class CategoryItem(
    val id: String,
    val title: String,
    val backgroundColor: Color,
    val imageUrl: String = "",
    val searchQuery: String = title
)

@Composable
fun CategoryCard(
    category: CategoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(105.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        category.backgroundColor,
                        category.backgroundColor.copy(alpha = 0.82f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .echoPressable(scaleOnPress = 0.95f, onClick = onClick)
    ) {
        // Rotated Genre/Cover Art Image on bottom-right corner (Spotify signature style)
        if (category.imageUrl.isNotBlank()) {
            AsyncImage(
                model = category.imageUrl,
                contentDescription = category.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 16.dp, y = 8.dp)
                    .rotate(25f)
                    .size(68.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }

        // Category Title on top-start
        Text(
            text = category.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 14.dp, end = 50.dp)
        )
    }
}
