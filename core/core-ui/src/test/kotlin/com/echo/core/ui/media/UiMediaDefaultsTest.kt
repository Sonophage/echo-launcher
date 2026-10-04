package com.echo.core.ui.media

import com.echo.core.domain.model.UiMediaSlot
import com.echo.core.ui.sound.MenuSound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UiMediaDefaultsTest {
    @Test fun `the three movement events each have their own slot`() {
        assertEquals(UiMediaSlot.SOUND_SCROLL, MenuSound.SCROLL.slot)
        assertEquals(UiMediaSlot.SOUND_SELECT, MenuSound.SELECT.slot)
        assertEquals(UiMediaSlot.SOUND_SYSTEM_BROWSE, MenuSound.SYSTEM_BROWSE.slot)
    }

    @Test fun `every other event keeps its own slot`() {
        assertEquals(UiMediaSlot.SOUND_BACK, MenuSound.BACK.slot)
        assertEquals(UiMediaSlot.SOUND_CONFIRM, MenuSound.CONFIRM.slot)
        assertEquals(UiMediaSlot.SOUND_ERROR, MenuSound.ERROR.slot)
        assertEquals(UiMediaSlot.SOUND_LAUNCH, MenuSound.LAUNCH.slot)
        assertEquals(UiMediaSlot.SOUND_NOTIFICATION, MenuSound.NOTIFICATION.slot)
    }

    @Test fun `a custom gameboot clip keeps its own track`() {
        assertNull(resolveGameBootAudio("/video.mp4", null))
        assertNull(resolveGameBootAudio("/video.mp4", "/mine.mp3"))
    }

    @Test fun `no custom media means a silent gameboot, since nothing is bundled`() {
        assertNull(resolveGameBootAudio(null, null))
    }

    @Test fun `an assigned gameboot sound plays when there is no clip`() {
        assertEquals("/mine.mp3", resolveGameBootAudio(null, "/mine.mp3"))
    }
}
