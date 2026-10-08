package com.echo.core.data.repository

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test

class InterfacePreferencesTest {
    @Test
    fun `nothing stored reads as today's behaviour, so an upgrade changes nothing`() {
        val choices = InterfacePreferences.read(preferencesOf())
        assertEquals(InterfaceChoices(), choices)
        assertEquals("the shelf held 15 before this was a setting", 15, choices.lastPlayedSize)
        assertEquals("seek was 10 s before this was a setting", 10, choices.videoSeekStepSeconds)
        assertEquals("controls hid after 3.5 s before this was a setting", 3_500, choices.videoControlsHideMs)
    }

    @Test
    fun `a stored number the picker does not offer falls back instead of being used`() {
        val prefs = mutablePreferencesOf().apply {
            this[InterfacePreferences.KEY_LAST_PLAYED_SIZE] = 9_999
            this[InterfacePreferences.KEY_VIDEO_SEEK_STEP_SECONDS] = 0
            this[InterfacePreferences.KEY_VIDEO_CONTROLS_HIDE_MS] = -1
        }
        val choices = InterfacePreferences.read(prefs)
        assertEquals(InterfacePreferences.DEFAULT_LAST_PLAYED_SIZE, choices.lastPlayedSize)
        assertEquals(InterfacePreferences.DEFAULT_VIDEO_SEEK_STEP_SECONDS, choices.videoSeekStepSeconds)
        assertEquals(InterfacePreferences.DEFAULT_VIDEO_CONTROLS_HIDE_MS, choices.videoControlsHideMs)
    }

    @Test
    fun `every default is one of the options its picker offers`() {
        assertEquals(true, InterfacePreferences.DEFAULT_LAST_PLAYED_SIZE in InterfacePreferences.LAST_PLAYED_SIZES)
        assertEquals(true, InterfacePreferences.DEFAULT_VIDEO_SEEK_STEP_SECONDS in InterfacePreferences.VIDEO_SEEK_STEPS_SECONDS)
        assertEquals(true, InterfacePreferences.DEFAULT_VIDEO_CONTROLS_HIDE_MS in InterfacePreferences.VIDEO_CONTROLS_HIDE_MS)
    }

    @Test
    fun `Button hints reads what the two old settings held, so nobody's choice changes`() {
        assertEquals("nothing stored: all hints, as before", ButtonHints.ALL, InterfacePreferences.buttonHints(preferencesOf()))
        val offWasMinimal = preferencesOf(
            InterfacePreferences.KEY_BUTTON_HINTS_ON to false,
            InterfacePreferences.KEY_MINIMAL_HINTS to true,
        )
        assertEquals("hints turned off stay off", ButtonHints.OFF, InterfacePreferences.buttonHints(offWasMinimal))
        assertEquals(ButtonHints.MINIMAL, InterfacePreferences.buttonHints(preferencesOf(InterfacePreferences.KEY_MINIMAL_HINTS to true)))
    }

    @Test
    fun `each Button hints choice writes back as itself, and the panel tile steps through all three`() {
        ButtonHints.entries.forEach { hints ->
            val prefs = mutablePreferencesOf().apply { with(InterfacePreferences) { setButtonHints(hints) } }
            assertEquals(hints, InterfacePreferences.buttonHints(prefs))
        }
        assertEquals(ButtonHints.entries.toSet(), generateSequence(ButtonHints.ALL) { it.next }.take(3).toSet())
        assertEquals(ButtonHints.ALL, ButtonHints.OFF.next)
    }
}
