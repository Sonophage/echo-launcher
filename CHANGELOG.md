# Changelog

Release notes are on the GitHub releases page. This file holds what is on `main` and not yet
released; it becomes the next release's notes.

## Unreleased (since 2.13.4)

- On the crossbar, an app's background is tinted its own colour again. 2.13.4 washed it grey for apps such as Spotify, Stremio and Immich.
- Delete From Device now works on music, video, photo and book files. ECHO kept only read access to media folders, so every delete was refused. A folder added before this update needs relinking once (its menu, Relink Folder) before its files can be deleted.
- Removing the newest game in the library now deletes its art too.
- A game whose file is gone, or that you remove, comes off Pinned and loses its Playing or Backlog mark. Its play history stays.
- DroidDeck's games show up: ECHO reads the .steam files DroidDeck writes into the windows folder. A game you had under a launcher you have since uninstalled, such as GameNative, moves to DroidDeck with its play history.
- A PC game whose launcher is no longer installed is hidden by a STEAM scan, and comes back if you reinstall the launcher.
- A game's menu has Open Folder, which opens its folder in Files. App Info is at the top of an app's menu.
