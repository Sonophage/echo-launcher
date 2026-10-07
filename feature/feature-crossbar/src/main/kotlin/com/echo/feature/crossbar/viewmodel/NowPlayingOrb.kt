package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.GamepadAction

// the top-left orb (kit 05): rests as a circle, d-pad up from the top of a list focuses it (level 1), A expands it (level 2)
enum class OrbKind { MUSIC, RECENT }

// music whenever a player holds a track, playing or paused, so X can resume it; otherwise the last app, if the island shows it
fun CrossbarUiState.orbKind(): OrbKind? = when {
    (mediaStage() as? PanelStage.Music)?.loaded == true -> OrbKind.MUSIC
    recentTop != null && interfaceChoices.islandShowsRecent -> OrbKind.RECENT
    else -> null
}

sealed interface OrbStep {
    data class Level(val level: Int) : OrbStep
    data class Transport(val command: StageCommand) : OrbStep
    data object Launch : OrbStep
    data object Stay : OrbStep

    // the press was not for the orb: rest it and let the crossbar have the press
    data object RestAndPass : OrbStep
}

// owner's mapping (2026-10-04): left/right skip, X play/pause, A expands, B or down rests; R1 also skips at level 2.
// owner, 2026-10-06: Start brings the controller to the orb, and Start again rests it
internal fun orbStep(action: GamepadAction, level: Int, kind: OrbKind): OrbStep = when (action) {
    GamepadAction.BACK, GamepadAction.NAVIGATE_DOWN, GamepadAction.OPEN_ISLAND -> OrbStep.Level(0)
    GamepadAction.NAVIGATE_UP -> OrbStep.Stay
    else -> when (kind) {
        OrbKind.MUSIC -> when (action) {
            GamepadAction.NAVIGATE_LEFT -> OrbStep.Transport(StageCommand.PREV_TRACK)
            GamepadAction.NAVIGATE_RIGHT, GamepadAction.NEXT_CATEGORY -> OrbStep.Transport(StageCommand.NEXT_TRACK)
            GamepadAction.CHANGE_SORT -> OrbStep.Transport(StageCommand.PLAY_PAUSE)
            GamepadAction.SELECT -> OrbStep.Level(if (level == 1) 2 else 1)
            else -> OrbStep.RestAndPass
        }
        OrbKind.RECENT -> when (action) {
            GamepadAction.SELECT -> OrbStep.Launch
            GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT -> OrbStep.Stay
            else -> OrbStep.RestAndPass
        }
    }
}
