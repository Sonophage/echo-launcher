# Changelog

Release notes are on the GitHub releases page. This file holds what is on `main` and not yet
released; it becomes the next release's notes.

## Unreleased (since 2.10.0)

### Two screens (AYN Thor)
- **Search** types on one screen and shows its results on the other.
- With the **App Drawer** open on the second screen, the focused app fills the first, and the shelf
  takes the whole second screen.
- **L3 or R3** swaps the two screens.
- Game Info opens on Info when a game has no screenshots or description.
- The theme store shows the theme you're on, large, on the other screen; its shelves get the whole second screen.
- While a game runs, the second screen keeps a session clock: "NOW PLAYING · 12 MIN".
- Music plays on the second screen as a remote (A play/pause, up/down skip) and the crossbar stays free.
- The second screen's Recent follows the crossbar's column: Music shows recent music, games show games.
- A menu opens on the screen that asked for it.
- Quick settings tiles no longer break a word in two; the theme page's text fits the second screen;
  the Info button shows only when there is info.

- On the second screen, B leaves the music player for Recent and Y opens its options (Stop & Close).
- The second screen's Recent can show the selected item in full: right closes the list, left opens it.

### Notifications panel
- What is playing, or the video or book you just had open, is pinned at the top of Notifications and
  the panel opens on it, with its art, progress and buttons (Play/Pause, Next track, Open Music;
  Resume; Continue reading).
- Double-tap the island (or press Select twice) to open the player for whatever is playing.

### Library
- A on a video plays it straight away; Details is on its menu.
- A video can be made the wallpaper: **Set as Wallpaper** on its menu.
- The video wallpaper's motion is its own setting (Wallpaper ▸ Background Motion); turning the wave
  off no longer stops the video.
- Each column's folder row is named for the column: "Music Settings", not "Folders".

### Apps
- An app's Options offer **Artwork** (Artwork Studio, as for games), **App Info** and **Uninstall**.
  The old Edit App Details screen is gone.
- Y on the second screen's Recent opens that item's Options.
- Recent shows an app with no art on its icon's colour, with its icon, instead of black.

### Fixes
- A on a track in Recent plays it, queued with its album.
- Recent no longer crashes when one album was played in two separate runs.
- Recently Added lists games only (an app given art showed as "app_shortcut").
- "Today" means today's date: a game played yesterday afternoon no longer reads "Today" after midnight.
