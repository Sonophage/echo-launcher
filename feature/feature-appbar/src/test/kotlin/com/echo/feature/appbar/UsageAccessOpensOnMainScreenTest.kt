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

// spec #1, story 10: every ECHO launch uses the resolved display. The usage access screen is a system settings
// screen, so it opens on the Main Screen's display, which is the second one while Swap has put the Main Screen
// there; with no display given, Android opens it on the display of whichever window made the launch.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsageAccessOpensOnMainScreenTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `the usage access screen opens on the Main Screen's display`() {
        val launchScreens = mockk<LaunchScreens> { every { mainDisplay() } returns MAIN_ON_SECOND }
        val repo = InstalledAppRepository(app, mockk(relaxed = true), launchScreens)

        repo.openUsageAccessSettings()

        val started = Shadows.shadowOf(app).nextStartedActivityForResult
        assertEquals(MAIN_ON_SECOND, started.options?.getInt(LAUNCH_DISPLAY_ID, -1))
    }
}

private const val MAIN_ON_SECOND = 2

// the key ActivityOptions.setLaunchDisplayId writes into its bundle
private const val LAUNCH_DISPLAY_ID = "android.activity.launchDisplayId"
