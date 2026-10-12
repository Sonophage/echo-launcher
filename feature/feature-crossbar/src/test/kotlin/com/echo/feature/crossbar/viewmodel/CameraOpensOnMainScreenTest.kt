package com.echo.feature.crossbar.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

// spec #1, story 10: every ECHO launch uses the resolved display. The camera has no package ECHO knows, so it opens
// on the Main Screen's display, which is the second one while Swap has put the Main Screen there; with no display
// given, Android opens it on the display of whichever window made the launch. Quick Search's browser opens through
// the same CrossbarLauncher.startOnMainScreen.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CameraOpensOnMainScreenTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `the camera opens on the Main Screen's display`() {
        val vm = mockk<CrossbarViewModel>(relaxed = true) {
            every { context } returns app
            every { launchScreens.mainDisplay() } returns MAIN_ON_SECOND
        }
        val launcher = CrossbarLauncher(
            vm, MutableStateFlow(CrossbarUiState()), CoroutineScope(Dispatchers.Unconfined),
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
        )

        launcher.launchCamera()

        val started = Shadows.shadowOf(app).nextStartedActivityForResult
        assertEquals(MAIN_ON_SECOND, started.options?.getInt(LAUNCH_DISPLAY_ID, -1))
    }
}

private const val MAIN_ON_SECOND = 2

// the key ActivityOptions.setLaunchDisplayId writes into its bundle
private const val LAUNCH_DISPLAY_ID = "android.activity.launchDisplayId"
