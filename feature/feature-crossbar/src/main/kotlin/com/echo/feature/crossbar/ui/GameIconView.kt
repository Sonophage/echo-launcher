package com.echo.feature.crossbar.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.aspectRatio
import coil3.compose.AsyncImage
import com.echo.core.ui.image.ArtworkRevisions
import com.echo.core.ui.image.rememberArtworkModel
import com.echo.core.ui.icons.GameIconStyle
import com.echo.feature.artwork.store.ArtworkDimensions
import com.echo.feature.crossbar.R
import com.echo.feature.crossbar.viewmodel.CrossbarItem

private val PspShape    = RoundedCornerShape(4.dp)
private val SquircleShape = com.echo.core.ui.icons.AppIconContainerShape

private val IconBorder              = Color(0x55FFFFFF)
private val ShineColor              = Color(0x18FFFFFF)

// the shelf's cover, but not the icon it falls back to: with no cover the row keeps its icon. A game's cover is
// its icon slot first (owner, 2026-10-05)
private val CrossbarItem.rowCover: String?
    get() = if (gameId != null) com.echo.core.domain.model.coverArtOf(iconUri, artworkUri) else shelfCoverArt?.takeIf { it != iconUri }

@Composable
fun GameIcon(
    item: CrossbarItem,
    iconStyle: GameIconStyle,
    modifier: Modifier = Modifier,
) {
    when {
        item.isAndroidApp && !item.isRealGame && item.iconUri != null -> PspIcon0Icon(
            artworkUri  = item.iconUri,
            accentColor = item.accentColor?.let { Color(it) },
            title       = item.title,
            modifier    = modifier,
        )
        item.isAndroidApp && !item.isRealGame -> AndroidAppIcon(
            packageName = item.packageName,
            title       = item.title,
            modifier    = modifier,
        )

        // the cover keeps its own shape in the row's slot; a game with no cover keeps its icon
        iconStyle == GameIconStyle.COVER_ART && item.rowCover != null -> NaturalArtSlot(modifier) { artModifier ->
            AsyncImage(
                model = rememberArtworkModel(item.rowCover),
                contentDescription = item.title,
                contentScale = ContentScale.Fit,
                modifier = artModifier,
            )
        }

        iconStyle == GameIconStyle.CARTRIDGE -> NaturalArtSlot(modifier) { artModifier ->
            PhysicalMediaIcon(
                platformId  = item.platformId,
                accentColor = item.accentColor?.let { Color(it) },
                title       = item.title,
                modifier    = artModifier,
            )
        }

        else -> {
            val panelShowingVideo = LocalPanelShowingVideo.current
            val video = LocalFocusedGameVideo.current?.takeIf {
                it.gameId == item.gameId &&
                    snapSiteFor(it.placement, panelShowingVideo) == SnapSite.TILE
            }
            Box(modifier = modifier) {
                PspIcon0Icon(
                    artworkUri  = item.iconUri,
                    accentColor = item.accentColor?.let { Color(it) },
                    title       = item.title,
                    modifier    = Modifier.fillMaxSize(),
                )
                if (video != null) {
                    Icon1VideoOverlay(
                        videoUri = video.uri,
                        modifier = Modifier.fillMaxSize().clip(PspShape),
                    )
                }
            }
        }
    }
}

private val NATURAL_ART_HEIGHT = 84.dp

@Composable
private fun NaturalArtSlot(
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        content(Modifier.fillMaxWidth().requiredHeight(NATURAL_ART_HEIGHT))
    }
}

@Composable
fun PspIcon0Icon(
    artworkUri: String?,
    accentColor: Color?,
    title: String,
    modifier: Modifier = Modifier,
) {
    val accent = accentColor ?: Color(0xFF4A9EFF)

    Box(
        modifier = modifier
            .clip(PspShape)
            .background(Color(0xFF0A0A0F))
            .border(1.dp, IconBorder, PspShape),
        contentAlignment = Alignment.Center,
    ) {
        if (artworkUri != null) {
            AsyncImage(
                model              = rememberArtworkModel(artworkUri),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(accent.copy(alpha = 0.6f), Color(0xFF0A0A0F)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White.copy(alpha = 0.85f),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(ShineColor, Color.Transparent)))
        )
    }
}

private const val ASSET_BASE = "file:///android_asset/systems/physical-media"

@Composable
fun PhysicalMediaIcon(
    platformId: String?,
    accentColor: Color?,
    title: String,
    modifier: Modifier = Modifier,
) {
    val assetName   = physicalMediaAssetName(platformId)
    val fallbackRes = physicalMediaIconRes(platformId) ?: R.drawable.media_cartridge
    var assetFailed by remember(assetName) { mutableStateOf(false) }

    Box(
        modifier         = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (assetName != null && !assetFailed) {
            AsyncImage(
                model              = "$ASSET_BASE/$assetName.png",
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize(),
                onError            = { assetFailed = true },
            )
        } else {
            Image(
                painter            = painterResource(id = fallbackRes),
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun AndroidAppIcon(
    packageName: String?,
    title: String,
    modifier: Modifier = Modifier,

    size: Dp = 52.dp,
) {
    val context = LocalContext.current

    val imageBitmap = remember(packageName) {
        if (packageName == null) return@remember null
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bmp = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(96),
                drawable.intrinsicHeight.coerceAtLeast(96),
                Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        }.getOrNull()
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(SquircleShape)
            .background(Color(0xFF1A1A20)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                painter            = BitmapPainter(imageBitmap),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text       = title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White.copy(alpha = 0.7f),
            )
        }
    }
    }
}
