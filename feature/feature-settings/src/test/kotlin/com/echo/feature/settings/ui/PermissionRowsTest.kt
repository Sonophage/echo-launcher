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
import org.junit.Assert.assertEquals
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
}
