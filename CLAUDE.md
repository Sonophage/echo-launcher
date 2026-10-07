# ECHO

ECHO is an Android home-screen launcher for handhelds, written in Kotlin and Jetpack Compose. It is
driven by a game controller and by touch. The app id is `com.echo.launcher`; debug builds are
`com.echo.launcher.debug` and install beside it. `studio/` is Theme Studio, a desktop editor for
`.pfptheme` themes.

Open work and recent decisions are in the handoff, outside this repo:
`~/Documents/repositories/sonophage/the-grid/echo-launcher/HANDOFF.md`.

## The gate

Run this before a commit that changes code. It is the full check.

```sh
./gradlew checkStructure testDebugUnitTest :core:core-archive:test :core:core-navigation:test \
  :core:theme-kit:test :studio:test lintDebug --continue
```

- The four JVM modules run `test`, not `testDebugUnitTest`. Without them the count looks complete
  but is short.
- Count results from `**/build/test-results/**/TEST-*.xml`. Do not trust a summary line.
- `feature-settings` tests can hang (a Robolectric DataStore deadlock). If the gate runs past five
  minutes, get a stack with `jstack` before you stop it.

## Rules the build enforces

`./gradlew checkStructure` fails when one of these is broken. The gate and CI run it.

1. Every module in `settings.gradle.kts` has a `build.gradle.kts`, and every module folder is
   included.
2. The `package` line of every Kotlin file is its folder.
3. A feature module depends only on `core` modules. The exceptions are the two hosts,
   `feature-crossbar` and `feature-settings`, which embed other features' screens, and the
   exceptions listed in `checkStructure` with a reason.
4. The module map below lists every module and only those. When you add, remove or rename a
   module, change the map in the same commit.

The pre-commit hook (`.githooks/pre-commit`) stops a commit that adds or removes an `include(...)`
in `settings.gradle.kts` or a `project(...)` dependency in a `build.gradle.kts` without also
changing `CLAUDE.md` or a skill. Other build edits, such as a version bump, pass. Turn it on once per clone with
`git config core.hooksPath .githooks`.

## Module map

| Module | Owns |
|---|---|
| `:app` | `MainActivity` (the HOME activity), the application class, Hilt wiring. No tests: put logic in a module. |
| `:studio` | Theme Studio, the desktop `.pfptheme` editor (Compose Desktop). |
| `:baselineprofile` | Generates the startup baseline profile. Not shipped. |
| `:core:theme-kit` | Pure JVM. The `.pfptheme` codec, colour cascade, icon slots, crossbar layout, `ArgbImage`, accent and wallpaper metrics. Shared with Studio, so no Android. |
| `:core:core-archive` | Pure JVM. Bounded ZIP reading for themes and backups. |
| `:core:core-common` | Small shared utilities: formatting, logging, keystore secrets. |
| `:core:core-domain` | Models and repository interfaces. |
| `:core:core-data` | Room database, DataStore, repositories, scanners' storage, permissions helpers, the SteamGridDB client and its key. |
| `:core:core-ui` | The UI kit: footer (`EchoHintBar`), context menus, panels, waves, holds, design units, theme. |
| `:core:core-navigation` | Pure JVM navigation logic. |
| `:discord:discord-native` | Discord rich presence through the native SDK. Needs the SDK aar (`tools/fetch-discord-sdk.sh`). |
| `:feature:feature-crossbar` | Host. The crossbar shell, Last Played, the status strip and island, search, music, game info, app detail, the second screen's activity. |
| `:feature:feature-library` | ROM and media scanners. |
| `:feature:feature-launcher` | Emulator detection and launching. |
| `:feature:feature-artwork` | Scrapers (SteamGridDB's client lives in core-data), the artwork folder, ES-DE import and export, the Artwork Studio. |
| `:feature:feature-settings` | Host. Settings screens, the setup wizard, the permissions catalogue. |
| `:feature:feature-appbar` | The app drawer and app classification. |
| `:feature:feature-backup` | Backup and restore. |
| `:feature:feature-reader` | The built-in book reader (Readium). |
| `:feature:feature-achievements` | RetroAchievements and Steam achievements. |
| `:feature:feature-photos` | The photo viewer. |
| `:feature:feature-video` | The video player and video detail screen. |

## Skills

Read the skill before you do the task it covers.

| Skill | Use it to |
|---|---|
| `echo-ui` | build or change a screen with the kit: footer, holds, context menus, panels, touch and controller input |
| `echo-module` | add, split or remove a module |
| `echo-feature` | add a feature: where its code goes and how it is wired into the crossbar or settings |
| `echo-release` | bump the version, build, check and publish a release |

When a change makes a skill wrong, fix the skill in the same commit.

## Conventions

- A decision the owner made is written beside the code as `(owner, YYYY-MM-DD)`.
- Commits follow Conventional Commits. The body says what changed and why, then a `Verify:` line
  that names the test that fails without the change and what was checked on a device.
- A test must fail when the behaviour it guards is broken. Break the code on purpose once and see
  the named test fail.
