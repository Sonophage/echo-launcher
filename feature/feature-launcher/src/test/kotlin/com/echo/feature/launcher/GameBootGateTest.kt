package com.echo.feature.launcher

import com.echo.core.data.repository.GameBootPreferences
import com.echo.core.data.repository.UiMediaStore
import com.echo.core.domain.model.UiMediaSlot
import com.echo.core.ui.media.UiMediaAudioPlayer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameBootGateTest {
    private class Harness(
        val scope: TestScope,
        enabled: Boolean,
        customVideo: String? = null,
        customAudio: String? = null,
        style: com.echo.core.data.repository.GameBootStyle = com.echo.core.data.repository.GameBootStyle.DISC,
    ) {
        val prefs: GameBootPreferences = mockk(relaxed = true) {
            every { gameBootEnabledFlow } returns flowOf(enabled)
            every { styleFlow } returns flowOf(style)
        }
        val store: UiMediaStore = mockk(relaxed = true) {
            every { pathFor(any()) } returns null
            every { pathFor(UiMediaSlot.GAMEBOOT_VIDEO) } returns customVideo
            every { pathFor(UiMediaSlot.GAMEBOOT_AUDIO) } returns customAudio
        }
        val player: UiMediaAudioPlayer = mockk(relaxed = true)

        val gate = GameBootGate(prefs, store, player, scope)
    }

    private fun TestScope.eventually(what: String, timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("Timed out waiting for: $what")
            }
            Thread.sleep(20)
            testScheduler.runCurrent()
        }
    }

    @Test
    fun `switched off returns immediately and never raises a request or plays`() = runTest {
        val h = Harness(this, enabled = false)

        h.gate.awaitPresentation("Crash Bandicoot")

        assertNull(h.gate.active.value, "A disabled gate must never put a presentation on screen")
        assertFalse(h.gate.isActive)
        verify(exactly = 0) { h.player.play(any<String>(), any(), any()) }
    }

    // owner, 2026-10-05: Lens starts the launch sound as the lens opens (2700 ms), not when the disc leaves
    @Test
    fun `with Lens the request carries the style and the sound starts at 2700 ms`() = runTest {
        val h = Harness(this, enabled = true, customAudio = "/data/ui-media/gameboot_audio.mp3", style = com.echo.core.data.repository.GameBootStyle.LENS)

        val awaiting = async { h.gate.awaitPresentation("Skyrim", backdropArt = "bg.png", cardArt = "card.png") }
        eventually("the presentation request is raised") { h.gate.active.value != null }
        val request = assertNotNull(h.gate.active.value)
        assertEquals(com.echo.core.data.repository.GameBootStyle.LENS, request.style)
        assertEquals("bg.png", request.backdropArt)

        advanceTimeBy(com.echo.core.ui.components.LensCeremony.SOUND_MS.toLong() - 1)
        runCurrent()
        verify(exactly = 0) { h.player.play(any<String>(), any(), any()) }
        advanceTimeBy(2)
        runCurrent()
        verify(exactly = 1) { h.player.play(any<String>(), any(), any()) }

        h.gate.onPresentationFinished()
        advanceUntilIdle()
        assertTrue(awaiting.isCompleted)
    }

    // owner, 2026-10-08: Settings' preview played the disc while GameBoot Style was Lens; it asks requestFor now
    @Test
    fun `the preview's request wears the chosen style without raising a presentation`() = runTest {
        val h = Harness(this, enabled = true, style = com.echo.core.data.repository.GameBootStyle.LENS)

        val request = h.gate.requestFor("Preview", "card.png", backdropArt = "bg.png", cardArt = "card.png")

        assertEquals(com.echo.core.data.repository.GameBootStyle.LENS, request.style)
        assertEquals("bg.png", request.backdropArt)
        assertNull(h.gate.active.value, "Asking what a launch would show must not show it")
    }

    @Test
    fun `switched on suspends until the overlay reports back and plays the assigned sound`() = runTest {
        val h = Harness(this, enabled = true, customAudio = "/data/ui-media/gameboot_audio.mp3")

        val awaiting = async { h.gate.awaitPresentation("Crash Bandicoot") }
        eventually("the presentation request is raised") { h.gate.active.value != null }

        val request = assertNotNull(h.gate.active.value, "The overlay should have been asked to present")

        assertEquals("/data/ui-media/gameboot_audio.mp3", request.audioPath)

        verify(exactly = 0) { h.player.play(any<String>(), any(), any()) }

        advanceTimeBy(com.echo.core.ui.components.DiscCeremony.DiscOutStartMs.toLong() + 1)
        runCurrent()
        verify(exactly = 1) { h.player.play(any<String>(), any(), any()) }
        assertTrue(awaiting.isActive, "The launch must still be waiting")

        h.gate.onPresentationFinished()
        advanceUntilIdle()

        assertTrue(awaiting.isCompleted, "The launch must be released by onPresentationFinished")
        assertNotNull(
            h.gate.active.value,
            "The overlay must stay up until it says it has left the screen",
        )

        h.gate.onPresentationDismissed()
        assertNull(h.gate.active.value, "The request must be cleared once the overlay is dismissed")
    }

    @Test
    fun `a custom clip replaces the whole presentation and keeps its own track`() = runTest {
        val h = Harness(this, enabled = true, customVideo = "/data/ui-media/gameboot_video.mp4")

        val awaiting = async { h.gate.awaitPresentation("Crash Bandicoot") }
        eventually("the presentation request is raised") { h.gate.active.value != null }

        val request = assertNotNull(h.gate.active.value)
        assertTrue(request.videoPath == "/data/ui-media/gameboot_video.mp4")

        assertNull(request.audioPath, "A custom clip must keep its own audio track")
        verify(exactly = 0) { h.player.play(any<String>(), any(), any()) }

        h.gate.onPresentationFinished()
        advanceUntilIdle()
        assertTrue(awaiting.isCompleted)
    }

    @Test
    fun `a stalled presentation times out and proceeds with the launch`() = runTest {
        val h = Harness(this, enabled = true)

        val awaiting = async { h.gate.awaitPresentation("Crash Bandicoot") }
        eventually("the presentation request is raised") { h.gate.active.value != null }
        assertTrue(awaiting.isActive)

        advanceTimeBy(GameBootGate.TIMEOUT_MS + 1)
        advanceUntilIdle()

        assertTrue(awaiting.isCompleted)
        awaiting.await()
        assertNull(h.gate.active.value)
    }

    @Test
    fun `a second request while presenting is dropped rather than queued`() = runTest {
        val h = Harness(this, enabled = true)

        val first = async { h.gate.awaitPresentation("Crash Bandicoot") }
        eventually("the presentation request is raised") { h.gate.active.value != null }
        val firstRequest = h.gate.active.value

        launch { h.gate.awaitPresentation("Spyro") }

        testScheduler.runCurrent()

        assertTrue(h.gate.active.value === firstRequest, "The on-screen presentation must not change")

        h.gate.onPresentationFinished()
        advanceUntilIdle()
        assertTrue(first.isCompleted)
    }
}
