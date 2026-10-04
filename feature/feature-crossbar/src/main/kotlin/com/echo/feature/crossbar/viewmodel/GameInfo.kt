package com.echo.feature.crossbar.viewmodel

import com.echo.core.common.format.playTimeLabel
import com.echo.core.common.format.formatByteSize
import com.echo.core.common.format.relativeTime
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.notification.AndroidNotice
import com.echo.feature.crossbar.ui.detail.DetailPanelContent

data class GameInfoState(
    val item: CrossbarItem,
    val content: DetailPanelContent? = null,
    val achievementsStat: String? = null,
    val appVersion: String? = null,
    val appStorageBytes: Long? = null,
    val cursor: Int? = null,
    val videoUri: String? = null,
    val manualPath: String? = null,
    val open: GameInfoAction? = null,
    val infoScroll: Int = 0,
    val infoScrollMax: Int = 0,
) {
    val isApp: Boolean get() = recentKind(item) == RecentKind.APP

    fun scrolledBy(delta: Int): GameInfoState = copy(infoScroll = (infoScroll + delta).coerceIn(0, infoScrollMax))
}

enum class GameInfoAction { PLAY, INFO, VIDEO, MANUAL, OPTIONS }

fun gameInfoActions(info: GameInfoState): List<GameInfoAction> = listOfNotNull(
    GameInfoAction.PLAY,
    GameInfoAction.INFO.takeIf { !info.isApp && info.content != null },
    GameInfoAction.VIDEO.takeIf { !info.isApp && info.videoUri != null },
    GameInfoAction.MANUAL.takeIf { !info.isApp && info.manualPath != null },
    GameInfoAction.OPTIONS,
)


// owner, 2026-10-04: LT/RT walk Game Info's views; null is the screenshots page itself.
// The manual opens its own viewer, so it stays in Options
fun gameInfoSections(info: GameInfoState): List<GameInfoAction?> =
    listOf<GameInfoAction?>(null) + gameInfoActions(info).filter { it == GameInfoAction.INFO || it == GameInfoAction.VIDEO }

fun stepGameInfoSection(info: GameInfoState, delta: Int): GameInfoAction? {
    val sections = gameInfoSections(info)
    return sections[(sections.indexOf(info.open).coerceAtLeast(0) + delta).mod(sections.size)]
}

fun gameInfoSectionLabel(section: GameInfoAction?): String = when (section) {
    null -> "Screenshots"
    GameInfoAction.INFO -> "Info"
    GameInfoAction.VIDEO -> "Video"
    else -> section.name.lowercase().replaceFirstChar { it.uppercase() }
}

data class GameInfoStat(val label: String, val value: String)

fun GameInfoState.notices(all: List<AndroidNotice>): List<AndroidNotice> =
    if (isApp) all.filter { it.packageName == item.packageName } else emptyList()

fun GameInfoState.cardCount(all: List<AndroidNotice>): Int =
    if (isApp) notices(all).size else content?.media?.size ?: 0

fun gameInfoStats(info: GameInfoState, newNotices: Int, now: Long): List<GameInfoStat> =
    if (info.isApp) {
        listOfNotNull(
            newNotices.takeIf { it > 0 }?.let { GameInfoStat("New", it.toString()) },
            info.appStorageBytes?.let { GameInfoStat("Storage", formatByteSize(it)) },
            info.appVersion?.takeIf { it.isNotBlank() }?.let { GameInfoStat("Version", it) },
        )
    } else {
        listOfNotNull(
            info.achievementsStat?.let { GameInfoStat("Achievements", it) },
            info.item.totalPlayTimeMillis.takeIf { it > 0L }?.let { GameInfoStat("Played", playTimeLabel(it)) },
            info.item.lastOpenedAt?.let { GameInfoStat("Last played", relativeTime(now, it)) },
            info.content?.platformName?.takeIf { it.isNotBlank() }?.let { GameInfoStat("Platform", it) },
        )
    }

fun stepGameInfoCursor(cursor: Int?, count: Int, action: GamepadAction): Int? {
    if (count <= 0) return null
    return when (action) {
        GamepadAction.NAVIGATE_DOWN -> cursor ?: 0
        GamepadAction.NAVIGATE_UP -> null
        GamepadAction.NAVIGATE_LEFT -> cursor?.let { (it - 1).coerceAtLeast(0) }
        GamepadAction.NAVIGATE_RIGHT -> cursor?.let { (it + 1).coerceAtMost(count - 1) }
        else -> cursor
    }?.coerceIn(0, count - 1)
}
