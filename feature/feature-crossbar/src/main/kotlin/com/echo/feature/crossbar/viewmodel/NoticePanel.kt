package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.notification.AndroidNotice
import com.echo.core.ui.notification.SystemToast

data class LibraryChip(val id: String, val name: String, val visible: Boolean)

val LIBRARY_CHIP_IDS = listOf(
    com.echo.core.domain.model.BuiltInCategory.GAMES,
    com.echo.core.domain.model.BuiltInCategory.MUSIC,
    com.echo.core.domain.model.BuiltInCategory.VIDEO,
    com.echo.core.domain.model.BuiltInCategory.LIBRARY,
    com.echo.core.domain.model.BuiltInCategory.PHOTO,
    "network",
)

enum class NoticeIslandPress { SHOW_CARD, OPEN_PANEL }

// owner, 2026-10-05: the first press brings the newest notification out as the card, the second opens the
// panel; with nothing waiting a press opens it at once
internal fun noticeIslandPress(cardOut: Boolean, hasNotice: Boolean): NoticeIslandPress =
    if (!cardOut && hasNotice) NoticeIslandPress.SHOW_CARD else NoticeIslandPress.OPEN_PANEL

internal const val NOTICE_CARD_MS = 4_000L

// the notification card's rows, newest first; the card shows as many as fit (owner, 2026-10-05)
val CrossbarUiState.noticeCardRows: List<com.echo.core.ui.notification.AndroidNotice>
    get() = androidNotices.sortedByDescending { it.postedAt }

sealed interface NoticeCardStep {
    data class Move(val cursor: Int) : NoticeCardStep
    data class Open(val index: Int) : NoticeCardStep
    data class Dismiss(val index: Int) : NoticeCardStep
    data object Close : NoticeCardStep
    data object Stay : NoticeCardStep
}

// owner, 2026-10-05: on the card, up and down pick a notification, A opens its app, X dismisses it, B closes
// the card; anything else closes it and goes on to the screen
internal fun noticeCardStep(action: GamepadAction, cursor: Int, rows: Int): NoticeCardStep {
    if (rows == 0) return NoticeCardStep.Close
    val at = cursor.coerceIn(0, rows - 1)
    return when (action) {
        GamepadAction.NAVIGATE_UP -> if (at > 0) NoticeCardStep.Move(at - 1) else NoticeCardStep.Stay
        GamepadAction.NAVIGATE_DOWN -> if (at < rows - 1) NoticeCardStep.Move(at + 1) else NoticeCardStep.Stay
        GamepadAction.SELECT -> NoticeCardStep.Open(at)
        GamepadAction.CHANGE_SORT -> NoticeCardStep.Dismiss(at)
        else -> NoticeCardStep.Close
    }
}

enum class QuickSetting { WAVE, BACKDROP, ROW_ART, RECENT_APPS, SECOND_SCREEN, ANDROID_SETTINGS, LIBRARIES }

val PANEL_QUICK_SETTINGS = listOf(QuickSetting.WAVE, QuickSetting.BACKDROP, QuickSetting.ROW_ART, QuickSetting.RECENT_APPS, QuickSetting.ANDROID_SETTINGS)

// owner, 2026-10-06: dual or single screen, quickly; offered only on a device with a second display
fun quickSettingsFor(secondDisplay: Boolean): List<QuickSetting> =
    if (secondDisplay) PANEL_QUICK_SETTINGS.toMutableList().apply { add(indexOf(QuickSetting.ANDROID_SETTINGS), QuickSetting.SECOND_SCREEN) }
    else PANEL_QUICK_SETTINGS

const val LIBRARY_GRID_COLUMNS = 3

// four across, so the seven sections fit the panel in two rows (Library made the seventh)
const val SETTINGS_GRID_COLUMNS = 4

val PANEL_SETTINGS: List<com.echo.core.domain.model.SettingsSectionId> = com.echo.core.domain.model.SettingsSectionId.entries

fun panelSettingScreen(index: Int): String? =
    PANEL_SETTINGS.getOrNull(index)?.let { com.echo.core.domain.model.settingsEntriesIn(it).firstOrNull()?.id }

enum class PanelTab(val label: String) {
    NOTIFICATIONS("Notifications"),
    PROFILE("Profile"),
    QUICK("Quick settings"),
    LIBRARIES("Libraries"),
    SETTINGS("Settings"),
}

enum class PanelMove { UP, DOWN, LEFT, RIGHT, PREV_TAB, NEXT_TAB }

data class PanelCursor(
    val tab: PanelTab = PanelTab.NOTIFICATIONS,
    val notice: Int = 0,
    val quick: Int = 0,
    val chip: Int = 0,
    val setting: Int = 0,
    val profile: ProfileFocus = ProfileFocus(),
)

fun movePanel(cursor: PanelCursor, move: PanelMove, rows: Int, quicks: Int, chips: Int, settings: Int = PANEL_SETTINGS.size, recents: Int = 0): PanelCursor {
    val tabs = PanelTab.entries
    when (move) {
        PanelMove.PREV_TAB -> return cursor.copy(tab = tabs[(cursor.tab.ordinal - 1 + tabs.size) % tabs.size])
        PanelMove.NEXT_TAB -> return cursor.copy(tab = tabs[(cursor.tab.ordinal + 1) % tabs.size])
        else -> Unit
    }
    return when (cursor.tab) {
        PanelTab.NOTIFICATIONS -> {
            val at = cursor.notice.coerceIn(0, (rows - 1).coerceAtLeast(0))
            when (move) {
                PanelMove.UP -> cursor.copy(notice = (at - 1).coerceAtLeast(0))
                PanelMove.DOWN -> cursor.copy(notice = (at + 1).coerceAtMost((rows - 1).coerceAtLeast(0)))
                else -> cursor
            }
        }
        PanelTab.PROFILE -> cursor.copy(profile = moveProfileFocus(cursor.profile, move, recents))
        PanelTab.QUICK -> when (move) {
            PanelMove.LEFT -> cursor.copy(quick = (cursor.quick - 1).coerceAtLeast(0))
            PanelMove.RIGHT -> cursor.copy(quick = (cursor.quick + 1).coerceAtMost((quicks - 1).coerceAtLeast(0)))
            else -> cursor
        }
        PanelTab.LIBRARIES -> cursor.copy(chip = gridMove(cursor.chip, move, LIBRARY_GRID_COLUMNS, chips))
        PanelTab.SETTINGS -> cursor.copy(setting = gridMove(cursor.setting, move, SETTINGS_GRID_COLUMNS, settings))
    }
}

private fun gridMove(at: Int, move: PanelMove, columns: Int, count: Int): Int {
    val column = at % columns
    return when (move) {
        PanelMove.LEFT -> if (column > 0) at - 1 else at
        PanelMove.RIGHT -> if (column < columns - 1 && at + 1 < count) at + 1 else at
        PanelMove.UP -> if (at - columns >= 0) at - columns else at
        PanelMove.DOWN -> if (at + columns < count) at + columns else at
        else -> at
    }
}

sealed interface NoticeFocus {
    data class Notice(val key: String) : NoticeFocus

    data class Launcher(val id: Long) : NoticeFocus
}

sealed interface PanelEntry {
    val postedAt: Long
    val focus: NoticeFocus

    data class Android(val notice: AndroidNotice) : PanelEntry {
        override val postedAt get() = notice.postedAt
        override val focus get() = NoticeFocus.Notice(notice.key)
    }

    data class Launcher(val toast: SystemToast) : PanelEntry {
        override val postedAt get() = toast.postedAt
        override val focus get() = NoticeFocus.Launcher(toast.id)
    }
}

// kit 11's chips, switched with LB/RB
enum class NoticeChip(val label: String) { ALL("All"), MESSAGES("Messages"), SYSTEM("System") }

// Notification.category values: what a person sent, and what the device itself reports
private val MESSAGE_CATEGORIES = setOf("msg", "email", "call", "social")
private val SYSTEM_CATEGORIES = setOf("sys", "status", "progress", "err", "service")

// ECHO's own notices and Android's are System; messages, mail, calls and social posts are Messages;
// anything else (a calendar reminder, a store update) shows under All only
fun PanelEntry.chips(): Set<NoticeChip> = when (this) {
    is PanelEntry.Launcher -> setOf(NoticeChip.ALL, NoticeChip.SYSTEM)
    is PanelEntry.Android -> when {
        notice.category in MESSAGE_CATEGORIES -> setOf(NoticeChip.ALL, NoticeChip.MESSAGES)
        notice.category in SYSTEM_CATEGORIES || notice.packageName == "android" || notice.packageName.startsWith("com.android.") ->
            setOf(NoticeChip.ALL, NoticeChip.SYSTEM)
        else -> setOf(NoticeChip.ALL)
    }
}

fun panelEntries(android: List<AndroidNotice>, launcher: List<SystemToast>): List<PanelEntry> =
    (android.map { PanelEntry.Android(it) } + launcher.map { PanelEntry.Launcher(it) })
        .sortedByDescending { it.postedAt }

enum class RecentKind { MUSIC, VIDEO, BOOK, GAME, APP }

fun recentKind(item: CrossbarItem): RecentKind = when {
    item.gameId != null -> RecentKind.GAME
    item.type == CrossbarItemType.MUSIC_TRACK || item.type == CrossbarItemType.MUSIC_GROUP -> RecentKind.MUSIC
    item.type == CrossbarItemType.VIDEO_FILE -> RecentKind.VIDEO
    item.type == CrossbarItemType.LIBRARY_BOOK -> RecentKind.BOOK
    else -> RecentKind.APP
}

sealed interface PanelStage {
    data class Music(
        val title: String,
        val artist: String?,
        val album: String?,
        val art: Any?,
        val loaded: Boolean,
        val playing: Boolean,
        val durationMs: Long,
        val app: String? = null,
        val packageName: String? = null,

        // the queue's next title, when the player shares its queue (kit 05, level 2)
        val nextTitle: String? = null,
    ) : PanelStage

    data class Video(val title: String, val detail: String?, val art: String?, val progress: Float?, val progressLabel: String?) : PanelStage

    data class Book(val title: String, val detail: String?, val cover: String?, val progress: Float? = null) : PanelStage

    data class Game(val title: String, val art: String?, val lastPlayedAt: Long?, val playTimeMs: Long) : PanelStage

    data class App(val title: String, val packageName: String?, val art: String?, val lastUsedAt: Long?) : PanelStage

    data class Android(val notice: AndroidNotice) : PanelStage

    data class Launcher(val toast: SystemToast) : PanelStage

    data object Empty : PanelStage
}

fun CrossbarUiState.mediaStage(): PanelStage? {
    val own = musicPlayback.track?.let { track ->
        PanelStage.Music(
            title = track.title ?: track.displayName,
            artist = track.artist,
            album = track.album,
            art = track.artUri,
            loaded = true,
            playing = musicPlayback.isPlaying,
            durationMs = musicPlayback.durationMs.toLong(),
            nextTitle = musicPlayback.upNext.firstOrNull()?.value?.let { it.title ?: it.displayName },
        )
    }
    val external = externalPlayback?.let {
        PanelStage.Music(it.title, it.artist, null, it.art, true, it.playing, it.durationMs, it.appLabel, it.packageName, it.nextTitle)
    }
    return when {
        own?.playing == true -> own
        external?.playing == true -> external
        else -> own ?: external
    } ?: recentStage()
}

fun CrossbarUiState.recentStage(): PanelStage? {
    val top = recentTop ?: return null
    return when (recentKind(top)) {
        RecentKind.MUSIC -> PanelStage.Music(top.title, top.subtitle, null, top.shelfCoverArt, false, false, 0)
        RecentKind.VIDEO -> PanelStage.Video(top.title, top.subtitle, top.shelfCoverArt, top.progressFraction, top.progressLabel)
        RecentKind.BOOK -> PanelStage.Book(top.title, top.subtitle, top.shelfCoverArt, top.progressFraction)
        RecentKind.GAME -> PanelStage.Game(top.title, top.backdropArt.firstOrNull(), recentTopAt, top.totalPlayTimeMillis)
        RecentKind.APP -> PanelStage.App(top.title, top.packageName, top.shelfCoverArt, recentTopAt)
    }
}

fun playbackFraction(positionMs: Long, durationMs: Long): Float? =
    if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else null

fun PanelStage.Music.timeLabel(positionMs: Long): String? =
    (formatDuration(positionMs) + " / " + formatDuration(durationMs))
        .takeIf { loaded && (packageName == null || durationMs > 0) }

fun PanelStage.islandProgress(positionMs: Long): Float? = when (this) {
    is PanelStage.Music -> playbackFraction(positionMs, durationMs)
    is PanelStage.Video -> progress
    is PanelStage.Book -> progress
    else -> null
}

fun CrossbarUiState.panelStage(): PanelStage = when (val focus = focusedNotice) {
    is NoticeFocus.Notice -> androidNotices.firstOrNull { it.key == focus.key }?.let { PanelStage.Android(it) }
    is NoticeFocus.Launcher -> launcherNotices.firstOrNull { it.id == focus.id }?.let { PanelStage.Launcher(it) }
    null -> null
} ?: PanelStage.Empty

val CrossbarUiState.clearableNoticeCount: Int
    get() = androidNotices.count { it.canDismiss } + launcherNotices.size

enum class StageCommand { PLAY_PAUSE, NEXT_TRACK, PREV_TRACK, OPEN_MUSIC, OPEN_APP, LAUNCH_RECENT, OPEN_NOTICE, DISMISS, CLEAR_ALL }

data class StageAction(val button: GamepadAction, val label: String, val command: StageCommand)

fun stageActions(stage: PanelStage, clearable: Int): List<StageAction> = buildList {
    fun a(label: String, command: StageCommand) = add(StageAction(GamepadAction.SELECT, label, command))
    fun x(label: String, command: StageCommand) = add(StageAction(GamepadAction.CHANGE_SORT, label, command))
    fun y(label: String, command: StageCommand) = add(StageAction(GamepadAction.OPEN_CONTEXT_MENU, label, command))
    val clearAll = { if (clearable > 0) y("Clear all $clearable", StageCommand.CLEAR_ALL) }
    // the panel shows notices only; what is playing or was last played is on the orb
    when (stage) {
        is PanelStage.Android -> {
            if (stage.notice.canOpen) a("Open", StageCommand.OPEN_NOTICE)
            if (stage.notice.canDismiss) x("Dismiss", StageCommand.DISMISS)
            clearAll()
        }
        is PanelStage.Launcher -> {
            x("Dismiss", StageCommand.DISMISS)
            clearAll()
        }
        else -> Unit
    }
}
