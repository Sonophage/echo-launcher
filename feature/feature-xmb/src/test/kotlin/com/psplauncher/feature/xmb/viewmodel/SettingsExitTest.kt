package com.psplauncher.feature.xmb.viewmodel

import com.psplauncher.core.domain.model.GamepadAction
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsExitTest {
    @Test fun `leaving settings forgets that it was opened from the panel`() {
        val open = XMBUiState(
            showBootSequence = false,
            activeSettingsScreen = "settings_layout",
            settingsReturnTo = XMBViewModel.INITIAL_SETUP_SCREEN_ID,
            pendingSettingsAction = GamepadAction.SELECT,
            settingsFromPanel = true,
        )
        val closed = open.withSettingsClosed()
        assertNull(closed.activeSettingsScreen)
        assertNull("a stale return address would send a later Back somewhere old", closed.settingsReturnTo)
        assertNull(closed.pendingSettingsAction)
        assertFalse("a stale flag makes a later Back open the notification panel", closed.settingsFromPanel)
    }

    @Test fun `every way out of settings goes through withSettingsClosed`() {
        val root = generateSequence(File(".").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
        val source = File(root, "feature/feature-xmb/src/main/kotlin/com/psplauncher/feature/xmb/viewmodel/XMBViewModel.kt").readText()
        assertEquals(
            "a settings exit that clears the screen by hand will forget to clear settingsFromPanel",
            1,
            Regex("""activeSettingsScreen\s*=\s*null""").findAll(source).count(),
        )
    }

    @Test fun `opening settings closes the overlays that draw above it`() {
        val covered = XMBUiState(
            showBootSequence = false,
            gameInfo = GameInfoState(XMBItem(id = "g", title = "Ico", gameId = 1L)),
            profile = ProfileState(),
        )
        val open = covered.withSettingsOpen("settings_themes")
        assertEquals("settings_themes", open.activeSettingsScreen)
        assertNull("Game info draws above settings, so the screen would open unseen", open.gameInfo)
        assertNull("the Profile draws above settings, so the screen would open unseen", open.profile)
    }

    @Test fun `every way into settings goes through withSettingsOpen`() {
        val root = generateSequence(File(".").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
        val source = File(root, "feature/feature-xmb/src/main/kotlin/com/psplauncher/feature/xmb/viewmodel/XMBViewModel.kt").readText()
        assertEquals(
            "a settings screen opened by hand leaves Game info or the Profile drawn over it",
            1,
            Regex("""activeSettingsScreen\s*=\s*(?![=\s$]|null\b)""").findAll(source).count(),
        )
    }

    @Test fun `opening Game info closes the Profile that draws above it`() {
        val covered = XMBUiState(showBootSequence = false, profile = ProfileState())
        val info = GameInfoState(XMBItem(id = "g", title = "Ico", gameId = 1L))
        val open = covered.withGameInfoOpen(info)
        assertEquals(info, open.gameInfo)
        assertNull("the Profile draws above Game info and takes its input, so Game info would open unseen", open.profile)
    }
}
