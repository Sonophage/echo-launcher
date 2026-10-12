# The Main Screen is single-screen ECHO; the Companion Screen only adds to it

On a device with a second display, the Main Screen behaves exactly as ECHO does on a device with
one display, and the Companion Screen only shows more about what the Main Screen has focused,
taking touch only (the keyboard is the one exception). This replaces the dual-screen design of
2026-10-06 to 2026-10-08, in which Recent, the App Drawer, Search, Settings and the music player
left the top screen for the bottom one and a tap moved the controller to the screen tapped. On the
AYN Thor that design worked but had to be fought: the player flipped the screens to make it usable
(owner, 2026-10-12).

## Consequences

- The `(owner, 2026-10-06)` and `(owner, 2026-10-08)` comments that describe the old design are
  superseded and are rewritten with the code that changes.
- The handoff's "the whole dual-screen design: do not re-ask" refers to the old design; this ADR
  replaces it.
- Swap still exchanges the two screens' roles as a whole, and on a device with one display it shows
  the Companion Screen in place of the Main Screen.
