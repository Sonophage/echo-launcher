package com.echo.feature.settings.ui

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.echo.core.ui.theme.EchoTheme
import com.echo.feature.settings.permissions.AppPermission
import com.echo.feature.settings.permissions.AppPermissions
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.robolectric.Shadows.shadowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// owner, 2026-10-05: Setup and Settings ▸ Permissions share one set of rows, so they read and act alike
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w800dp-h480dp")
class PermissionRowsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @org.junit.After
    fun unmock() = io.mockk.unmockkAll()

    @Test
    fun `a grantable row asks, and an install-time row has nothing to tap`() {
        val rows = AppPermissions.forSdk(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        val notifications = rows.first { it.manifestName == android.Manifest.permission.POST_NOTIFICATIONS }
        val installTime = rows.first { it.route == com.echo.feature.settings.permissions.GrantRoute.INSTALL_TIME }
        val asked = mutableListOf<AppPermission>()
        composeRule.setContent {
            EchoTheme { Column { AppPermissionRows(listOf(notifications, installTime), readToken = 0, onAsk = { asked += it }) } }
        }

        composeRule.onNodeWithText(notifications.label).performClick()
        composeRule.onNodeWithText(installTime.label).performClick()

        assertEquals("only the grantable row asks", listOf(notifications), asked)
    }

    // on touch a row's sublabel shows only while it has focus, so the steps are a card a tap opens
    @Test
    fun `a tap on the restricted hint shows the steps`() {
        val listener = AppPermissions.ALL.first { it.id == AppPermissions.NOTIFICATION_LISTENER }
        composeRule.setContent {
            var steps by remember { mutableStateOf(false) }
            // the settings host gives an overlay its pad input; this test is outside it
            val overlayInput = remember { mutableStateOf<((com.echo.core.domain.model.GamepadAction) -> Unit)?>(null) }
            androidx.compose.runtime.CompositionLocalProvider(LocalSettingsOverlayInput provides overlayInput) {
                EchoTheme {
                    Column { RestrictedSettingsRow(listOf(listener), readToken = 0) { steps = true } }
                    if (steps) RestrictedSettingsSteps(onDismiss = { steps = false })
                }
            }
        }

        composeRule.onNodeWithText("Blocked as a restricted setting?").performClick()

        composeRule.onNodeWithText(RESTRICTED_STEPS).assertIsDisplayed()
        composeRule.onNodeWithText("Open App info").assertIsDisplayed()
    }

    // a ROM without the usage access screens: the asker goes on to ECHO's App info instead of doing nothing
    @Test
    fun `a missing special-access screen falls back to App info`() {
        val app = composeRule.activity.application
        shadowOf(app).checkActivities(true)
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", app.packageName, null))
        shadowOf(app.packageManager).addResolveInfoForIntent(
            details,
            ResolveInfo().apply { activityInfo = ActivityInfo().apply { packageName = "com.android.settings"; name = "AppInfo" } },
        )
        val usage = AppPermissions.ALL.first { it.id == AppPermissions.USAGE_ACCESS }
        // the Main Screen's options come from Hilt, which this test does not start
        io.mockk.mockkObject(com.echo.core.data.launch.LaunchScreens.Companion)
        io.mockk.every { com.echo.core.data.launch.LaunchScreens.mainScreenOptions(any()) } returns null
        composeRule.setContent {
            val ask = rememberPermissionAsker {}
            Text("ask", Modifier.clickable { ask(usage) })
        }

        composeRule.onNodeWithText("ask").performClick()

        val started = shadowOf(composeRule.activity).nextStartedActivity
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, started?.action)
    }
}
