# Changelog

Release notes are on the GitHub releases page. This file holds what is on `main` and not yet
released; it becomes the next release's notes.

## Unreleased (since 2.14.0)

- Permissions: an APK installed from a browser or a file manager could not get usage access (Android 15 and
  later) or notification access (Android 13 and later), because Android blocks them as restricted settings. The
  hint in Setup and Settings ▸ Permissions now opens a card with the steps in the order Android needs, with a
  button to App info. It shows only on the Android versions that block each one, and sits under those rows in
  Setup.
- Permissions: usage access opens on ECHO's own switch. When this Android has no such screen, ECHO opens the
  next one it has, and last its App info, where before nothing happened.
- App Drawer: the empty Recently Used page's button reads Grant Access, since it opens Settings ▸ Permissions.
