package com.echo.core.data.media

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

// review P8 of spec #32, after #28: Open Folder starts the file manager, which is not a game or app, so it opens on
// the Main Screen's display, the second one while Swap has put the Main Screen there. With no display given,
// Android opens it on the first display.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderOpensOnMainScreenTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `a game's folder opens on the Main Screen's display`() {
        FolderOpen.open(app, "/storage/emulated/0/ROMs/psp/game.iso", MAIN_ON_SECOND)

        assertEquals(MAIN_ON_SECOND, launchDisplayOfNextStart())
    }

    @Test
    fun `a photo's or video's folder opens on the Main Screen's display`() {
        FolderOpen.openMediaFolder(
            app, "content://com.android.externalstorage.documents/document/primary%3ADCIM%2FCamera%2Fp.jpg", "DCIM/Camera", "p.jpg", MAIN_ON_SECOND,
        )

        assertEquals(MAIN_ON_SECOND, launchDisplayOfNextStart())
    }

    private fun launchDisplayOfNextStart(): Int? =
        Shadows.shadowOf(app).nextStartedActivityForResult?.options?.getInt(LAUNCH_DISPLAY_ID, -1)
}

private const val MAIN_ON_SECOND = 2

// the key ActivityOptions.setLaunchDisplayId writes into its bundle
private const val LAUNCH_DISPLAY_ID = "android.activity.launchDisplayId"
