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
| Keep a file the user can edit | the ECHO folder: the artwork folder the user picked, named ECHO (`EchoFolder`, `ArtworkFolderSetup.adopt`). `Look/` holds the look as files. Never put a password, API key or account detail there |
| Ask for a permission | a row in `feature-settings/.../permissions/AppPermissions.kt`. Setup offers every row that is not granted at install, and `AppPermissionsTest` checks the manifest declares it |

A launch that leaves ECHO is a hold (`holdMsFor`, `LaunchHold`); media that plays inside ECHO acts
at once. Owner decisions about controls are in the handoff; do not re-ask them.

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

Usage access and notification access can need granting again after an install. Record what you
check (see `echo-ui`), and say in the commit what was and was not seen on the device.

## Data safety

An operation that rewrites artwork links or the artwork folder can lose the owner's library. First
pull the database from the device (`run-as com.echo.launcher.debug cat databases/pfp_database`,
with `-wal` and `-shm`), run `pragma integrity_check` on the copy, and record the counts to compare
after. Rewrite links deterministically (`ArtworkLinkRepoint`); do not rescan to repair them, and
never run `ArtworkLinkRepair` while the folder grant is lost: it clears links it cannot read.
