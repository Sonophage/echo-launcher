# Changelog

Release notes are on the GitHub releases page. This file holds what is on `main` and not yet
released; it becomes the next release's notes.

## Unreleased (since 2.9.0)

### Library
- "Memory Card" is gone from ECHO: a console's entry is a **system** (Rename System, Remove
  System). Old default names lose "Memory Card" on their own; names you chose are kept.
- The PC system is called **PC** and keeps a name you give it.

### Crossbar
- **Options ▸ Move** lifts a system, an app or a folder row, and **Move Column** a whole column:
  the d-pad carries it, A drops it, B puts it back. The order settings in Settings are gone.
- **Rename Column**, **Change Icon**, **Change Column Icon** and **Change Art** in the same menu.
  Change Art puts your own image or GIF in place of a system's covers, in the row and the fan.
- **Customize Crossbar Icons** is a side rail, and the crossbar stays visible behind it.

### Themes
- Themes are `.echo-theme` files and folders in `ECHO/Themes`, and carry sounds, boot and game-start
  media, the wave, and the button set.
- **Look ▸ Store**: a shelf of online themes (New, Update, Downloaded) and a shelf of your own, led
  by **Your look**. A theme's page sits on its blurred wallpaper, lists its parts with where yours
  come from now, and applies all of them or only the ones you tick. **Your look** opens the same page
  for the look in use, with **Save as Theme**. This replaces Mix.
- A theme that updates in the online store offers **Update**.
- **Remove** asks first, then deletes the theme and its folder in `ECHO/Themes`.
- Applying a theme first saves your look as **Before <theme>** on the device shelf: once, however
  many themes you try, and not when your look is the default.

### Achievements
- The wall starts with the game you played last; Steam games outside your library are placed by
  their newest unlock.
- Bigger, round game tiles with a dot for the source (blue Steam, gold RetroAchievements) and a
  legend in the footer; points and Mastered under the ring; two rows of badges that scroll.

### Controls
- **Start** opens the notifications (held: Home) and **Select** the island; they traded places.
  Saved button maps are updated once.

### Look and feel
- Text prompts (renames, notes, Save as Theme) sit on the side rail like every menu.
- A long menu's title sits below the profile picture.
- App Drawer and Search logos stay readable on small screens.
- The Libraries tab drops its "6 of 6 on the crossbar" line.
- **Button hints** is one setting, All / Minimal / Off, in Controls ▸ Touch and the panel.
- **Game Rows Show** (Cover art / Icons) replaces Card Art Grid in Settings and matches the panel's tile.
- Settings moved: Boot Sound to Boot, the video player to **Library ▸ Video Player**, Rescan On Return
  to Library Manager, and Reset Emulator Configuration onto Emulators ▸ Installed.
- Library Manager asks before removing an app or a file extension.
- Every settings screen uses ECHO's own controls: text fields open the side-rail prompt, Add PC Game is
  a rail menu (Test Launch now reachable by controller), progress shows as rows, and the confirms match.
- The App Drawer's footer matches every other screen's.

### PC games
- **DroidDeck** is a PC launcher: its Steam games launch on its own link, and its exported files
  become games.

### Fixes
- The keyboard shows on the Thor's bottom screen in Swap.
- The Thor's top screen no longer turns grey after a rename prompt is cancelled.
- Custom emulator profiles are no longer lost to an interrupted save or two saves at once.
- Pinning a shortcut to ECHO no longer freezes the screen, and a slow save no longer drops it.
- An achievement's detail card keeps its tier, players and status in view under a long description.
- The boot sound plays to its end.
- Applying a theme with nothing left out applies all of it again.
- Theme files are written the same way every time.
- The Emulators hint names the right place for Library Manager.
- A backup that fails to copy no longer leaves an empty backup to restore.
- A preview clip, album art or wallpaper cut off mid-write is deleted, not kept; an animated
  wallpaper copy stops at the size limit.
- The video player no longer crashes when a rescan shortens its list.
- Clear All Artwork, Clear All Logs and the controller reset ask first.
- Re-Scrape All's confirm says art you picked is kept; Backup's text points at Permissions ▸
  Folders. The fixed Screen Orientation row is gone.
