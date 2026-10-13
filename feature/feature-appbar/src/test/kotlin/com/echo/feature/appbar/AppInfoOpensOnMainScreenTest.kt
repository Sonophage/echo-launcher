package com.echo.feature.appbar

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.launch.LaunchScreens
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

// #28: app info and the uninstall prompt are Android's screens, not games or apps, so they open on the Main
// Screen's display, the second one while Swap has put the Main Screen there. With no display given, Android
// opens them on the first display.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppInfoOpensOnMainScreenTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val launchScreens = mockk<LaunchScreens> { every { mainDisplay() } returns MAIN_ON_SECOND }
    private val repo = InstalledAppRepository(app, mockk(relaxed = true), launchScreens)

    @Test
    fun `app info opens on the Main Screen's display`() {
        repo.openAppInfo("com.example.game")

        assertEquals(MAIN_ON_SECOND, launchDisplayOfNextStart())
    }

    @Test
    fun `the uninstall prompt opens on the Main Screen's display`() {
        repo.uninstallApp("com.example.game")

        assertEquals(MAIN_ON_SECOND, launchDisplayOfNextStart())
    }

    private fun launchDisplayOfNextStart(): Int? =
        Shadows.shadowOf(app).nextStartedActivityForResult?.options?.getInt(LAUNCH_DISPLAY_ID, -1)
}

private const val MAIN_ON_SECOND = 2

// the key ActivityOptions.setLaunchDisplayId writes into its bundle
private const val LAUNCH_DISPLAY_ID = "android.activity.launchDisplayId"
