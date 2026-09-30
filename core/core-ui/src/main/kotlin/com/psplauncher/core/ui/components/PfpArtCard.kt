package com.psplauncher.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

val PfpArtCardComplete = Color(0xFF66BB6A)
val PfpArtCardPartial = Color(0xFFE0A030)

@Composable
fun PfpArtCard(
    title: String,
    artUri: String?,
    focused: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    lit: Boolean = focused,
    titleAlpha: Float = if (lit) 1f else 0.38f,
    onClick: (() -> Unit)? = null,
    emptyArt: @Composable BoxScope.(dim: Float) -> Unit = { dim ->
        Text("no sample yet", color = Color.White.copy(alpha = 0.18f * dim + 0.06f), fontSize = 9.sp)
    },
    footer: @Composable ColumnScope.(dim: Float) -> Unit,
) {
    val dim = if (lit) 1f else 0.38f
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (focused) 0.13f else if (lit) 0.08f else 0.04f))
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) accent else Color.White.copy(alpha = 0.07f),
                RoundedCornerShape(12.dp),
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
    ) {
        Text(
            title,
            color = Color.White.copy(alpha = titleAlpha),
            fontSize = 14.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.30f)),
            contentAlignment = Alignment.Center,
        ) {
            if (artUri != null) {
                AsyncImage(
                    model = artUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = dim,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                emptyArt(dim)
            }
        }
        Spacer(Modifier.height(8.dp))
        footer(dim)
    }
}
