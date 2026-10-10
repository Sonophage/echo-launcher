package com.echo.feature.crossbar.ui.apppicker

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.echo.core.ui.preview.EchoScreenPreview
import com.echo.feature.crossbar.viewmodel.AppPickerEntry
import com.echo.feature.crossbar.viewmodel.AppPickerState
import com.echo.feature.crossbar.viewmodel.AppPickerTarget
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// owner, 2026-10-09: the picker showed a letter for every app, where the App Drawer shows the app's own icon
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w640dp-h360dp")
class AppPickerIconTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `an installed app's tile shows its icon, not its letter`() {
        val pm = shadowOf(composeRule.activity.packageManager)
        pm.installPackage(PackageInfo().apply {
            packageName = "com.test.quill"
            applicationInfo = ApplicationInfo().apply { packageName = "com.test.quill" }
        })
        pm.setApplicationIcon("com.test.quill", ColorDrawable(android.graphics.Color.RED))
        val state = AppPickerState(
            title = "Add Apps",
            target = AppPickerTarget.AndroidGames(platformId = "android"),
            apps = listOf(AppPickerEntry(packageName = "com.test.quill", label = "Quill")),
        )
        composeRule.setContent {
            EchoScreenPreview {
                AppPickerScreen(
                    state = state,
                    onTileTapped = {}, onTouchBrowse = {}, onHeaderBack = {},
                    onSearchToggle = {}, onSearchChange = {}, onSearchDone = {},
                    onApply = {}, onConfirmRemoval = {}, onCancelRemoval = {},
                )
            }
        }
        // the icon loads off the main thread; until it does, the letter stands in
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Q", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
    }
}
