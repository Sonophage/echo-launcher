package com.echo.feature.crossbar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.echo.core.data.repository.UiMediaStore
import com.echo.core.domain.model.UiMediaSlot
import com.echo.core.ui.components.DiscCeremony
import com.echo.core.ui.media.UiMediaAudioPlayer
import com.echo.core.ui.sound.LocalLaunchDiscCue
import com.echo.core.ui.sound.LocalMenuSounds
import com.echo.core.ui.sound.MenuSoundPlayer
import com.echo.core.ui.theme.EchoTheme

// what every ECHO window is drawn inside: the theme, the menu sounds, the launch disc's sound and the
// controller's prompt style. Both screens use it, so a screen moved to the other display (the swap,
// owner 2026-10-06) keeps its sounds and prompts.
@Composable
fun EchoRoot(
    menuSounds: MenuSoundPlayer,
    uiMediaStore: UiMediaStore,
    uiMediaAudio: UiMediaAudioPlayer,
    content: @Composable () -> Unit,
) {
    EchoTheme {
        CompositionLocalProvider(
            LocalMenuSounds provides { sound -> menuSounds.play(sound) },
            LocalLaunchDiscCue provides {
                uiMediaStore.pathFor(UiMediaSlot.LAUNCH_DISC_AUDIO)?.let { track ->
                    uiMediaAudio.play(uri = track, clipEndMs = DiscCeremony.HandOffMs.toLong(), label = "launch-disc")
                }
            },
        ) {
            ProvideControllerPrompts(content)
        }
    }
}
