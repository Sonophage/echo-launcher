# Changelog

Release notes are on the GitHub releases page. This file holds what is on `main` and not yet
released; it becomes the next release's notes.

## Unreleased (since 2.13.3)

### Keeping the library clean

- Every game scan now hides games whose file is gone, not just the Remove Missing one. A game on a card that is out comes back, with its history, when the card is in.
- Re-Scan All (Remove Missing) now deletes those games, with their art and play history, after a confirm. Removing a game anywhere deletes the art ECHO saved for it. Art in your ECHO folder is left alone.
- A rescan of music, video, photos or books clears out the cached album art, thumbnails and covers of files that are gone.
- Uninstalling an app clears what ECHO kept for it: its custom name, its places in columns, where it was hidden, and its Android library entry.

### Recent

- Clear Recent, in any Recent row's menu and in Settings > Display > Last Played, empties the shelf. Pins and play history stay, and anything you use again comes back.
- An app's colour shows more strongly over the wallpaper on Recent and the crossbar.
