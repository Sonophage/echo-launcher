package com.echo.feature.crossbar.viewmodel

import android.net.Uri
import androidx.datastore.preferences.core.edit
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.move
import com.echo.core.ui.notification.AndroidNotifications
import com.echo.core.ui.sound.MenuSound
import com.echo.feature.crossbar.ui.detail.DetailPanelPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CrossbarPanel(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    fun openProfile(tab: ProfileTab, gameId: Long? = null, set: Int = 0, fromPanel: Boolean = false) {
        menuSound.play(MenuSound.SELECT)
        uiState.update {
            it.withDrawerAndSearchClosed().copy(
                notificationsOpen = if (fromPanel) false else it.notificationsOpen,
                profile = ProfileState(tab = tab, set = set, openOnGameId = gameId, returnToPanel = fromPanel).withData(it.profileData),
            )
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    internal fun observeProfileData() {
        scope.launch {
            uiState.map { it.notificationsOpen || it.profile != null }.distinctUntilChanged()
                .flatMapLatest { shown -> if (shown) profileData() else kotlinx.coroutines.flow.emptyFlow() }
                .collect { data -> uiState.update { s -> s.copy(profileData = data, profile = s.profile?.withData(data)) } }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun profileData(): Flow<ProfileData> {
        val achievements = combine(vm.achievementController.observeSets(), vm.achievementController.observeAllAchievements()) { sets, all ->
            sets to sets.associate { set -> setKey(set) to all[set.provider to set.providerGameId].orEmpty() }
        }
        val accounts = combine(
            vm.achievementCredentials.raUsernameFlow,
            vm.achievementCredentials.steamId64Flow,
            vm.discordSocial.signedIn,
            vm.discordSocial.user,
            vm.discordSocial.friends,
        ) { ra, steam, signedIn, user, friends ->
            ProfileData(
                raLinked = !ra.isNullOrBlank(),
                steamLinked = !steam.isNullOrBlank(),
                discordSignedIn = signedIn,
                discordUser = user,
                friends = friends,
            )
        }
        val library = combine(
            vm.gameRepository.observeGamesOnlyStats(),
            vm.gameRepository.observeRecentGamesOnly(RECENTLY_PLAYED_COUNT * 8),
        ) { stats, recent -> stats to recentlyPlayed(recent) }
        return combine(
            library,
            achievements,
            vm.achievementController.observeTotals(),
            accounts,
            vm.platformDao.observeAll(),
        ) { (stats, recent), (sets, badges), totals, acc, platforms ->
            val platformOf = platforms.associate { it.id to it.shortName }
            acc.copy(
                games = stats.games,
                playTimeMs = stats.playTimeMs,
                recent = recent,
                totals = totals,
                sets = sets,
                badges = badges,
                platforms = sets.mapNotNull { set -> set.gameId?.let { id -> platformOf[set.platformId]?.let { id to it } } }.toMap(),
            )
        }
    }

    internal fun handleProfileInput(p: ProfileState, action: GamepadAction) {
        when {
            action == GamepadAction.BACK -> closeProfile()
            p.tab == ProfileTab.ACHIEVEMENTS && p.data.sets.isEmpty() && action == GamepadAction.SELECT ->
                openSettingsFromProfile("settings_accounts")
            p.tab == ProfileTab.FRIENDS && !p.data.discordSignedIn && action == GamepadAction.SELECT ->
                openSettingsFromProfile("settings_discord")
            else -> setProfile(stepProfile(p, action))
        }
    }

    private fun setProfile(next: ProfileState) {
        val current = uiState.value.profile ?: return
        if (next == current) {
            vm.gamepadInputHandler.cancelRepeat()
            return
        }
        menuSound.play(MenuSound.SCROLL)
        uiState.update { s -> s.copy(profile = s.profile?.let { next.copy(data = it.data) }) }
    }

    fun onProfileSetTapped(index: Int) {
        uiState.value.profile?.let { setProfile(it.copy(set = index, inGrid = false, badge = 0)) }
    }

    fun onProfileBadgeTapped(index: Int) {
        uiState.value.profile?.let { setProfile(it.copy(inGrid = true, badge = index)) }
    }

    fun onProfileFilterTapped(filter: BadgeFilter) {
        uiState.value.profile?.let { setProfile(it.copy(filter = filter, inGrid = false, badge = 0)) }
    }

    fun onProfileFriendTapped(index: Int) {
        uiState.value.profile?.let { setProfile(it.copy(friend = index)) }
    }

    fun closeProfile() {
        val profile = uiState.value.profile ?: return
        menuSound.play(MenuSound.BACK)
        uiState.update {
            if (profile.returnToPanel) it.copy(profile = null, notificationsOpen = true, panelTab = PanelTab.PROFILE) else it.copy(profile = null)
        }
    }

    internal fun runPanelProfile(action: GamepadAction) {
        val s = uiState.value
        val focus = s.panelProfile
        when (action) {
            GamepadAction.CHANGE_SORT -> onPanelProfileTapped(ProfileSpot.EDIT, 0)
            GamepadAction.SELECT -> onPanelProfileTapped(focus.spot, if (focus.spot == ProfileSpot.SHOWCASE) showcaseSet(s.profileData) else focus.recent)
            else -> Unit
        }
    }

    fun onPanelProfileTapped(spot: ProfileSpot, index: Int) {
        val s = uiState.value
        when (spot) {
            ProfileSpot.EDIT -> {
                menuSound.play(MenuSound.SELECT)
                uiState.update { it.copy(panelTab = PanelTab.PROFILE, panelProfile = it.panelProfile.copy(spot = ProfileSpot.EDIT_NAME)) }
            }
            ProfileSpot.EDIT_NAME -> {
                uiState.update { it.copy(panelProfile = it.panelProfile.copy(spot = ProfileSpot.EDIT)) }
                closeNotifications()
                editProfileName()
            }
            ProfileSpot.EDIT_PICTURE -> {
                uiState.update { it.copy(panelProfile = it.panelProfile.copy(spot = ProfileSpot.EDIT)) }
                pickProfileAvatar()
            }
            ProfileSpot.RECENT -> s.profileData.recent.getOrNull(index)?.let { game ->
                uiState.update { it.copy(panelProfile = ProfileFocus(ProfileSpot.RECENT, index)) }
                closeNotifications()
                vm.gameDetail.onOpenGameInfo(game.toSearchRow(vm.platformCache[game.platformId]?.name))
            }
            ProfileSpot.SHOWCASE -> {
                uiState.update { it.copy(panelProfile = it.panelProfile.copy(spot = ProfileSpot.SHOWCASE)) }
                openProfile(ProfileTab.ACHIEVEMENTS, set = index, fromPanel = true)
            }
            ProfileSpot.FRIENDS -> {
                uiState.update { it.copy(panelProfile = it.panelProfile.copy(spot = ProfileSpot.FRIENDS)) }
                openProfile(ProfileTab.FRIENDS, fromPanel = true)
            }
        }
    }

    private fun openSettingsFromProfile(screenId: String) {
        menuSound.play(MenuSound.SELECT)
        uiState.update {
            it.withSettingsOpen(screenId).copy(settingsReturnTo = null, settingsFromPanel = false)
        }
    }

    fun editProfileName() {
        menuSound.play(MenuSound.SELECT)
        uiState.update {
            it.copy(collectionNameDialog = CollectionNameDialogState(
                title = "Profile name",
                subtitle = "The name your profile shows. It stays on this device.",
                initialText = it.profileName,
                renameProfile = true,
                placeholder = DEFAULT_PROFILE_NAME,
            ))
        }
    }

    fun pickProfileAvatar() {
        menuSound.play(MenuSound.SELECT)
        uiState.update { it.copy(profileAvatarPick = true) }
    }

    fun onProfileAvatarPicked(uri: android.net.Uri?) {
        uiState.update { it.copy(profileAvatarPick = false) }
        if (uri == null) return
        scope.launch(Dispatchers.IO) {
            val dir = java.io.File(vm.context.filesDir, "profile").apply { mkdirs() }
            val dest = java.io.File(dir, "avatar_${System.currentTimeMillis()}.jpg")
            val ok = runCatching {
                vm.context.contentResolver.openInputStream(uri)?.use { input -> dest.outputStream().use { input.copyTo(it) } } != null &&
                    android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        .also { android.graphics.BitmapFactory.decodeFile(dest.absolutePath, it) }.outWidth > 0
            }.getOrDefault(false)
            if (!ok) {
                dest.delete()
                Timber.w("Profile picture %s could not be read", uri)
                return@launch
            }
            vm.context.echoDataStore.edit { it[CrossbarViewModel.KEY_PROFILE_AVATAR] = dest.absolutePath }
            dir.listFiles()?.filter { it != dest }?.forEach { it.delete() }
        }
    }

    internal fun observeProfilePrefs() {
        scope.launch {
            vm.context.echoDataStore.data.collect { prefs ->
                val avatar = prefs[CrossbarViewModel.KEY_PROFILE_AVATAR]?.takeIf { java.io.File(it).exists() }
                uiState.update {
                    it.copy(profileName = prefs[CrossbarViewModel.KEY_PROFILE_NAME]?.ifBlank { null } ?: DEFAULT_PROFILE_NAME, profileAvatar = avatar)
                }
            }
        }
    }

    internal fun openAndroidNotice(key: String) {
        menuSound.play(MenuSound.SELECT)
        if (!AndroidNotifications.open(key)) Timber.i("Notification $key had nothing to open")
    }

    fun onPanelPageTapped(page: DetailPanelPage) = uiState.update {
        it.copy(panelPage = page, panelPageGameId = it.hoverPanelItem?.gameId)
    }

    internal fun observeAndroidNotices() {
        scope.launch {
            combine(
                AndroidNotifications.active,
                uiState.map { it.interfaceChoices.showDeviceNotifications }.distinctUntilChanged(),
            ) { notices, show -> if (show) notices else emptyList() }
                .collect { notices ->
                    uiState.update { it.copy(androidNotices = notices) }
                }
        }
        scope.launch {
            combine(
                AndroidNotifications.playback,
                uiState.map { it.interfaceChoices.showDeviceNotifications }.distinctUntilChanged(),
            ) { playback, show -> playback?.takeIf { show } }
                .collect { playback ->
                    uiState.update { it.copy(externalPlayback = playback) }
                }
        }
    }

    private var noticeCardJob: kotlinx.coroutines.Job? = null

    // owner, 2026-10-05: Home (Guide or View) and a tap on the notification island do the same thing: the first
    // press brings the newest notification out as the card, the second opens the panel
    fun pressNoticeIsland() {
        val s = uiState.value
        if (s.notificationsOpen) return toggleNotifications()
        when (noticeIslandPress(cardOut = s.noticeCardOut, hasNotice = s.androidNotices.isNotEmpty())) {
            NoticeIslandPress.SHOW_CARD -> { menuSound.play(MenuSound.SCROLL); showNoticeCard(pinned = true) }
            NoticeIslandPress.OPEN_PANEL -> toggleNotifications()
        }
    }

    // a press brings the card out pinned, to stay until it is closed; a new notification brings it out on its
    // own for a few seconds, unless it is already pinned
    fun showNoticeCard(pinned: Boolean = false) {
        if (!pinned && uiState.value.noticeCardPinned) return
        noticeCardJob?.cancel()
        uiState.update { it.copy(noticeCardOut = true, noticeCardPinned = pinned, noticeCardCursor = 0) }
        if (!pinned) noticeCardJob = scope.launch {
            kotlinx.coroutines.delay(NOTICE_CARD_MS)
            closeNoticeCard()
        }
    }

    fun closeNoticeCard() {
        noticeCardJob?.cancel()
        uiState.update { it.copy(noticeCardOut = false, noticeCardPinned = false) }
    }

    // true when the card took the press
    internal fun onNoticeCardButton(action: GamepadAction, state: CrossbarUiState): Boolean {
        val rows = state.noticeCardRows
        return when (val step = noticeCardStep(action, state.noticeCardCursor, rows.size)) {
            is NoticeCardStep.Move -> { menuSound.play(MenuSound.SCROLL); uiState.update { it.copy(noticeCardCursor = step.cursor) }; true }
            is NoticeCardStep.Open -> { openNoticeFromCard(rows[step.index].key); true }
            is NoticeCardStep.Dismiss -> { dismissNoticeFromCard(rows[step.index].key); true }
            NoticeCardStep.Stay -> true
            NoticeCardStep.Close -> { closeNoticeCard(); action == GamepadAction.BACK }
        }
    }

    fun openNoticeFromCard(key: String) {
        closeNoticeCard()
        openAndroidNotice(key)
    }

    fun dismissNoticeFromCard(key: String) {
        val notice = uiState.value.androidNotices.firstOrNull { it.key == key } ?: return
        if (!notice.canDismiss) return
        menuSound.play(MenuSound.BACK)
        AndroidNotifications.dismiss(key)
        // the card stays for the rest, and stays put while the user works through them
        noticeCardJob?.cancel()
        uiState.update {
            val left = it.noticeCardRows.size - 1
            it.copy(noticeCardPinned = true, noticeCardCursor = it.noticeCardCursor.coerceIn(0, (left - 1).coerceAtLeast(0)))
        }
        if (uiState.value.androidNotices.size <= 1) closeNoticeCard()
    }

    fun toggleNotifications() {
        noticeCardJob?.cancel()
        menuSound.play(if (uiState.value.notificationsOpen) MenuSound.BACK else MenuSound.SYSTEM_BROWSE)
        uiState.update {
            it.copy(
                notificationsOpen = !it.notificationsOpen,
                noticeCardOut = false,
                noticeCardPinned = false,
                panelTab = PanelTab.NOTIFICATIONS,
                noticeCursor = 0,
                panelQuick = QuickSetting.WAVE,
                panelProfile = ProfileFocus(),
                panelChip = 0,
                panelSetting = 0,
            )
        }
    }

    fun onNotificationsSwipedOpen() {
        if (!uiState.value.notificationsOpen) toggleNotifications()
    }

    fun onNotificationsSwipedClosed() {
        if (uiState.value.notificationsOpen) toggleNotifications()
    }

    internal fun movePanelCursor(move: PanelMove) {
        val s = uiState.value
        val quicks = quickSettingsFor(s.secondDisplayPresent)
        val before = PanelCursor(s.panelTab, s.noticeCursor, quicks.indexOf(s.panelQuick).coerceAtLeast(0), s.panelChip, s.panelSetting, s.panelProfile)
        val after = movePanel(
            before, move,
            rows = s.noticeFocusables.size,
            quicks = quicks.size,
            chips = s.libraryChips.size,
            recents = s.profileData.recent.size,
        )
        if (after == before) {
            vm.gamepadInputHandler.cancelRepeat()
            return
        }
        menuSound.play(MenuSound.SCROLL)
        uiState.update {
            it.copy(
                panelTab = after.tab,
                noticeCursor = after.notice,
                panelQuick = quicks[after.quick],
                panelChip = after.chip,
                panelSetting = after.setting,
                panelProfile = after.profile,
            )
        }
    }

    fun onPanelSettingTapped(index: Int) {
        uiState.update { it.copy(panelTab = PanelTab.SETTINGS, panelSetting = index) }
        openPanelSetting(index)
    }

    internal fun openPanelSetting(index: Int) {
        val screenId = panelSettingScreen(index) ?: return
        menuSound.play(MenuSound.SELECT)
        uiState.update {
            it.withSettingsOpen(screenId).copy(
                notificationsOpen = false,
                settingsReturnTo = null,
                settingsFromPanel = true,
            )
        }
    }

    internal fun returnToPanelSettings() {
        uiState.update { it.withSettingsClosed().copy(notificationsOpen = true, panelTab = PanelTab.SETTINGS) }
    }

    fun onPanelTabTapped(tab: PanelTab) {
        if (uiState.value.panelTab == tab) return
        menuSound.play(MenuSound.SCROLL)
        uiState.update { it.copy(panelTab = tab) }
    }

    fun closeNotifications() {
        uiState.update { it.copy(notificationsOpen = false) }
    }

    // owner, 2026-10-05: a tapped notification opens its app at once; the first tap used to only pick it
    fun onPanelRowTapped(focus: NoticeFocus) {
        val index = uiState.value.noticeFocusables.indexOf(focus)
        if (index < 0) return
        vm.markTouchInput()
        uiState.update { it.copy(panelTab = PanelTab.NOTIFICATIONS, noticeCursor = index) }
        vm.runStageButton(GamepadAction.SELECT)
    }

    fun onFocusedNoticeTapped() {
        vm.markTouchInput()
        vm.runStageButton(GamepadAction.SELECT)
    }

    // kit 11: LB/RB step the notifications chips (All, Messages, System)
    fun stepNoticeChip(delta: Int) {
        val chips = NoticeChip.entries
        uiState.update { s -> s.copy(noticeChip = chips[(s.noticeChip.ordinal + delta).mod(chips.size)], noticeCursor = 0) }
        menuSound.play(MenuSound.SCROLL)
    }

    fun onNoticeChipTapped(chip: NoticeChip) {
        vm.markTouchInput()
        if (uiState.value.noticeChip == chip) return
        uiState.update { it.copy(noticeChip = chip, noticeCursor = 0) }
        menuSound.play(MenuSound.SCROLL)
    }

    internal fun onButton(action: GamepadAction, state: CrossbarUiState) {
        when (action) {
            GamepadAction.NAVIGATE_UP   -> movePanelCursor(PanelMove.UP)
            GamepadAction.NAVIGATE_DOWN -> movePanelCursor(PanelMove.DOWN)
            GamepadAction.NAVIGATE_LEFT  -> movePanelCursor(PanelMove.LEFT)
            GamepadAction.NAVIGATE_RIGHT -> movePanelCursor(PanelMove.RIGHT)
            GamepadAction.PREV_CATEGORY -> movePanelCursor(PanelMove.PREV_TAB)
            GamepadAction.NEXT_CATEGORY -> movePanelCursor(PanelMove.NEXT_TAB)
            GamepadAction.PREV_PAGE,
            GamepadAction.NEXT_PAGE -> if (state.panelTab == PanelTab.NOTIFICATIONS) stepNoticeChip(if (action == GamepadAction.NEXT_PAGE) 1 else -1)
            GamepadAction.SELECT,
            GamepadAction.CHANGE_SORT,
            GamepadAction.OPEN_SEARCH,
            GamepadAction.OPEN_CONTEXT_MENU -> when (state.panelTab) {
                PanelTab.NOTIFICATIONS -> vm.runStageButton(action)
                PanelTab.PROFILE -> runPanelProfile(action)
                PanelTab.QUICK -> if (action == GamepadAction.SELECT) vm.toggleQuickSetting(state.panelQuick)
                PanelTab.LIBRARIES -> if (action == GamepadAction.SELECT) vm.toggleQuickSetting(QuickSetting.LIBRARIES)
                PanelTab.SETTINGS -> if (action == GamepadAction.SELECT) openPanelSetting(state.panelSetting)
            }
            GamepadAction.BACK,
            GamepadAction.HOME               -> if (action == GamepadAction.BACK && state.panelTab == PanelTab.PROFILE && state.panelProfile.choosing) {
                menuSound.play(MenuSound.BACK)
                uiState.update { it.copy(panelProfile = it.panelProfile.copy(spot = ProfileSpot.EDIT)) }
            } else {
                menuSound.play(MenuSound.BACK)
                closeNotifications()
            }
            else -> Unit
        }
    }
}
