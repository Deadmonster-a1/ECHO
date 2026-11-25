package com.spotify.clone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
    val cardShape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(102.dp)
            .shadow(
                elevation = 10.dp,
                shape = cardShape,
                ambientColor = category.backgroundColor.copy(alpha = 0.28f),
                spotColor = category.backgroundColor.copy(alpha = 0.38f)
            )
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        category.backgroundColor,
                        category.backgroundColor.copy(alpha = 0.72f)
                    )
                )
            )
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f))
                ),
                shape = cardShape
            )
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
    ) {
        // Subtle ambient highlight orb for depth
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-24).dp)
                .size(84.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        )

        if (category.imageUrl.isNotBlank()) {
            AsyncImage(
                model = category.imageUrl,
                contentDescription = category.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
                    .rotate(16f)
                    .size(66.dp)
                    .shadow(10.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.75.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
            )
        }

        Text(
            text = category.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.5.sp,
            letterSpacing = (-0.2).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 15.dp, top = 14.dp, end = 54.dp)
        )
    }
}
