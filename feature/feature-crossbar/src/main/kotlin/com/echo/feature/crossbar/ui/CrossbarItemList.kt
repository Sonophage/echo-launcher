package com.echo.feature.crossbar.ui

import com.echo.core.ui.theme.EchoTextStyle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.feature.crossbar.viewmodel.PanelStage
import com.echo.feature.crossbar.viewmodel.formatDuration
import com.echo.feature.crossbar.viewmodel.playbackFraction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.echo.core.ui.icons.AppIconContainerShape
import com.echo.core.ui.icons.rememberAppIcon
import com.echo.core.ui.icons.GameIconStyle
import com.echo.core.ui.icons.LocalCrossbarIconOverrides
import com.echo.core.ui.icons.PortalIcon
import com.echo.core.ui.icons.ThemedGlyph
import com.echo.core.ui.icons.categoryIconFor
import com.echo.core.ui.icons.systemIconRes
import com.echo.core.ui.theme.LocalEchoColors
import com.echo.feature.crossbar.viewmodel.GRID_COVER_COUNT
import com.echo.core.domain.model.PlayState
import androidx.compose.ui.graphics.vector.ImageVector
import com.echo.core.ui.design.holdOutline
import com.echo.core.ui.design.holdProgress
import com.echo.core.ui.design.pressAndHold
import com.echo.feature.crossbar.viewmodel.shelfCardFor
import com.echo.feature.crossbar.viewmodel.ShelfCard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.NewReleases
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarItemType
import com.echo.core.ui.image.rememberArtworkModel
import com.echo.themekit.CrossbarLayoutSpec
import androidx.compose.runtime.ReadOnlyComposable
import com.echo.core.ui.theme.LocalEchoTextColors
import com.echo.core.ui.components.CrossbarDim

private val GAME_ICON_WIDTH = 126.dp
private val GAME_ICON_HEIGHT = 70.dp

internal val ROW_HEIGHT = 88.dp

private val ARTWORK_TEXT_GAP = 16.dp

private val TAP_TARGET_HEIGHT = 72.dp

internal val LEADING_ICON_SLOT = CrossbarLayoutSpec.DEFAULT.itemIconSlotDp.dp

internal data class LiveRowProgress(
    val itemId: String,
    val durationMs: Long,
)

internal val LocalLiveRowProgress = androidx.compose.runtime.compositionLocalOf<LiveRowProgress?> { null }

internal class PlaybackPositions(val music: StateFlow<Int>, val external: StateFlow<Long>)

internal val LocalPlaybackPositions = androidx.compose.runtime.staticCompositionLocalOf {
    PlaybackPositions(MutableStateFlow(0), MutableStateFlow(0L))
}

@Composable
internal fun ownPositionMs(): Long =
    LocalPlaybackPositions.current.music.collectAsStateWithLifecycle().value.toLong()

@Composable
internal fun PanelStage.Music.livePositionMs(): Long = when {
    !loaded -> 0L
    packageName != null -> LocalPlaybackPositions.current.external.collectAsStateWithLifecycle().value
    else -> ownPositionMs()
}

@Composable
private fun LiveRowScrubber(durationMs: Long, style: TextStyle) {
    val positionMs = ownPositionMs()
    RowScrubber(
        playbackFraction(positionMs, durationMs) ?: 0f,
        formatDuration(positionMs) + "  /  " + formatDuration(durationMs),
        style,
    )
}

@Composable
private fun RowScrubber(fraction: Float, label: String?, style: TextStyle) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Box(
            Modifier
                .width(ScrubberWidth)
                .height(ScrubberHeight)
                .clip(RoundedCornerShape(ScrubberHeight / 2))
                .background(Color.White.copy(alpha = 0.22f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(ScrubberHeight)
                    .clip(RoundedCornerShape(ScrubberHeight / 2))
                    .background(LocalEchoColors.current.accentColor),
            )
        }
        label?.let {
            Text(
                text = it,
                color = SecondaryText,
                fontSize = 11.sp,
                style = style,
                maxLines = 1,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

private val ScrubberWidth = 96.dp
private val ScrubberHeight = 3.dp

private val LEADING_ICON_SIZE = CrossbarLayoutSpec.DEFAULT.itemIconDp.dp

internal val LEADING_ICON_CENTER = 18.dp + LEADING_ICON_SLOT / 2

private val PrimaryText: Color @Composable @ReadOnlyComposable get() = LocalEchoTextColors.current.primary

private val SecondaryText: Color @Composable @ReadOnlyComposable get() = LocalEchoTextColors.current.secondary

private val InactiveText: Color @Composable @ReadOnlyComposable get() = LocalEchoTextColors.current.inactive

private const val FlatUnfocusedRowAlpha = 0.68f

private val SelectedTextShadow = Shadow(
    color = Color(0x73001627),
    offset = Offset.Zero,
    blurRadius = 12f,
)

private val ROW_HORIZONTAL_PADDING = 18.dp

val CrossbarTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.75f),
    offset = Offset(0f, 2f),
    blurRadius = 4f,
)

internal const val MEMORY_CARD_DEFAULT_ART = "file:///android_asset/systems/physical-media/_default.png"

private val DRILL_GAME_COLUMN_LEFT = 138.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrossbarDrillFlyout(
    siblings: List<CrossbarItem>,
    siblingIndex: Int,
    items: List<CrossbarItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onItemLongPress: (Int) -> Unit,

    onSiblingTap: (Int) -> Unit = {},
    iconStyle: GameIconStyle = GameIconStyle.PSP_RECTANGLE,

    belowTopY: Dp = 152.dp,

    iconAnimatingAllowed: Boolean = false,

    labelHiddenByPanel: Boolean,


    cardArtGrid: Boolean = true,
    metadataAsSubtitle: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        CrossbarItemList(
            items = siblings,
            selectedIndex = siblingIndex,
            onItemSelected = onSiblingTap,
            onItemLongPress = {},
            iconStyle = iconStyle,
            belowTopY = belowTopY,
            showLabels = false,
            drillCursorOnSelected = true,
            iconAnimatingAllowed = iconAnimatingAllowed,
            modifier = Modifier.fillMaxHeight().width(DRILL_GAME_COLUMN_LEFT - 10.dp),
        )

        CrossbarGameColumn(
            items = items,
            selectedIndex = selectedIndex,
            iconStyle = iconStyle,
            belowTopY = belowTopY,
            onItemSelected = onItemSelected,
            onItemLongPress = onItemLongPress,
            iconAnimatingAllowed = iconAnimatingAllowed,
            cardArtGrid = cardArtGrid,
            labelHiddenByPanel = labelHiddenByPanel,
            metadataAsSubtitle = metadataAsSubtitle,
            modifier = Modifier.fillMaxSize().padding(start = DRILL_GAME_COLUMN_LEFT),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CrossbarGameColumn(
    items: List<CrossbarItem>,
    selectedIndex: Int,
    iconStyle: GameIconStyle,
    belowTopY: Dp,
    labelHiddenByPanel: Boolean,

    cardArtGrid: Boolean = true,
    metadataAsSubtitle: Boolean = false,
    onItemSelected: (Int) -> Unit,
    onItemLongPress: (Int) -> Unit,
    iconAnimatingAllowed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        if (items.isEmpty()) return@BoxWithConstraints
        val sel = selectedIndex.coerceIn(0, items.lastIndex)

        val rowsBelow = ((maxHeight.value - belowTopY.value) / ROW_HEIGHT.value).toInt() + 2
        val glide = rememberGlidePosition(sel)

        for (i in columnRows(sel, items.size, rowsBelow)) {
            CrossbarVerticalListRow(
                item = items[i],
                isSelected = i == selectedIndex,

                showText = true,

                cardArtGrid = cardArtGrid,

                labelHiddenByPanel = labelHiddenByPanel,
                metadataAsSubtitle = metadataAsSubtitle,
                iconStyle = iconStyle,
                onClick = { onItemSelected(i) },
                onLongPress = { onItemLongPress(i) },
                showIcon = true,

                iconAnimatingAllowed = iconAnimatingAllowed,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
                    .offset { IntOffset(0, (belowTopY + ROW_HEIGHT * (i - glide.value)).roundToPx()) },
            )
        }
    }
}

@Composable
private fun CardArtGrid(covers: List<String>, size: Dp, modifier: Modifier = Modifier) {
    val gap = size * 0.06f
    val cell = (size - gap) / 2
    Box(modifier.size(size)) {
        covers.take(GRID_COVER_COUNT).forEachIndexed { i, uri ->
            AsyncImage(
                model = rememberArtworkModel(uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(cell)
                    .offset(
                        x = if (i % 2 == 0) 0.dp else cell + gap,
                        y = if (i < 2) 0.dp else cell + gap,
                    )
                    .clip(RoundedCornerShape(cell * 0.10f)),
            )
        }
    }
}

internal fun memoryCardSlotKeyFor(item: CrossbarItem): String? = when {
    item.type == CrossbarItemType.COLLECTION -> "item_memcard_games"
    item.type != CrossbarItemType.MEMORY_CARD -> null
    item.id == "all_music" -> "item_memcard_music"
    item.id == "all_videos" -> "item_memcard_video"
    item.id == "all_photos" -> "item_memcard_photos"
    else -> null
}

internal fun itemSlotKeyFor(type: CrossbarItemType): String? = when (type) {
    CrossbarItemType.ADD_ACTION -> "item_add"
    CrossbarItemType.MISSING -> "item_missing"
    CrossbarItemType.VIDEO_FOLDER -> "item_video_folder"
    CrossbarItemType.VIDEO_LIBRARY -> "item_video_library"
    CrossbarItemType.VIDEO_RECENT -> "item_video_recent"
    CrossbarItemType.VIDEO_FAVORITES -> "item_video_favorites"
    CrossbarItemType.VIDEO_COLLECTIONS -> "item_video_collections"
    CrossbarItemType.VIDEO_FILE -> "item_video_file"
    CrossbarItemType.PHOTO_FOLDER -> "item_photo_folder"
    CrossbarItemType.PHOTO_FILE -> "item_photo_file"
    CrossbarItemType.PHOTO_ALBUMS -> "item_photo_albums"
    CrossbarItemType.LIBRARY_SHELVES -> "item_library_shelves"
    CrossbarItemType.LIBRARY_READER -> "item_library_reader"
    CrossbarItemType.LIBRARY_FOLDER -> "item_library_folder"
    CrossbarItemType.LIBRARY_BOOK -> "item_library_book"
    CrossbarItemType.LIBRARY_SERIES -> "item_library_series"
    CrossbarItemType.CAMERA -> "item_camera"
    CrossbarItemType.SEARCH -> "item_search"
    CrossbarItemType.MUSIC_TRACK -> "item_music_track"
    CrossbarItemType.MUSIC_ARTISTS -> "item_music_artists"
    CrossbarItemType.MUSIC_ALBUMS -> "item_music_albums"
    CrossbarItemType.PLAYLIST -> "item_playlist"
    else -> null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrossbarItemList(
    items: List<CrossbarItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onItemLongPress: (Int) -> Unit,
    iconStyle: GameIconStyle = GameIconStyle.PSP_RECTANGLE,


    belowTopY: Dp = 152.dp,

    showIcons: Boolean = true,

    showLabels: Boolean = true,

    labelHiddenByPanel: Boolean = false,

    cardArtGrid: Boolean = true,
    metadataAsSubtitle: Boolean = false,

    drillCursorOnSelected: Boolean = false,

    fadeByDistance: Boolean = true,

    textShadow: Boolean = true,

    iconAnimatingAllowed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().fillMaxHeight().clipToBounds()) {
        val rowsBelow = ((maxHeight.value - belowTopY.value) / ROW_HEIGHT.value).toInt()
            .coerceAtLeast(1)
        val sel = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        val glide = rememberGlidePosition(sel)

        if (items.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = belowTopY)
                    .graphicsLayer { translationY = (sel - glide.value) * ROW_HEIGHT.toPx() },
            ) {
                for (i in columnRows(sel, items.size, rowsBelow)) {
                    key(items[i].id) {
                        CrossbarVerticalListRow(
                            labelHiddenByPanel = labelHiddenByPanel,
                            cardArtGrid = cardArtGrid,
                            metadataAsSubtitle = metadataAsSubtitle,
                            item = items[i],
                            isSelected = i == selectedIndex,

                            showText = showLabels,
                            iconStyle = iconStyle,
                            onClick = { onItemSelected(i) },
                            onLongPress = { onItemLongPress(i) },
                            showIcon = showIcons,
                            trailingCursor = drillCursorOnSelected && i == selectedIndex,
                            fadeByDistance = fadeByDistance,

                            distance = i - sel,
                            textShadow = textShadow,
                            iconAnimatingAllowed = iconAnimatingAllowed,
                            modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CrossbarVerticalListRow(
    item: CrossbarItem,
    isSelected: Boolean,

    showText: Boolean,
    iconStyle: GameIconStyle,
    onClick: () -> Unit,
    onLongPress: () -> Unit,

    showIcon: Boolean = true,

    trailingCursor: Boolean = false,

    fadeByDistance: Boolean = true,

    distance: Int = 0,

    textShadow: Boolean = true,

    labelHiddenByPanel: Boolean,

    cardArtGrid: Boolean = true,
    metadataAsSubtitle: Boolean = false,

    iconAnimatingAllowed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 0.9f,

        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "crossbarListRowScale",
    )
    val rowAlpha by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f

            item.type == CrossbarItemType.EMPTY -> 0.5f

            fadeByDistance -> CrossbarDim.ranked(distance)
            else -> FlatUnfocusedRowAlpha
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "crossbarListRowAlpha",
    )

    val glow by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "crossbarRowGlow",
    )

    val subtitleStyle = if (textShadow) EchoTextStyle.copy(shadow = CrossbarTextShadow) else EchoTextStyle

    val density = LocalDensity.current
    val iconCenterPx = remember(density) { with(density) { LEADING_ICON_CENTER.toPx() } }
    var rowWidthPx by remember { mutableStateOf(0f) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .onSizeChanged { rowWidthPx = it.width.toFloat() }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                if (rowWidthPx > 0f) {
                    transformOrigin = TransformOrigin((iconCenterPx / rowWidthPx).coerceIn(0f, 1f), 0.5f)
                }
            }
            .alpha(rowAlpha),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier

                .weight(1f, fill = false)
                .height(TAP_TARGET_HEIGHT)

                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongPress,
                )
                .padding(horizontal = ROW_HORIZONTAL_PADDING),
        ) {
            if (showIcon && !item.textOnly) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.echo.core.ui.icons.LocalIconAnimating provides
                        (isSelected && iconAnimatingAllowed),
                ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.crossbarFocusGlow(
                        visible = glow,
                        reach = CrossbarGlow.RowReach,
                        alpha = CrossbarGlow.RowAlpha,
                    ),
                ) {
                    CrossbarItemLeadingIcon(
                        item = item,
                        iconStyle = iconStyle,
                        isSelected = isSelected,
                        cardArtGrid = cardArtGrid,
                    )
                }
                }
            }

            val showGameText = item.textOnly || !item.isRealGame || isSelected

            val panelHidesLabel = isSelected && item.isRealGame && !item.textOnly && labelHiddenByPanel
            val labelAlpha by animateFloatAsState(
                targetValue = if (panelHidesLabel) 0f else 1f,
                animationSpec = tween(220),
                label = "crossbarRowLabelFade",
            )
            if (showText && showGameText && labelAlpha > 0f) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .alpha(labelAlpha)
                        .padding(start = CrossbarLayoutSpec.DEFAULT.itemTextStartGapDp.dp),
                ) {
                    val titleColor = if (isSelected) PrimaryText else InactiveText
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.title,
                            color = titleColor,
                            fontSize = if (isSelected) CrossbarLayoutSpec.DEFAULT.itemTextSelectedSp.sp
                            else CrossbarLayoutSpec.DEFAULT.itemTextSp.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            style = if (isSelected) EchoTextStyle.copy(shadow = SelectedTextShadow) else EchoTextStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        PlayState.fromName(item.playState)?.let { state ->
                            Spacer(Modifier.width(7.dp))
                            PlayStateBadge(state, dimmed = !isSelected)
                        }
                    }

                    val effectiveSubtitle =
                        if (metadataAsSubtitle) item.metadataLine ?: item.subtitle
                        else item.subtitle
                    effectiveSubtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                        Text(
                            text = subtitle,
                            color = SecondaryText,
                            fontSize = if (isSelected) 12.sp else 11.sp,
                            fontWeight = FontWeight.Normal,
                            style = subtitleStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }

                    if (isSelected) {
                        val live = LocalLiveRowProgress.current?.takeIf { it.itemId == item.id }
                        if (live != null) {
                            LiveRowScrubber(live.durationMs, subtitleStyle)
                        } else {
                            item.progressFraction?.let { RowScrubber(it, item.progressLabel, subtitleStyle) }
                        }
                    }

                }
            }

            if (trailingCursor) {
                Text(
                    text = "◀",
                    color = LocalEchoColors.current.accentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

private fun Modifier.selectedIconBloom(isSelected: Boolean): Modifier = this

@Composable
private fun CrossbarItemLeadingIcon(
    item: CrossbarItem,
    iconStyle: GameIconStyle,
    isSelected: Boolean,
    cardArtGrid: Boolean = true,
) {
    val iconTint = LocalEchoColors.current.iconColor

    val userPickedIcon = (memoryCardSlotKeyFor(item) ?: itemSlotKeyFor(item.type))
        ?.let { com.echo.core.ui.icons.LocalCustomIcons.current[it] }
    if (cardArtGrid && item.insideCovers.isNotEmpty() && userPickedIcon == null && item.iconKey == null) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(LEADING_ICON_SIZE).selectedIconBloom(isSelected),
            ) {
                CardArtGrid(item.insideCovers, LEADING_ICON_SIZE)
            }
        }
        return
    }

    when {
        item.type == CrossbarItemType.MUSIC_TRACK -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(LEADING_ICON_SLOT),
            ) {
                if (item.coverUri != null) {
                    AsyncImage(
                        model = item.coverUri,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)),
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1B1B27)),
                    ) {
                        ThemedGlyph(
                            slotKey = itemSlotKeyFor(item.type) ?: "",
                            defaultVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        }

        item.type == CrossbarItemType.MUSIC_ARTISTS -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.Person, null, iconTint, Modifier.size(48.dp))
            }
        }
        item.type == CrossbarItemType.MUSIC_ALBUMS -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.Album, null, iconTint, Modifier.size(48.dp))
            }
        }

        item.type == CrossbarItemType.PLAYLIST -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(LEADING_ICON_SLOT),
            ) {
                ThemedGlyph(
                    slotKey = itemSlotKeyFor(item.type) ?: "",
                    defaultVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        item.type == CrossbarItemType.VIDEO_FILE -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(LEADING_ICON_SLOT),
            ) {
                if (item.coverUri != null) {
                    AsyncImage(
                        model = item.coverUri,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.size(width = 60.dp, height = 40.dp).clip(RoundedCornerShape(6.dp)),
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = 60.dp, height = 40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1B1B27)),
                    ) {
                        ThemedGlyph(
                            slotKey = itemSlotKeyFor(item.type) ?: "",
                            defaultVector = Icons.Filled.Movie,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }

        item.type == CrossbarItemType.VIDEO_FOLDER -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                if (item.coverUri != null) {
                    AsyncImage(
                        model = item.coverUri,
                        contentDescription = null,
                        modifier = Modifier.size(LEADING_ICON_SIZE).clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.Folder, null, iconTint, Modifier.size(48.dp))
                }
            }
        }

        item.type == CrossbarItemType.VIDEO_LIBRARY -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.VideoLibrary, null, iconTint, Modifier.size(48.dp))
            }
        }
        item.type == CrossbarItemType.VIDEO_RECENT -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.History, null, iconTint, Modifier.size(48.dp))
            }
        }
        item.type == CrossbarItemType.VIDEO_FAVORITES || item.type == CrossbarItemType.PHOTO_FAVORITES -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.Star, null, iconTint, Modifier.size(48.dp))
            }
        }

        item.type == CrossbarItemType.VIDEO_COLLECTIONS -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.Bookmarks, null, iconTint, Modifier.size(46.dp))
            }
        }

        item.type == CrossbarItemType.PHOTO_FILE -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(LEADING_ICON_SLOT),
            ) {
                if (item.coverUri != null) {
                    AsyncImage(
                        model = item.coverUri,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.size(width = 60.dp, height = 40.dp).clip(RoundedCornerShape(6.dp)),
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = 60.dp, height = 40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1B1B27)),
                    ) {
                        ThemedGlyph(
                            slotKey = itemSlotKeyFor(item.type) ?: "",
                            defaultVector = Icons.Filled.Photo,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }

        item.type == CrossbarItemType.PHOTO_FOLDER -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.Folder, null, iconTint, Modifier.size(48.dp))
            }
        }

        item.type == CrossbarItemType.PHOTO_ALBUMS -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.PhotoLibrary, null, iconTint, Modifier.size(48.dp))
            }
        }

        item.type == CrossbarItemType.LIBRARY_SHELVES -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.CollectionsBookmark, null, iconTint, Modifier.size(46.dp))
            }
        }
        item.type == CrossbarItemType.LIBRARY_READER -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.ImportContacts, null, iconTint, Modifier.size(46.dp))
            }
        }
        item.type == CrossbarItemType.LIBRARY_FOLDER -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.Folder, null, iconTint, Modifier.size(46.dp))
            }
        }

        item.type == CrossbarItemType.LIBRARY_SERIES -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                if (item.coverUri != null) {
                    AsyncImage(
                        model = item.coverUri,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.size(width = 40.dp, height = 56.dp).clip(RoundedCornerShape(4.dp)),
                    )
                } else {
                    ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.Bookmarks, null, iconTint, Modifier.size(46.dp))
                }
            }
        }
        item.type == CrossbarItemType.LIBRARY_BOOK -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                if (item.coverUri != null) {
                    AsyncImage(
                        model = item.coverUri,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.size(width = 40.dp, height = 56.dp).clip(RoundedCornerShape(4.dp)),
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = 40.dp, height = 56.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1B1B27)),
                    ) {
                        ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.Book, null, iconTint, Modifier.size(26.dp))
                    }
                }
            }
        }

        item.type == CrossbarItemType.CAMERA -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.PhotoCamera, null, iconTint, Modifier.size(48.dp))
            }
        }

        item.type == CrossbarItemType.SEARCH -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.Search, null, iconTint, Modifier.size(44.dp))
            }
        }

        item.type == CrossbarItemType.ADD_ACTION -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                ThemedGlyph(itemSlotKeyFor(item.type) ?: "", Icons.Filled.Add, null, iconTint, Modifier.size(44.dp))
            }
        }

        item.type == CrossbarItemType.SHELF -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(LEADING_ICON_SIZE).selectedIconBloom(isSelected),
                ) {
                    Icon(
                        imageVector = shelfGlyphFor(item.id),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(LEADING_ICON_SIZE),
                    )
                }
            }
        }
        item.type == CrossbarItemType.MISSING -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(LEADING_ICON_SLOT)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(LEADING_ICON_SIZE).selectedIconBloom(isSelected),
                ) {
                    ThemedGlyph(
                        itemSlotKeyFor(item.type) ?: "",
                        Icons.AutoMirrored.Filled.HelpOutline,
                        null,
                        iconTint,
                        Modifier.size(LEADING_ICON_SIZE),
                    )
                }
            }
        }
        item.type == CrossbarItemType.ALL_GAMES ||
            item.type == CrossbarItemType.FAVORITES ||
            item.type == CrossbarItemType.MEMORY_CARD ||
            item.type == CrossbarItemType.COLLECTION -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(LEADING_ICON_SLOT),
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(LEADING_ICON_SIZE).selectedIconBloom(isSelected),
              ) {
                val collectionIconKey = item.iconKey?.takeIf { item.type == CrossbarItemType.COLLECTION }

                val memoryCardArt = item.coverUri
                    ?: MEMORY_CARD_DEFAULT_ART.takeIf { item.type == CrossbarItemType.COLLECTION && collectionIconKey == null }

                val memcardOverride = if (
                    collectionIconKey == null &&
                    (memoryCardArt == null || memoryCardArt.startsWith("file:///android_asset/systems/physical-media/"))
                ) {
                    memoryCardSlotKeyFor(item)?.let { key ->
                        com.echo.core.ui.icons.LocalCustomIcons.current[key]
                            ?: LocalCrossbarIconOverrides.current[key]
                    }
                } else {
                    null
                }

                if (memcardOverride != null) {
                    com.echo.core.ui.icons.CustomIconSurface(
                        icon = memcardOverride,
                        contentDescription = null,
                        modifier = Modifier.size(LEADING_ICON_SIZE),
                    )
                } else if (collectionIconKey != null) {
                    PortalIcon(
                        painter = painterResource(categoryIconFor(collectionIconKey).resId),
                        contentDescription = null,
                        modifier = Modifier.size(LEADING_ICON_SIZE),
                    )
                } else if (memoryCardArt != null) {
                    val isBundledSilhouette =
                        memoryCardArt.startsWith("file:///android_asset/systems/physical-media/")
                    if (isBundledSilhouette) {
                        BundledSilhouetteIcon(
                            assetUri = memoryCardArt,
                            modifier = Modifier.size(LEADING_ICON_SIZE),
                        )
                    } else {
                        AsyncImage(
                            model = memoryCardArt,
                            contentDescription = null,
                            modifier = Modifier.size(LEADING_ICON_SIZE),
                        )
                    }
                } else {
                    val iconKey = when (item.type) {
                        CrossbarItemType.MEMORY_CARD -> item.platformId
                        CrossbarItemType.ALL_GAMES   -> "allgames"
                        CrossbarItemType.FAVORITES   -> "favorites"
                        else                    -> null
                    }

                    if (iconKey != null) {
                        com.echo.core.ui.icons.ConsoleIcon(
                            platformId = iconKey,
                            contentDescription = null,
                            modifier = Modifier.size(LEADING_ICON_SIZE),
                        )
                    } else {
                        PortalIcon(
                            painter = painterResource(systemIconRes(iconKey)),
                            contentDescription = null,
                            modifier = Modifier.size(LEADING_ICON_SIZE),
                        )
                    }
                }
              }
            }
        }
        item.gameId != null -> {
            GameIcon(
                item = item,
                iconStyle = iconStyle,
                modifier = Modifier.size(width = GAME_ICON_WIDTH, height = GAME_ICON_HEIGHT),
            )
            Spacer(modifier = Modifier.width(ARTWORK_TEXT_GAP))
        }
        item.isAndroidApp && item.iconUri != null -> {
            GameIcon(
                item = item,
                iconStyle = iconStyle,
                modifier = Modifier.size(width = GAME_ICON_WIDTH, height = GAME_ICON_HEIGHT),
            )
            Spacer(modifier = Modifier.width(ARTWORK_TEXT_GAP))
        }
        item.isAndroidApp && item.packageName != null -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(LEADING_ICON_SLOT),
            ) {
                AppListIcon(
                    packageName = item.packageName,
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        item.id.startsWith("settings_") -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(LEADING_ICON_SLOT),
            ) {
                val settingsOverride = com.echo.core.ui.icons.LocalCustomIcons.current["item_settings"]
                    ?: LocalCrossbarIconOverrides.current["item_settings"]
                if (settingsOverride != null) {
                    com.echo.core.ui.icons.CustomIconSurface(
                        icon = settingsOverride,
                        contentDescription = null,
                        modifier = Modifier.size(LEADING_ICON_SIZE),
                    )
                } else {
                    PortalIcon(
                        painter = painterResource(systemIconRes("settings")),
                        contentDescription = null,
                        modifier = Modifier.size(LEADING_ICON_SIZE),
                    )
                }
            }
        }

        else -> Spacer(modifier = Modifier.width(LEADING_ICON_SLOT))
    }
}

@Composable
internal fun BundledSilhouetteIcon(assetUri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(assetUri) {
        runCatching {
            val assetPath = assetUri.removePrefix("file:///android_asset/")
            context.assets.open(assetPath).use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream).asImageBitmap()
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        PortalIcon(
            painter = BitmapPainter(bitmap),
            contentDescription = null,
            modifier = modifier,
        )
    } else {
        AsyncImage(model = assetUri, contentDescription = null, modifier = modifier)
    }
}

@Composable
private fun AppListIcon(
    packageName: String,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberAppIcon(packageName, sizePx = 192, foregroundOnly = true, colorOf = null)?.bitmap ?: return
    Image(
        bitmap = bitmap,
        contentDescription = null,

        modifier = modifier.clip(AppIconContainerShape),
    )
}


@Composable
private fun PlayStateBadge(state: PlayState, dimmed: Boolean) {
    val tint = when (state) {
        PlayState.COMPLETED -> Color(0xFF6FD08C)
        PlayState.PLAYING   -> Color(0xFFFFDCAA)
        PlayState.BACKLOG   -> Color(0x99FFFFFF)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = if (dimmed) 0.10f else 0.20f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = state.mark,
            color = tint.copy(alpha = if (dimmed) 0.55f else 1f),
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun shelfGlyphFor(cardId: String): ImageVector = when (val shelf = shelfCardFor(cardId)) {
    is ShelfCard.Favorites -> Icons.Filled.Star
    is ShelfCard.RecentlyAdded -> Icons.Filled.NewReleases
    is ShelfCard.Marked -> when (shelf.state) {
        PlayState.PLAYING -> Icons.Filled.PlayCircle
        PlayState.COMPLETED -> Icons.Filled.CheckCircle
        PlayState.BACKLOG -> Icons.Filled.Bookmarks
    }
    null -> Icons.Filled.Star
}

const val GLIDE_MAX_LEAD_ROWS = 3

const val GLIDE_STIFFNESS = 700f

fun columnRows(selected: Int, size: Int, rowsBelow: Int): IntRange =
    selected until minOf(size, selected + rowsBelow + GLIDE_MAX_LEAD_ROWS)

fun glideStart(current: Float, target: Int, maxLead: Int = GLIDE_MAX_LEAD_ROWS): Float =
    current.coerceIn(target - maxLead.toFloat(), target + maxLead.toFloat())

@Composable
private fun rememberGlidePosition(target: Int): State<Float> {
    val position = remember { Animatable(target.toFloat()) }
    LaunchedEffect(target) {
        position.snapTo(glideStart(position.value, target))
        position.animateTo(target.toFloat(), spring(dampingRatio = 1f, stiffness = GLIDE_STIFFNESS))
    }
    return position.asState()
}
