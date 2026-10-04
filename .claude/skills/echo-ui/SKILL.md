---
name: echo-ui
description: Build or change an ECHO screen with the UI kit in core-ui — the footer and its A action, press-and-hold launches, context menus, panels and waves, design units, and touch versus controller input. Use before writing or restyling any Compose UI in this repo.
---

# The ECHO UI kit

Every screen is built from `core/core-ui`. Do not draw a second version of something the kit has.
Paths below are under `core/core-ui/src/main/kotlin/com/echo/core/ui/`.

## Sizes: design units

- `panelDesignUnits(widthDp, heightDp, density)` (`design/PanelStyle.kt`) gives a `DesignUnits`
  (`design/MediaDesign.kt`) for the screen. Size things with `u.dp(n)` and text with `u.sp(n)`,
  where `n` is in the kit's 1200 x 752 design.
- `u.sp` applies the legibility floor, so text never drops below the kit's minimum.
- Text uses `EchoTextStyle` (`theme/EchoType.kt`). `u.eyebrow()` is the small spaced label above a
  title.

## The footer: `EchoHintBar`

`components/EchoHintBar.kt`. One footer for every screen.

- Left: the actions that are always there (Home, Back). Centre: the A action. Right: what this
  screen adds.
- `items`: `ControllerPromptItem(action, label)` for each hint. `onAction` receives a tapped hint's
  `GamepadAction`.
- `primary = HintAction(action, label, detail, holdMs)`: the A action. With `holdMs = 0` it is the
  bare A orb and a tap acts. With `holdMs > 0` the card rises when the hold starts.
- `secondary`: a second action on the card (Y Resume beside A Play).
- On touch, a `BACK` item draws as a back-arrow button. With a controller it is a glyph hint. A
  screen that can go back must put a `BACK` item in its footer, or touch users are stuck: the system
  bars are hidden.

## Holds

- `Modifier.pressAndHold(holdMs, label, onPressing, onHeld)` (`design/HoldRing.kt`). Launches use
  `LAUNCH_HOLD_MS` (1000).
- Trap: `animateFloatAsState` starts at its target on first composition. A ring composed mid-hold
  shows full. Use an `Animatable` that starts at 0 (see `holdProgress`).
- Trap: a `clickable(enabled = false)` inside a gesture wrapper still takes the finger down, so the
  wrapper never sees the press. Leave the modifier off instead of disabling it.

## Input mode

- `LocalPadPrompts` (`components/ControllerPrompt.kt`) is true when a controller is connected and
  the last input was not touch (`padPromptsShown`). Touch-only UI checks `!LocalPadPrompts.current`.
- Drive a screen on a device with `adb shell input gamepad keyevent KEYCODE_DPAD_DOWN`. Plain
  `input keyevent` and `input tap` count as touch.

## Panels, menus, waves

- `RAIL_PANEL_WIDTH` and `RailPanelFill` (`design/PanelStyle.kt`) are the one definition of the
  side rail that the Recent rail and every context menu share.
- `EchoContextMenuOverlay` (`components/EchoContextMenu.kt`) is the context menu. Its backing art
  comes from `LocalMenuBackdropArt` and its wave from `LocalBackdropWave` (`design/GlowWave.kt`),
  both provided by `CrossbarShell`. Do not pass art per screen.
- `GlowMaskedWave(side)` shows the wave only inside a screen's glow.
- `WaveLayers(style)` draws the wave; the design (PSP, Echo Rings, Echo Arcs) comes from
  `LocalWaveDesign`.

## Settings rows

`SettingsRow` and `SettingsValueRow` (`feature-settings`, `SettingsScaffold.kt`) carry controller
focus and navigation. Inside `LocalSettingsRailUnits` they draw as the kit's rail rows; the setup
wizard sets it. Use them for any list of options instead of a new row.

## See it on a device

- `screenrecord` works. `screencap` has returned black frames on this app's GL surfaces. Record two
  seconds and take a frame with `ffmpeg -ss 1.2 -i in.mp4 -frames:v 1 out.png`.
- Tap by label, never by remembered coordinates: dump with `uiautomator dump` and tap the one node
  whose text matches, in the same command. Refuse when there are zero or several.
- A release build (`com.echo.launcher`) installs beside the debug build. Install it and run
  `pm clear com.echo.launcher` to see a true first run without touching the owner's debug install.
