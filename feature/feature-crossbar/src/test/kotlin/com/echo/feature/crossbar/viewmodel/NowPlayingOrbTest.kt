package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingOrbTest {
    private val music = OrbKind.MUSIC
    private val recent = OrbKind.RECENT

    @Test
    fun `the owner's music mapping, left and right skip, X pauses, A expands and collapses`() {
        assertEquals(OrbStep.Transport(StageCommand.PREV_TRACK), orbStep(GamepadAction.NAVIGATE_LEFT, 1, music))
        assertEquals(OrbStep.Transport(StageCommand.NEXT_TRACK), orbStep(GamepadAction.NAVIGATE_RIGHT, 1, music))
        assertEquals(OrbStep.Transport(StageCommand.PLAY_PAUSE), orbStep(GamepadAction.CHANGE_SORT, 1, music))
        assertEquals("A opens level 2", OrbStep.Level(2), orbStep(GamepadAction.SELECT, 1, music))
        assertEquals("A again goes back to level 1", OrbStep.Level(1), orbStep(GamepadAction.SELECT, 2, music))
        assertEquals("R1 is the kit's next-track button at level 2", OrbStep.Transport(StageCommand.NEXT_TRACK), orbStep(GamepadAction.NEXT_CATEGORY, 2, music))
    }

    @Test
    fun `B, down and Start always put the orb back to rest, so the crossbar is one press away`() {
        listOf(music, recent).forEach { kind ->
            assertEquals(OrbStep.Level(0), orbStep(GamepadAction.BACK, 2, kind))
            assertEquals(OrbStep.Level(0), orbStep(GamepadAction.NAVIGATE_DOWN, 1, kind))
            // owner, 2026-10-06: Start brings the controller to the orb, so Start again must give it back
            assertEquals(OrbStep.Level(0), orbStep(GamepadAction.OPEN_ISLAND, 1, kind))
        }
    }

    @Test
    fun `the last-app orb launches on A and ignores the transport buttons`() {
        assertEquals(OrbStep.Launch, orbStep(GamepadAction.SELECT, 1, recent))
        assertEquals(OrbStep.Stay, orbStep(GamepadAction.NAVIGATE_RIGHT, 1, recent))
        assertEquals("X is not pause for an app", OrbStep.RestAndPass, orbStep(GamepadAction.CHANGE_SORT, 1, recent))
    }

    @Test
    fun `a press the orb does not own rests it and passes through`() {
        assertEquals(OrbStep.RestAndPass, orbStep(GamepadAction.HOME, 1, music))
        assertEquals(OrbStep.RestAndPass, orbStep(GamepadAction.OPEN_SEARCH, 1, music))
    }
}
