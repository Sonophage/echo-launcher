---
name: echo-feature
description: Add or change a feature in ECHO — where its code goes, how it is wired into the crossbar shell, the controller actions, the footer prompts, settings and permissions, and how it is tested and checked on a device. Use when starting any new feature or behaviour.
---

# Adding a feature

## Where the code goes

1. Its own module, when it depends only on `core` (see the `echo-module` skill).
2. `feature-crossbar`, when it needs the crossbar's state or items.
3. `feature-settings`, for a settings screen.
4. Never `app/`: it has no test source set, so nothing there can be pinned by a test.

## Wiring points

| To do this | Change this |
|---|---|
| Show a full-screen view over the crossbar | a flag in `CrossbarUiState`, and the screen composed in `CrossbarShell` behind that flag |
| React to a controller button | `CrossbarViewModel.dispatchGamepadAction` (the `when` on `GamepadAction`) |
| React to touch the same way | route the touch to the same rule the button uses. A swipe calls `stepCategory`, which applies the d-pad's rail rule through `swipeRailStep`. Do not give touch a separate copy of the rule |
| Show the footer's hints | `promptsFor` in `feature-crossbar/.../viewmodel/HintPrompts.kt` |
| Add a settings screen | an entry in `core-domain/.../model/SettingsCatalog.kt` and a case in `SettingsNavHost.kt` |
| Keep a file the user can edit | the ECHO folder: the artwork folder the user picked, named ECHO (`EchoFolder`, `ArtworkFolderSetup.adopt`). `EchoFolderMirror` writes `settings.json` (only the keys in `EchoSettingsExport.GROUPS`, each with its `Kind`) and copies sounds, icons and the wallpaper into `Look/`, never over a newer file there; `EchoFolderReader` applies them back on start and on Reload, only when newer and different (`EchoFolder.shouldRead`), and in full when a folder is newly linked while ECHO runs, before the mirror writes to it (`EchoFolderMirror.readIfNewlyLinked`). A new setting a user should carry goes in `GROUPS`; `EchoSettingsExportTest` refuses anything that looks like a key, account, path or personal record |
| Show something on the Companion Screen (a second display, such as the AYN Thor's bottom screen) | `CompanionActivity` draws the Companion Screen (`CompanionScreen`) from `CompanionLink`, the one owner of its state, which `CrossbarCompanion` keeps current from the crossbar. What it shows is one pure rule, `companionContent` in `companion/CompanionRule.kt` (tested by `CompanionRuleTest`): the running game, else the item an open menu belongs to, else the hovered item (`mainScreenHover`, the App Drawer's app), else what it showed last. Add a new kind of Companion content there, as a case of `CompanionContent`. Its only controls are Swap, Options on Details and, while a game runs, Resume. Options opens the item's menu on the Companion (ADR 0002) through `openContextMenuFor(item, MenuSlot.COMPANION, place)`: the same openers write the slot they are given and read the item's column and view from `place`, the Main Screen's state when the Companion took the item (`Details.place`, `CompanionOptionsTest`), the Companion's slot is `companionMenu` (never `activeContextMenu`, so the Main Screen neither draws it nor is blocked by it), and `CrossbarMove.menuRows` leaves Edit Layout out of it (`CompanionMenuTest`). A picker or name prompt a row opens follows the slot of its menu: the dispatch hands `slot` to the handler, pickers write `withMenu(slot, …)`, and a prompt opens in that slot's `NamePrompts` (`withPrompts(slot)`): `CrossbarShell` draws `mainPrompts`, `CompanionScreen` draws `companionPrompts`, both through `NamePromptDialogs`, and the controller acts on `mainPrompts` only (`CompanionPromptsTest`). A menu value a row acts on is taken when the menu opens, not read from the Main Screen's focus when it runs (`MenuKeepsItsItemTest`). The Main Screen is single-screen ECHO (ADR 0001); `MainScreenRollbackGuardTest` fails if it changes because a Companion Screen is attached. Settings (with the Theme Store), Search, the music player (`withMusicPlayer`) and every context menu but the Companion's own open on the Main Screen; only Search's keyboard and a Companion prompt's are on the Companion. Swap (`display_swap_screens`) draws the shell on the second display and the Companion in `MainActivity`; the Screens quick setting (`display_second_screen`, offered where a second display exists) turns the second screen off; both windows sit inside `EchoRoot`. The Companion takes touch only, and a tap never moves the controller; it takes keys only while Search or its own prompt types (`companionKeys`), hands key focus back when it closes, and hides the keyboard the Thor keeps on that screen |
| Ask for a permission | a row in `feature-settings/.../permissions/AppPermissions.kt`. Setup offers every row that is not granted at install, and `AppPermissionsTest` checks the manifest declares it |

A launch that leaves ECHO is a hold (`holdMsFor`, `LaunchHold`), and so is any button or pill that
launches the same item (`pillHoldMs`); media that plays inside ECHO acts at once. Owner decisions about controls are in the handoff; do not re-ask them.

## Tests

- Put pure rules in plain functions (like `recentRailStep`, `swipeRailStep`) and test those.
- Touch and layout: Robolectric Compose tests with `createComposeRule()`,
  `@GraphicsMode(NATIVE)`, `@Config(sdk = [34])`. Drive time with `mainClock`
  (`HoldToLaunchTouchTest` is an example).
- Break the code on purpose once and see the named test fail. Say which test in the commit's
  `Verify:` line.

## Check it on a device

The owner's Konker answers over wireless adb. Find it with `adb mdns services` and use its mDNS
name as `-s`; the IP and port change.

```sh
./gradlew :app:assembleDebug && adb -s $D install -r app/build/outputs/apk/debug/app-debug.apk
```

The owner's AYN Thor (two displays, USB) is driven by `tools/thor.sh`: `install`, `key A DPAD_DOWN`
(controller keys), `shot NAME` (both displays, half size, into `build/thor/`) and `ime` (which window
each display's keyboard answers to, whether it is shown, which display has key focus). Run it
before and after a change to the Companion Screen, Swap or typing: those bugs show only on the device.

Usage access and notification access can need granting again after an install. Record what you
check (see `echo-ui`), and say in the commit what was and was not seen on the device.

## Data safety

Never write the database file while ECHO can run. ECHO is the home app, so `am force-stop` does not
stop it: Android starts it again within a second, and a file swapped under the open connection corrupts
the games table (2026-10-07, on the Konker). Run `pm disable-user --user 0 com.echo.launcher.debug`,
check `pidof` is empty, write the file and delete `-wal` and `-shm`, then `pm enable` it. Keep the copy
you pulled until `pragma integrity_check` on the live database says `ok`.

An operation that rewrites artwork links or the artwork folder can lose the owner's library. First
pull the database from the device (`run-as com.echo.launcher.debug cat databases/pfp_database`,
with `-wal` and `-shm`), run `pragma integrity_check` on the copy, and record the counts to compare
after. Rewrite links deterministically (`ArtworkLinkRepoint`); do not rescan to repair them, and
never run `ArtworkLinkRepair` while the folder grant is lost: it clears links it cannot read.
