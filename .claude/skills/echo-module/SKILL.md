---
name: echo-module
description: Add, split out, rename or remove a Gradle module in ECHO, and keep settings.gradle.kts, the host dependencies, CLAUDE.md's module map and checkStructure in step. Use before creating a module or moving code between modules.
---

# Modules

## Decide first

- A module is a feature slice: its screens, view models, rules and tests in one place.
- A feature module depends only on `core` modules. Only the hosts, `feature-crossbar` and
  `feature-settings`, may depend on other features, because they embed their screens.
- If the code needs crossbar types (`CrossbarItem`, `CrossbarUiState`, `CrossbarViewModel`), it is
  part of the crossbar, not a slice. Leave it there, or first move the shared type into `core`.
- Check before moving: `grep -hoE "^import com\.echo\.feature\.[a-z]+" <files> | sort | uniq -c`.

## Add a feature module

1. Make `feature/feature-<name>/build.gradle.kts`. Copy `feature/feature-photos/build.gradle.kts`
   and change the namespace to `com.echo.feature.<name>`. Add only the libraries the code imports
   (for example `media3` for video).
2. Put code in `src/main/kotlin/com/echo/feature/<name>/` and tests in
   `src/test/kotlin/com/echo/feature/<name>/`. The package line must match the folder.
3. Add `include(":feature:feature-<name>")` to `settings.gradle.kts`.
4. Add `implementation(project(":feature:feature-<name>"))` to the host that shows its screen.
5. Add a row to the module map in `CLAUDE.md` that says what the module owns.
6. Run `./gradlew checkStructure` and the gate.

When you move files, use `git mv`, then rewrite the package lines and every import of the old
package. `grep -rn "<old.package>"` afterwards must find nothing.

## A pure JVM module

A module with the `kotlin.jvm` plugin runs `test`, not `testDebugUnitTest`. Add `:<path>:test` to
the gate command in `CLAUDE.md`, or its tests never run in the gate.

## Remove a module

Remove the `include`, every `project(...)` dependency on it, and its row in `CLAUDE.md`, in the
same commit. `checkStructure` fails on any of the three left behind.

## Exceptions

`checkStructure` (root `build.gradle.kts`) holds the allowed feature-to-feature edges, each with
the reason it exists. Add one only with the owner's agreement. When the reason goes away, remove
the exception: `checkStructure` fails while an exception names an edge that no longer exists.
