package com.psplauncher.feature.xmb.viewmodel

import com.psplauncher.core.domain.model.GamepadAction
import com.psplauncher.core.ui.notification.AndroidNotice
import com.psplauncher.core.ui.notification.SystemToast

data class LibraryChip(val id: String, val name: String, val visible: Boolean)

val LIBRARY_CHIP_IDS = listOf(
    com.psplauncher.core.domain.model.BuiltInCategory.GAMES,
    com.psplauncher.core.domain.model.BuiltInCategory.MUSIC,
    com.psplauncher.core.domain.model.BuiltInCategory.VIDEO,
    com.psplauncher.core.domain.model.BuiltInCategory.LIBRARY,
    com.psplauncher.core.domain.model.BuiltInCategory.PHOTO,
    "network",
)

enum class QuickSetting { WAVE, BACKDROP, RECENT_APPS, ANDROID_SETTINGS, LIBRARIES }

val PANEL_QUICK_SETTINGS = listOf(QuickSetting.WAVE, QuickSetting.BACKDROP, QuickSetting.RECENT_APPS, QuickSetting.ANDROID_SETTINGS)

const val LIBRARY_GRID_COLUMNS = 3

const val SETTINGS_GRID_COLUMNS = 3

val PANEL_SETTINGS: List<com.psplauncher.core.domain.model.SettingsSectionId> = com.psplauncher.core.domain.model.SettingsSectionId.entries

fun panelSettingScreen(index: Int): String? =
    PANEL_SETTINGS.getOrNull(index)?.let { com.psplauncher.core.domain.model.settingsEntriesIn(it).firstOrNull()?.id }

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
    data object Media : NoticeFocus

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

fun panelEntries(android: List<AndroidNotice>, launcher: List<SystemToast>): List<PanelEntry> =
    (android.map { PanelEntry.Android(it) } + launcher.map { PanelEntry.Launcher(it) })
        .sortedByDescending { it.postedAt }

enum class RecentKind { MUSIC, VIDEO, BOOK, GAME, APP }

fun recentKind(item: XMBItem): RecentKind = when {
    item.gameId != null -> RecentKind.GAME
    item.type == XMBItemType.MUSIC_TRACK || item.type == XMBItemType.MUSIC_GROUP -> RecentKind.MUSIC
    item.type == XMBItemType.VIDEO_FILE -> RecentKind.VIDEO
    item.type == XMBItemType.LIBRARY_BOOK -> RecentKind.BOOK
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
        val positionMs: Long,
        val durationMs: Long,
        val app: String? = null,
        val packageName: String? = null,
    ) : PanelStage

    data class Video(val title: String, val detail: String?, val art: String?, val progress: Float?, val progressLabel: String?) : PanelStage

    data class Book(val title: String, val detail: String?, val cover: String?) : PanelStage

    data class Game(val title: String, val art: String?, val lastPlayedAt: Long?, val playTimeMs: Long) : PanelStage

    data class App(val title: String, val packageName: String?, val art: String?, val lastUsedAt: Long?) : PanelStage

    data class Android(val notice: AndroidNotice) : PanelStage

    data class Launcher(val toast: SystemToast) : PanelStage

    data object Empty : PanelStage
}

fun XMBUiState.mediaStage(): PanelStage? {
    val own = musicPlayback.track?.let { track ->
        PanelStage.Music(
            title = track.title ?: track.displayName,
            artist = track.artist,
            album = track.album,
            art = track.artUri,
            loaded = true,
            playing = musicPlayback.isPlaying,
            positionMs = musicPlayback.positionMs.toLong(),
            durationMs = musicPlayback.durationMs.toLong(),
        )
    }
    val external = externalPlayback?.let {
        PanelStage.Music(it.title, it.artist, null, it.art, true, it.playing, it.positionMs, it.durationMs, it.appLabel, it.packageName)
    }
    return when {
        own?.playing == true -> own
        external?.playing == true -> external
        else -> own ?: external
    } ?: recentStage()
}

fun XMBUiState.recentStage(): PanelStage? {
    val top = recentTop ?: return null
    return when (recentKind(top)) {
        RecentKind.MUSIC -> PanelStage.Music(top.title, top.subtitle, null, top.shelfCoverArt, false, false, 0, 0)
        RecentKind.VIDEO -> PanelStage.Video(top.title, top.subtitle, top.shelfCoverArt, top.progressFraction, top.progressLabel)
        RecentKind.BOOK -> PanelStage.Book(top.title, top.subtitle, top.shelfCoverArt)
        RecentKind.GAME -> PanelStage.Game(top.title, top.backdropArt.firstOrNull(), recentTopAt, top.totalPlayTimeMillis)
        RecentKind.APP -> PanelStage.App(top.title, top.packageName, top.shelfCoverArt, recentTopAt)
    }
}

val PanelStage.islandProgress: Float?
    get() = when (this) {
        is PanelStage.Music -> if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else null
        is PanelStage.Video -> progress
        else -> null
    }

fun XMBUiState.panelStage(): PanelStage = when (val focus = focusedNotice) {
    NoticeFocus.Media -> mediaStage()
    is NoticeFocus.Notice -> androidNotices.firstOrNull { it.key == focus.key }?.let { PanelStage.Android(it) }
    is NoticeFocus.Launcher -> launcherNotices.firstOrNull { it.id == focus.id }?.let { PanelStage.Launcher(it) }
    null -> null
} ?: PanelStage.Empty

val XMBUiState.clearableNoticeCount: Int
    get() = androidNotices.count { it.canDismiss } + launcherNotices.size

enum class StageCommand { PLAY_PAUSE, NEXT_TRACK, OPEN_MUSIC, OPEN_APP, LAUNCH_RECENT, OPEN_NOTICE, DISMISS, CLEAR_ALL }

data class StageAction(val button: GamepadAction, val label: String, val command: StageCommand)

fun stageActions(stage: PanelStage, clearable: Int): List<StageAction> = buildList {
    fun a(label: String, command: StageCommand) = add(StageAction(GamepadAction.SELECT, label, command))
    fun x(label: String, command: StageCommand) = add(StageAction(GamepadAction.CHANGE_SORT, label, command))
    fun y(label: String, command: StageCommand) = add(StageAction(GamepadAction.OPEN_CONTEXT_MENU, label, command))
    val clearAll = { if (clearable > 0) y("Clear all $clearable", StageCommand.CLEAR_ALL) }
    when (stage) {
        is PanelStage.Music -> if (stage.loaded) {
            a(if (stage.playing) "Pause" else "Play", StageCommand.PLAY_PAUSE)
            x("Next track", StageCommand.NEXT_TRACK)
            if (stage.packageName != null) y("Open ${stage.app ?: "app"}", StageCommand.OPEN_APP) else y("Open Music", StageCommand.OPEN_MUSIC)
        } else {
            a("Play", StageCommand.LAUNCH_RECENT)
        }
        is PanelStage.Video -> a(if (stage.progress != null) "Resume" else "Play", StageCommand.LAUNCH_RECENT)
        is PanelStage.Book -> a("Continue reading", StageCommand.LAUNCH_RECENT)
        is PanelStage.Game -> a("Continue", StageCommand.LAUNCH_RECENT)
        is PanelStage.App -> a("Return to app", StageCommand.LAUNCH_RECENT)
        is PanelStage.Android -> {
            if (stage.notice.canOpen) a("Open", StageCommand.OPEN_NOTICE)
            if (stage.notice.canDismiss) x("Dismiss", StageCommand.DISMISS)
            clearAll()
        }
        is PanelStage.Launcher -> {
            x("Dismiss", StageCommand.DISMISS)
            clearAll()
        }
        PanelStage.Empty -> Unit
    }
}
