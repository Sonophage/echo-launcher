# Changelog

Release notes are on the GitHub releases page. This file holds what is on `main` and not yet
released; it becomes the next release's notes.

## Unreleased (since 2.11.0)

### Games
- Games have a genre: Edit Genre on a game's menu picks one, or keeps what the scrapers found.
- **Group by Genre** on All Games' Options turns the Game column's system folders into genre
  folders (and the App Drawer's Games buttons into genres); **Group by System** turns them back.
- **Show Only <genre>** on a game's menu filters the game lists and the App Drawer's Games;
  **Show All Genres** clears it. The footer names the genre in force.

### Music and books
- Music and books have genres, read from the files on the next scan (a music file's genre tag, an
  EPUB's subject). Music has a **Genres** row next to Artists and Albums; the Library shows one when
  its books have genres.
- **Edit Genre** on a track, a book, or an album in Albums (which sets all its tracks) picks one of
  the library's genres, a new one, or puts back the file's own. Rescans keep what you set.

### App Drawer
- The drawer has **Music** (albums), **Videos** and **Books** sections, each shown when that library has
  something in it. A plays the album, plays the video or opens the book.
- In Music, X steps the buttons through artist, album (A-Z by album name) and genre; in Books,
  author, title and genre. Grouped by genre the buttons show even with one genre.
- A game's last earned achievement badges show in a row under its achievements count.
- The drawer's info column names an album and its artist (a book and its author) and genre, and
  shows a game's achievements under its details.
- Y on an album gives Play Album and Edit Genre (in the drawer and in Music's Albums); on a video
  or a book, its usual menu.

### Photos and look
- Setting a photo as the wallpaper lets you move and zoom it first (d-pad or drag, LT/RT or pinch);
  the wallpaper keeps exactly what the screen showed.
- With the wave off, the selected game's or app's colour still tints the crossbar and Recent.

### Fixes
- Game Info from the App Drawer's Options opens again.
- With a genre filter on, the drawer's system buttons count only that genre.
- A video opened to play shows no page before the player on a single screen.
- Inside a shelf, the strip on the left shows the shelves with their covers instead of a blank card.
- GameBoot's Preview in Settings plays the style you chose (it always played the disc).

### Two screens (AYN Thor)
- With the App Drawer on the second screen, the other screen shows the focused game as Recent
  does: its art and logo, play time, year, genre, developer and achievements.
