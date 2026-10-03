package com.psplauncher.feature.xmb.viewmodel

import com.psplauncher.core.common.format.formatByteSize
import com.psplauncher.core.common.format.relativeTime
import com.psplauncher.core.domain.model.GamepadAction
import com.psplauncher.core.ui.notification.AndroidNotice
import com.psplauncher.feature.xmb.ui.detail.DetailPanelContent
import com.psplauncher.feature.xmb.ui.playTimeLabel

data class GameInfoState(
    val item: XMBItem,
    val content: DetailPanelContent? = null,
    val achievementsStat: String? = null,
    val appVersion: String? = null,
    val appStorageBytes: Long? = null,
    val cursor: Int? = null,
    val videoUri: String? = null,
    val manualPath: String? = null,
    val band: GameInfoAction = GameInfoAction.PLAY,
    val open: GameInfoAction? = null,
    val infoScroll: Int = 0,
) {
    val isApp: Boolean get() = recentKind(item) == RecentKind.APP
}

enum class GameInfoAction { PLAY, INFO, VIDEO, MANUAL, OPTIONS }

fun gameInfoActions(info: GameInfoState): List<GameInfoAction> = listOfNotNull(
    GameInfoAction.PLAY,
    GameInfoAction.INFO.takeIf { !info.isApp && info.content != null },
    GameInfoAction.VIDEO.takeIf { !info.isApp && info.videoUri != null },
    GameInfoAction.MANUAL.takeIf { !info.isApp && info.manualPath != null },
    GameInfoAction.OPTIONS,
)

fun stepGameInfoBand(current: GameInfoAction, actions: List<GameInfoAction>, delta: Int): GameInfoAction {
    val index = actions.indexOf(current)
    if (index < 0) return actions.first()
    return actions[(index + delta).coerceIn(0, actions.lastIndex)]
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
