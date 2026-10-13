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

`components/EchoHintBar.kt`. One footer for every screen, across the full bottom edge
(`Modifier.align(Alignment.BottomCenter)`, content padded by `HintBarHeight`), never inside a
screen's padded column.

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
- A card or row that launches by touch reports the finger down and up to the view model, which
  runs the same `LaunchHold` as pad A, so the footer card rises and fills (`onRecentCardPress`).
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
  both provided by `CrossbarShell`. Do not pass art per screen. Any side-rail picker is drawn
  through it: `rowBadge` replaces the letter badge (the Colour Scheme picker's swatches).
- A menu row's `group` is its section (`MenuGroup`, `components/Menu.kt`), in this order: Do, About,
  Organize, Edit, Settings, Edit Layout, Remove. Do and About are the top level, then one divider;
  each other section is a submenu, or its row inline when it has one. A destructive row is always
  in Remove. The fold (`rowsShown`) and the divider (`dividerIndex`) are the only places this is
  decided; a builder only names the section. The divider is not a row: a drawn index is a row's index.
- A Favorites row, on any item in any module, takes its words from `favoritesLabel(isFavorite)` in
  `components/Menu.kt`. Do not write the pair again.
- `GlowMaskedWave(side)` shows the wave only inside a screen's glow.
- The focus marks are one definition in `theme/FocusMark.kt`: `LocalFocusStyle` is the theme's style, and
  `focusLook(style, visible, accent)` marks a focused icon: CLASSIC's warm glow, or the HALO, BRACKET or LIFT mark.
  It is the only place that decides glow or mark. Mark a focused icon with it; do not draw a second ring or glow.
- The theme's motion is one definition in `theme/Motion.kt`: `LocalMotion` is the preset and `glide()` its
  settle spring (STILL, reduced motion, snaps). `EchoTheme` provides it and `LocalFocusStyle`: a window's root
  (`EchoRoot` for both screens, the Reader) passes `EchoThemeStore.themeLook`, and a theme inside it keeps them.
  Do not provide them a second time.
- The belly under a finger is `railBelly(touched, rungs)` (`components/LetterRail.kt`): the Side Rail bulges left,
  `RailBellyReach` (48 dp) at the touched row, tapering over the rail's wave. Do not shape a second belly.
- A press draws the same mark: `PressMark` (`theme/PressMark.kt`) is the `LocalIndication` `EchoTheme` provides, so a
  plain `clickable` shows it while the finger is down. Never ask for a Material `ripple()`; `NoRippleTest` fails on one.
  Set `indication = null` only on a scrim or a whole-screen tap. When the tap target is wider than its icon, give the
  clickable an `interactionSource` with `indication = null` and put `Modifier.indication(source, LocalIndication.current)`
  on the icon (the crossbar rows, the footer's A orb). When the controller's focus is the target's own look, not
  an icon mark (a menu row's white bar, a tab's underline, an App Drawer case's lift, the search field's edge), give
  the clickable an `interactionSource` with `indication = null` and draw it focused when
  `source.showsFocus(focused)` is true (`theme/PressMark.kt`); `contextMenuRow` takes that source. A Material
  control (`Switch`, `Slider`, `Checkbox`, `TextButton`, `IconButton`...) draws its own ripple, which is off, not
  `LocalIndication`: pass it an `interactionSource` and put `Modifier.indication(source, LocalIndication.current)`
  on it, or use `showsFocus` when it has its own focus look. `NoRippleTest` fails on a control with no source.
- `WaveLayers(style)` draws the wave; the design (PSP, Echo Rings, Echo Arcs) comes from
  `LocalWaveDesign`. The wave sits just above the backdrop (wallpaper or full-screen art) and below
  icons, panels and words.
- The App Drawer and Search share the shelf room's surfaces in `design/ShelfTexture.kt`: `ShelfRoom`,
  `roomGlow`, `wallStripes`, `vignette`, `filmGrain`, `coverRings` (an app cover's echo rings),
  `CoverSheen` and `sideways()` for text up a spine. Search draws the crossbar's wave behind its shelf.
- An orb that opens a card (the top bar's islands, the footer's A orb) is joined to it with
  `drawIslandNeck` (`design/IslandNeck.kt`), drawn behind both, so the two read as one shape.
- A game's cover is `coverArtOf(iconUri, artworkUri)` (core-domain): the icon slot first.

## Settings rows

`SettingsRow` and `SettingsValueRow` (`feature-settings`, `SettingsScaffold.kt`) carry controller
focus and navigation. Inside `LocalSettingsRailUnits` they draw as the kit's rail rows;
`SettingsScaffold` and the setup wizard set it, so every settings page has them. Use them for any list of options instead of a new row.
A text value is `SettingsTextFieldRow`: a value row whose A opens the rail text prompt (the scaffold draws it;
`isPassword` masks keys). A job in progress is `SettingsProgressRow` (done / total, or Working…), never a raw
progress bar. A confirm is `SettingsConfirmOverlay`, drawn outside the scrolling column.

## See it on a device

- `screenrecord` works. `screencap` has returned black frames on this app's GL surfaces. Record two
  seconds and take the last frame with `ffmpeg -i in.mp4 -update 1 out.png`; a still screen gives a
  recording too short for `-ss`.
- Tap by label, never by remembered coordinates: dump with `uiautomator dump` and tap the one node
  whose text matches, in the same command. Refuse when there are zero or several.
- A release build (`com.echo.launcher`) installs beside the debug build. Install it and run
  `pm clear com.echo.launcher` to see a true first run without touching the owner's debug install.
