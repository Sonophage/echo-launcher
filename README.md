<p align="center"><img src="docs/echo-logo.png" alt="ECHO" width="128"></p>

# ECHO

**Extensible Console Handheld Operator: a controller-first Android home screen in the style of the PSP's XrossMediaBar.**

<p align="center">
  <img src="docs/screenshots/last-played.jpg" alt="ECHO: Last Played, the home shelf, with Skyrim's art filling the screen" width="820">
</p>

<p align="center">
  <a href="https://github.com/Sonophage/platform-selection-portal-launcher/releases/latest"><img src="https://img.shields.io/github/v/release/Sonophage/platform-selection-portal-launcher?label=latest" alt="Latest release"></a>
  &nbsp;·&nbsp; Android 10 or newer
  &nbsp;·&nbsp; Side-loaded APK, not on the Play Store
</p>

ECHO replaces your Android home screen with one crossbar: categories run left to right,
their items run top to bottom. Games from the emulators you already have, Android apps, and your
own music, video, photos and books all live on it, and all of it works from a controller.

It is local-first. There is no account and no telemetry, and it only goes online when you ask it
to fetch artwork or metadata.

> **ECHO is a fork of [PlayFieldPortal](https://github.com/JohnnyCollado/PlayFieldPortal)**
> by JohnnyC96 / JohnnyCollado. Most of its foundation is their work.
> [What the fork changed and removed](#a-fork-of-playfieldportal) is listed below.

---

## Contents

- [A tour](#a-tour)
- [Handhelds and tablets](#handhelds-and-tablets)
- [A fork of PlayFieldPortal](#a-fork-of-playfieldportal)
- [Install](#install)
- [Controls](#controls)
- [Guide](#guide)
- [Privacy](#privacy)
- [Troubleshooting](#troubleshooting)
- [Building from source](#building-from-source)
- [Credits](#credits)
- [License](#license)

---

## A tour

*Shot on 2026-10-01. Home, the crossbar, games, search and the App Drawer are from an AYANEO Pocket
FIT Elite (1920×1080) with a real library, on a development build of 1.20.1. Music, video, photos,
Settings and the confirm prompt are from a 2400×1504 tablet running 1.20.1, with demo files made for
these screenshots. Game artwork, wallpaper art and app icons belong to their owners.*

### Home

The first column is **Last Played**: everything you opened most recently, games, apps, music,
video and books together, newest first. Whatever is focused fills the screen. Press LEFT to bring in
the cover rail.

| | |
|:---:|:---:|
| <img src="docs/screenshots/last-played.jpg" width="420"> | <img src="docs/screenshots/last-played-rail.jpg" width="420"> |
| Last Played | Its cover rail |

### The crossbar

Each category is a column. Media columns list their own library rows, then the apps that belong
to them (YouTube under Video, Spotify under Music), then a **Folders** row for the folders they scan.

| | |
|:---:|:---:|
| <img src="docs/screenshots/col-game.jpg" width="420"> | <img src="docs/screenshots/col-shelves.jpg" width="420"> |
| Emulation (the Game column, renamed): All Games, then one Memory Card per console | Shelves: Playing, Backlog, Recently Added |
| <img src="docs/screenshots/col-music-playing.jpg" width="420"> | <img src="docs/screenshots/col-network.jpg" width="420"> |
| Music, with what is playing at the top | Network: web search, and the apps you put there |
| <img src="docs/screenshots/col-video.jpg" width="420"> | <img src="docs/screenshots/col-photo.jpg" width="420"> |
| Video | Photo |
| <img src="docs/screenshots/app-drawer.jpg" width="420"> | |
| The App Drawer: Recently Used, Apps, Emulators, Games | |

### Games

A focused game takes over the background with its own art and shows its details beside the tile.
**Y / △** opens its menu on the right edge; anything destructive asks twice. **Metadata ▸ Artwork**
opens the Artwork Studio.

| | |
|:---:|:---:|
| <img src="docs/screenshots/game-hover-panel.jpg" width="420"> | <img src="docs/screenshots/game-context-menu.jpg" width="420"> |
| A focused game in All Games | Its menu |
| <img src="docs/screenshots/context-menu-confirm.jpg" width="420"> | <img src="docs/screenshots/artwork-studio.jpg" width="420"> |
| A destructive row asks a second time | The Artwork Studio: background candidates from SteamGridDB |

### Music

Songs, Artists, Albums and Playlists open a fullscreen browser. Opening a track turns it into a
spinning disc wearing its cover, then the player. Music keeps playing in the background, and
**Start** opens a sheet with the transport and your notifications.

| | |
|:---:|:---:|
| <img src="docs/screenshots/music-songs.jpg" width="420"> | <img src="docs/screenshots/music-player.jpg" width="420"> |
| The music browser | The player |
| <img src="docs/screenshots/launch-disc-music.jpg" width="420"> | <img src="docs/screenshots/status-sheet.jpg" width="420"> |
| The launch disc as a track opens | The Start sheet |

### Video

Rows show duration, resolution and size. The built-in player seeks with LEFT and RIGHT, pages
between videos with L1 and R1, and keeps speed, subtitles, audio track and screen mode under
**Options**. You can choose an external player instead.

| | |
|:---:|:---:|
| <img src="docs/screenshots/video-browser.jpg" width="420"> | <img src="docs/screenshots/video-player-controls.jpg" width="420"> |
| The video list | The player |
| <img src="docs/screenshots/video-player-options.jpg" width="420"> | |
| Its options | |

### Photos

A fullscreen viewer that zooms, pans, rotates and pages with L1 and R1. Any photo can become the
launcher's wallpaper, with its EXIF data stripped.

| | |
|:---:|:---:|
| <img src="docs/screenshots/photo-browser.jpg" width="420"> | <img src="docs/screenshots/photo-viewer.jpg" width="420"> |
| The photo list | The viewer |
| <img src="docs/screenshots/photo-viewer-options.jpg" width="420"> | |
| Its options | |

### Search

**Select** searches every library at once: games, apps, music, video, photos and books. Words can
come in any order and partial words match.

<p align="center"><img src="docs/screenshots/search-results-keyboard.jpg" width="420"></p>

### Settings

Five sections: **Overview, Emulators, Look & Feel, System, Setup**.

| | |
|:---:|:---:|
| <img src="docs/screenshots/settings-home.jpg" width="420"> | <img src="docs/screenshots/settings-overview.jpg" width="420"> |
| Settings | Overview: library, artwork and build |
| <img src="docs/screenshots/settings-look-and-feel.jpg" width="420"> | <img src="docs/screenshots/theme-color-scheme.jpg" width="420"> |
| Look & Feel | Colour Scheme, previewed on the live crossbar |
| <img src="docs/screenshots/settings-system.jpg" width="420"> | <img src="docs/screenshots/setup-wizard-welcome.jpg" width="420"> |
| System ▸ Permissions, read from Android rather than assumed | The setup wizard |

---

## Handhelds and tablets

ECHO is developed and tested on two devices: a 1080p handheld (AYANEO Pocket FIT Elite)
and a 2400×1504 tablet. The same build runs on both. Phones and foldables work too.

- **The layout sizes itself to the screen.** Screens are grouped by their smallest width: compact
  (under 600 dp, most handhelds and phones), medium (600 to 839 dp, most tablets) and expanded
  (840 dp and up). Each group keeps its own **Adjust Crossbar Layout** tuning, so tuning the handheld
  never distorts the tablet.
- **Touch works everywhere.** Swipe up and down to move through a column and sideways to change
  category, tap to select and tap again to open, long-press for the options menu, and swipe in
  from the left edge to go back. *Look & Feel ▸ Touch* sets how far a swipe travels per step and
  whether the on-screen Back / App Drawer button shows.
- **A controller is optional on a tablet** and works the same as on a handheld when one is paired.

| | |
|:---:|:---:|
| <img src="docs/screenshots/game-hover-panel.jpg" width="420"> | <img src="docs/screenshots/tablet-game-hover.jpg" width="420"> |
| A focused game on the handheld | The same screen on the tablet |

---

## A fork of PlayFieldPortal

ECHO is a personal fork of **[PlayFieldPortal](https://github.com/JohnnyCollado/PlayFieldPortal)**,
the XMB-style Android launcher by **JohnnyC96 / JohnnyCollado**. The fork split from upstream on
2026-09-18 at `9c8a6ec9`, the last upstream commit in this history, and was renamed from
`com.playfieldportal.launcher` to `com.psplauncher` in `c7aa063a` the same day. It was renamed again,
to ECHO (`com.echo.launcher`), from version 2.0.0. Upstream is still
active and is not merged back. [NOTICE.md](NOTICE.md) covers authorship and why there is no
licence file.

Count each side's commits yourself rather than trusting a number written here:

```sh
git shortlog -sn --all
git log --oneline 9c8a6ec9..HEAD | wc -l   # commits since the fork
```

### What the fork changed

- **Last Played is the home shelf.** Games, apps, music, video and books together, with a cover
  rail and Remove from Recent.
- **A PS3-style wave background**, drawn at 30 fps (20 when idle), optionally over your wallpaper.
- **A launch disc.** What you open becomes a disc wearing its own cover; GameBoot plays for games.
  Every interface sound can be replaced, and there is optional looping menu music.
- **The crossbar takes on the focused game's art and colour**, with a hover panel in place of a
  separate details page.
- **One context menu everywhere**, a rail on the right edge. Destructive rows ask twice.
- **A status strip and a Start sheet** with media transport, device notifications and launcher
  notices.
- **One search page** across every library, with type-to-search.
- **Settings rebuilt** into five sections, with an Overview of art cards, a Permissions screen, and
  eight new settings for behaviour that used to be fixed in code.
- **The setup wizard rebuilt** so each step runs the same code as the matching Settings screen.
- **Media folders managed in place**, from a Folders row on each media column. Music gains Artists
  and Albums (artists are split out of credit strings). Video titles are read from scene-release
  file names, with TMDB posters. Photos get a choice of default viewer.
- **A Library column for books** (EPUB, PDF and CBZ, series, covers, a built-in reader or an app of
  your choice) and a
  **Shelves** column (Favorites, Playing, Completed, Backlog).
- **Artwork is one image per slot.** The Artwork Studio is down to seven tabs, Steam's store is a
  new source, and linked art is copied into app storage so the linked folder can be let go.
- **Input and look**: analog triggers read as axes, tappable button hints, keyboard support,
  coloured controller face buttons, Instrument Sans, and a new category icon set.

### What the fork removed, for simplicity

| Removed | Why | Commit |
|---|---|---|
| **Discord voice** and the Social category | Discord is back in the one build: sign in with a QR code, then see friends and share game activity. Voice and the Social category stay out. | `c7aa063a` |
| **The Goldberg converter**, Local Steam and the Achievements category | RetroAchievements and Steam achievements are back. Only the Goldberg and Local Steam keys are wiped on start; the RetroAchievements and Steam accounts stay. | `e6136d77`, `dc302e3c`, `e0adbb31`, `762c1860` |
| **TheGamesDB** as a scraper source | Owner's request | `81bfc328` |
| **The App Store column** | It held one row, Play Store, which the drawer already lists | `b3dd8044` |
| **The drawer's All Apps tab and grid** | Every other tab already lists every app | `a19b8e04` |
| **Collections** (screen, menu actions, pill and picker) | Gone from the interface. The tables stay, because pinned shortcuts from other launchers still write to them. | `28bc907f` |
| **The Game Details page**, and the "Launch Games Directly" setting | It duplicated the hover panel; Confirm now always launches | `8792a098`, `1956ca71` |
| **Separate Music, Video, Photo and Books settings screens** | Four copies of one screen, replaced by each column's Folders row | `06b96ded` |
| **Per-System Defaults and the ROM Root Access block** | The console's own menu owns its emulator and folders now | `7b332ebd` |
| **Icon display modes** (Box Art, 3D Box, Physical Media) and four Studio tabs | Used by 2 of 153 games | `1aed1d11` |
| **Text Legibility and Font Colour settings** | Text Legibility had no effect; Font Colour was not wanted | `fba80a71` |

---

## Install

**You need** Android 10 (API 29) or newer, and the emulators you want to use, installed
separately; ECHO launches them and does not emulate anything itself. A controller is
recommended, and touch works throughout.

1. Download `ECHO-<version>.apk` from
   [Releases](https://github.com/Sonophage/platform-selection-portal-launcher/releases).
   A release can also carry a `-debug.apk`; that one installs as
   `com.echo.launcher.debug`, beside the normal app rather than over it.
2. Open it on the device, allow installs from that source when Android asks, and tap **Install**.
3. Optional: press **Home**, pick **ECHO**, choose **Always**. Importing shortcuts from other
   launchers needs it to be the default home app.

### First run

A fresh install opens the **setup wizard**. Every step is optional and sets the same thing as the
matching Settings screen. Run it again any time from **Settings ▸ Setup ▸ Setup Wizard**.

1. **Welcome**
2. **Permissions**: each one turns something on; none is required.
3. **ROM folders**: grant a root folder with one subfolder per console (`gba`, `snes`, `psx`, …,
   the ES-DE names). Adding it here scans it straight away and creates a Memory Card for every
   console that has games, including a **Windows** card for PC games.
4. **Music, Video, Photo and Books** folders
5. **Artwork** folder, with an offer to import what is already there
6. **Artwork sources**: SteamGridDB, TMDB, IGDB and a ScreenScraper account, each optional.
   ScreenScraper's developer credentials are built in.
7. **Vita data folder** and **RetroArch**, shown only when those apps are installed
8. **Personalize**: shortcuts into the real theme, sound, boot and layout screens
9. **Finish**

### Permissions

ECHO asks only when a feature needs something. **Settings ▸ System ▸ Permissions** shows
what is granted and opens the screen to grant the rest.

- **Notifications** (Android 13+): scan and artwork progress, and confirming shortcuts that other
  apps try to add.
- **Notification access**: your device notifications in the Start sheet.
- **Usage access**: the App Drawer's Recently Used tab. Android blocks this for side-loaded apps
  until you allow it under *Android Settings ▸ Apps ▸ ECHO ▸ ⋮ ▸ Allow restricted settings*.

Folders are granted one at a time through Android's folder picker. ECHO never asks for
access to all of your storage.

---

## Controls

| Action | Controller | Keyboard | Touch |
|---|---|---|---|
| Move | D-pad / left stick | Arrow keys | Tap |
| Change category | D-pad ◀ ▶ | ◀ ▶ | Tap the category |
| Open / launch | **A / ✕** | Enter | Tap |
| Back | **B / ◯**, or D-pad ◀ | Esc | Back button, edge swipe, swipe left |
| Options menu | **Y / △** | F3 | Long-press |
| Change sort | **X / ▢** | F2 | |
| Drawer tabs, settings sections | **L1 / R1** | Page Up / Page Down | Tap |
| Previous / next page in the Artwork Studio | **L2 / R2** | | |
| Search everything | **Select** | | Magnifier, bottom right |
| Start sheet; confirm in pickers | **Start** | | |

- **B / ◯ at the top level** opens the App Drawer.
- **D-pad ◀ backs out** of a folder, flyout or settings page wherever LEFT is not already doing
  something. Turn it off with *Look & Feel ▸ Controller ▸ Left Backs Out*.
- *Look & Feel ▸ Controller* swaps A/B and X/Y. The bindings themselves are fixed.

---

## Guide

### Categories

The default order is **Last Played, Shelves, Game, Music, Video, Photo, Library, Network,
Settings**. *Look & Feel ▸ Categories* creates your own (gaming for games, non-gaming for apps),
and renames, reorders, hides or deletes them. Built-in categories can be hidden but not deleted.

### Games and consoles

The **Game** column holds **All Games**, **Missing** (only when some game files cannot be found),
one **Memory Card** per console, **Folders** (your ROM roots) and **Search**.

- **Add a console by hand**: *Emulators ▸ Library Manager ▸ Add Console*, choose the platform,
  assign an emulator, scan. The folder is found under your ROM root automatically.
- **Manage a card** from its **△** menu or Library Manager: rename, change emulator, hide, scan,
  update metadata, scrape missing artwork, or remove. ROM files are never deleted.
- **Rescanning**: there is no file watcher. Rescan a card, use Library Manager's **Scan All
  Consoles** or **Re-Scan All (Remove Missing)**, or turn on *Look & Feel ▸ Performance ▸ Rescan On
  Return*, which checks for new and missing games when you come back, at most every five minutes.
  A console whose folder cannot be read is skipped, so an unmounted SD card never empties a library.
- **Android games**: **Find Games** on the Android card's menu, or **△ ▸ Mark as Game** on an app.
- **PC games**: the Windows card's **Import PC Games**.

### Emulators

Installed emulators are detected from a built-in catalog, plus one profile per installed
**RetroArch** core.

| System | Emulators |
|---|---|
| PSP | PPSSPP / PPSSPP Gold |
| PS1 | DuckStation |
| PS2 | NetherSX2 / AetherSX2 |
| GameCube / Wii | Dolphin |
| DS / 3DS | melonDS, DraStic / Azahar, Citra, Lime3DS |
| Switch | Sudachi / Yuzu / Suyu family |
| N64 | Mupen64Plus FZ / AE |
| GB / GBC / GBA | mGBA, My Boy!, GBA.emu, GBC.emu |
| NES / SNES / Genesis / PC Engine / Neo Geo / WonderSwan / Lynx | the `*.emu` family |
| Dreamcast | Flycast, Redream |
| Xbox 360 | X360 Mobile (`.iso`) |
| Anything with a libretro core | RetroArch |

A game launches with, in order: its own override (**△ ▸ Settings ▸ Change Emulator**), then its
card's emulator, then the platform default, then the recommended one. If a launch fails you get a
recovery sheet to retry, change the emulator, or copy a diagnostic. For anything not in the
catalog, *Emulators ▸ Custom Emulators ▸ Add Custom Emulator* detects an app's launch settings and
lets you test-launch a ROM before saving.

### Last Played and Shelves

**Last Played** keeps its order, newest first, and is never sorted.
*Look & Feel ▸ Wallpaper & Text ▸ Last Played Size* sets how many it holds, and **△ ▸ Remove from
Recent** takes one off.

**Shelves** gathers **Favorites**, the play states **Playing**, **Completed** and **Backlog**, and
**Recently Added**. A shelf only shows when it has a game. Set them from a game's **△ ▸ Shelves**.

### Artwork

Art and metadata are fetched only when you ask. Sources are set up in
*Emulators ▸ Scraping Sources*:

- **ScreenScraper**: works without setup; a user account is optional.
- **SteamGridDB**: needs a free API key.
- **IGDB**: needs a client ID and secret.
- **TMDB**: video posters; needs a key.
- **Steam store**: PC games imported from Steam, no key.

*Emulators ▸ Artwork* scrapes everything or only what is missing, sets video snap placement and
delay, toggles **Animated Icons**, and clears the cache.

The **Artwork Studio** (**△ ▸ Metadata ▸ Artwork**) has seven tabs: Tile, Tile Video, Background,
Screenshot, Manual, Preview Video and Logo. Each pulls from ScreenScraper, SteamGridDB, IGDB or a
local file. Preview a candidate, then press **Start** to apply. **△** on a slot crops or
repositions it, restores the previous image, or clears it. Crops keep the untouched original, so
you can re-crop without loss.

**Video snaps**: resting on a game plays its snap, muted and at most 60 seconds, in the tile or
the background. Snaps are skipped under battery saver, low battery or heat.

**The artwork folder** (*Emulators ▸ Artwork ▸ Artwork Folder & Import*) is kept in the ES-DE
`downloaded_media` layout, so other frontends can read it as it is. The same screen imports an
ES-DE media folder and its `gamelist.xml`, and exports for ES-DE.

```text
{Artwork Folder}/
├─ pfp-artwork-library.json     marks the folder as an ECHO library
├─ Import/{Launcher}/           drop another launcher's ES-DE media here to import it
└─ Artwork/{platform}/          covers, miximages, fanart, marquees, screenshots,
                                titlescreens, physicalmedia, 3dboxes, manuals, videos
    └─ pfp/                     launcher-only: tile art, tile snaps, originals, previous versions
```

Imports match files to games by ROM file name, then title, then title without tags. Anything
ambiguous is shown to you rather than guessed, and existing art is never overwritten.

### Music, video, photos and books

Each media column scans one or more folders. Add, rescan, relink or remove them from the column's
**Folders** row; the same row picks the default player, viewer or reader app.

- **Music**: Songs, Artists, Albums and Playlists; a fullscreen player that keeps playing in the
  background, with notification controls.
- **Video**: libraries with thumbnails, Recently Watched, and the built-in player or an external
  app. Resume picks up where you stopped.
- **Photo**: albums, the viewer, and **Set as Wallpaper**. Location data is never read.
- **Library**: EPUB, PDF and CBZ books by series. They open in the built-in reader (two-page or
  single-page, contents, bookmarks, text size and page colour, and it remembers your place), or in
  a reader app chosen from the Folders row.

Each column also lists its apps. Add more with its **Add** row.

### Search

- **Select** (or the magnifier) searches games, apps, music, video, photos and books together.
  Results say which library they came from, and opening one takes you to it.
- The **Search** row at the end of Game, Video, Photo and Library searches only that library.
- **Quick Search** in Network searches the web in your own browser, or opens an address if you
  type one.

### Look & Feel

- **Theme**: 13 colour schemes previewed live, one icon tint across every crossbar glyph (8
  swatches or a custom colour), **Color from Wallpaper**, PSP theme import (`.ptf`), and your saved
  themes as shareable `.pfptheme` files.
- **Wallpaper & Text**: a still or motion wallpaper, the wave and whether it draws over the
  wallpaper, Last Played size, and the status strip.
- **Layout**: **Adjust Crossbar Layout** scales and shifts the crossbar over the live screen, kept
  separately for each screen size. **Customize Crossbar Icons** replaces any of the 42 theme glyphs
  or a console's icon, live.
- **Boot**: the boot sequence, your own boot video, the launch disc, and GameBoot (two seconds, or
  your own video).
- **Sound**: add any interface sound and set looping **Menu Music**. ECHO ships with no sounds;
  the interface is silent until you add some.
- **Categories, Controller, Touch, Performance**.

**Custom icons** can be PNG, JPG, WebP, BMP, HEIC or animated GIF, up to 8 MB (GIFs up to 512 px,
120 frames, 10 s). Animated icons only play on the row you are on. Your picks stay on top when you
change theme; clear one with **△**.

**Motion wallpapers** can be MP4, WebM or animated GIF, up to 1080p, 60 seconds and 60 MB. They
pause during video, behind fullscreen overlays, and on battery saver.

**Sounds** can be MP3, WAV, OGG or M4A:

| Sound | Plays when | Max |
|---|---|---|
| Navigation | Moving the cursor | 0.5 s |
| Select / Open | Opening an item | 0.5 s |
| Category Change | Changing category | 0.5 s |
| Back / Cancel | Backing out | 1 s |
| Confirm / Apply | Committing a choice | 1 s |
| Error / Invalid | A refused launch or import | 1 s |
| Launch Sound | Starting an app | 3 s |
| Notification | Preview only in this build | 2 s |
| Boot Sound | Startup | 10 s |
| Launch Disc Sound | The launch disc | 10 s |
| GameBoot Sound | A game launching | 10 s |

Boot and GameBoot videos can be MP4 or WebM, up to 10 seconds and 25 MB. A GameBoot that has not
finished in time never holds the game back.

### Backup and restore

*System ▸ Backup & Restore* writes your library and settings to a `.pfpbackup` file in a folder you
choose. Android's own cloud backup is off, so this is how you move to a new device. Android does not
carry folder access across, so after a restore relink each folder from its column's **Folders ▸ △ ▸
Relink Folder**.

### Settings map

| Section | Holds |
|---|---|
| **Overview** | Library, artwork and build cards |
| **Emulators** | Library Manager · Artwork · Scraping Sources · Hidden Items · Installed · Custom Emulators · RetroArch |
| **Look & Feel** | Theme · Wallpaper & Text · Layout · Boot · Sound · Categories · Controller · Touch · Performance |
| **System** | Permissions · About · Logs · Backup & Restore · Credits |
| **Setup** | Setup Wizard |

Media folders are not in Settings; they are on each column's **Folders** row.

---

## Privacy

- No account, no analytics, no telemetry. The network is used only for artwork and metadata
  (ScreenScraper, SteamGridDB, IGDB, Steam store, TMDB), always over HTTPS. Release builds trust
  only the system certificate store.
- Your library, settings and artwork stay in app storage. `allowBackup` is off.
- API keys are encrypted with a hardware-backed Android Keystore key. On a device without one the
  key is stored unencrypted, and the app tells you so when you save it.
- Logs are redacted when they are written: no credentials, tokens, account names or emails.
- Shortcuts that other apps try to add must be confirmed by you first.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Home does not open ECHO | *Android Settings ▸ Apps ▸ Default apps ▸ Home app* |
| A console shows no new games | **△ ▸ Scan This Console**, or turn on *Rescan On Return* |
| A game will not launch | Check the emulator is installed, then **△ ▸ Settings ▸ Change Emulator** and the card's **Default Emulator** |
| Games or media went missing after a reinstall or restore | Relink the folder: **Folders ▸ △ ▸ Relink Folder** |
| Artwork will not download | Add a key in *Emulators ▸ Scraping Sources* and check the connection |
| Recently Used is empty | Allow restricted settings for ECHO, then grant usage access (see [Permissions](#permissions)) |
| No notifications in the Start sheet | Grant notification access in *System ▸ Permissions* |
| The interface is too big, small or off-centre | *Look & Feel ▸ Layout ▸ Adjust Crossbar Layout* |

For a bug report, open *System ▸ Logs*, press **△** on a log and choose **Share**. Logs are
redacted.

---

## Building from source

**Stack:** Kotlin, Jetpack Compose, Hilt, Room (hand-written migrations only), DataStore, Ktor,
Media3, Coil, WorkManager, Kotlinx Serialization; JUnit 4, MockK and Turbine for tests. The desktop
Theme Studio is Compose Multiplatform. Exact versions are in `gradle/libs.versions.toml` and the
wrapper.

**You need** a recent Android Studio, JDK 17 for command-line Gradle, and Android SDK 37
(compileSdk 37, targetSdk 35, minSdk 29).

```bash
git clone https://github.com/Sonophage/platform-selection-portal-launcher.git
cd platform-selection-portal-launcher

./gradlew :app:assembleDebug      # debug APK, application id ends in .debug
./gradlew :app:assembleRelease    # release APK, signed if keystore.properties exists
./gradlew test                    # unit tests
./gradlew dist                    # release APK + Theme Studio installer into dist/
./gradlew :studio:run             # run Theme Studio
```

There are no product flavours: one app, `debug` and `release` build types. Release signing reads a
gitignored `keystore.properties` at the repo root:

```properties
storeFile=/absolute/path/to/release.keystore
storePassword=…
keyAlias=…
keyPassword=…
```

Without it, release builds assemble unsigned.

### Modules

Features depend on core, never the other way; `app` wires everything with Hilt.

```
app/                 MainActivity (the HOME activity), application, Hilt module
studio/              Theme Studio, desktop companion (Windows / Linux / macOS)
core/
  theme-kit/         pure-JVM theme core shared with Theme Studio: PSP theme parsers,
                     .pfptheme codec, colour cascade, icon slots, layout, media limits
  core-archive/      bounded ZIP reading for themes, backups and the codec
  core-common/       shared utilities
  core-domain/       models and repository interfaces
  core-data/         Room database, DAOs, DataStore, repositories, migrations, user asset stores
  core-navigation/   pure navigation logic
  core-ui/           theme, wave, icons, motion wallpaper, menu sounds
feature/
  feature-crossbar/  the crossbar, hover panel, players, Artwork Studio, boot
  feature-library/   ROM and media scanners
  feature-launcher/  emulator detection and launching
  feature-artwork/   scrapers, artwork folder, ES-DE import and export
  feature-themes/    theme loading and built-in themes
  feature-settings/  Settings screens
  feature-appbar/    App Drawer and app classification
  feature-backup/    backup and restore
baselineprofile/     startup baseline profile (test only, never shipped)
```

---

## Credits

**Upstream.** PlayFieldPortal by **JohnnyC96 / JohnnyCollado**, which this project is a fork of.

**Interface.** Inspired by Sony's **XMB (XrossMediaBar)** on the PSP and PS3. "XrossMediaBar",
"XMB", "PSP" and "PlayStation" are trademarks of Sony Interactive Entertainment Inc. ECHO is
an independent, non-commercial fan project, not affiliated with or endorsed by Sony, and ships none
of Sony's code, firmware, fonts or audio.

**The wave** is ported from Mart's **[PlayStation-3-XMB](https://github.com/linkev/PlayStation-3-XMB)**.
The strand model and motion are theirs; the shader, colour cascade and wallpaper tinting are this
project's. MIT, Copyright (c) 2025 Mart: [LICENSES/PlayStation-3-XMB-MIT.txt](LICENSES/PlayStation-3-XMB-MIT.txt).

**Typeface.** [Instrument Sans](https://github.com/Instrument/instrument-sans), SIL Open Font
License 1.1: [LICENSES/InstrumentSans-OFL.txt](LICENSES/InstrumentSans-OFL.txt). The reader sets
books in [Literata](https://github.com/googlefonts/literata), also SIL OFL 1.1:
[LICENSES/Literata-OFL.txt](LICENSES/Literata-OFL.txt).

**The book reader** is built on the [Readium Kotlin toolkit](https://github.com/readium/kotlin-toolkit)
(BSD 3-Clause), with PDF pages drawn by PdfiumAndroid.

**App icon and logo** by **johakovi** ([u/silverloc96](https://www.reddit.com/user/silverloc96)),
who volunteered them.

**System and console icons** from **[XMB Menu for ES-DE](https://github.com/anthonycaccese/xmb-menu-es-de)**
by Anthony Caccese, building on InitialDin's original work. All rights remain with them.

**Controller button icons** from **[Zacksly](https://zacksly.itch.io)**'s PS5, Xbox Series and
Switch 2 button packs, under [CC BY 3.0](http://creativecommons.org/licenses/by/3.0/). The files
are unmodified apart from their names.

**Sounds.** No audio is bundled. Every sound is one you add yourself.

**Game artwork and metadata** are fetched on request from ScreenScraper, SteamGridDB, IGDB, the
Steam store and TMDB, and belong to their owners. This product uses the TMDB API but is not endorsed
or certified by TMDB.

If you hold rights to something here and want the credit changed or the asset removed, open an
issue.

---

## License

There is no `LICENSE` file, on purpose: most of this code is not the maintainer's to license. See
[NOTICE.md](NOTICE.md).
