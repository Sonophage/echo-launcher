# The Companion Screen acts on its item by touch

ADR 0001 says the Companion Screen only shows more about what the Main Screen has focused. This
decision lets it also act on the item it shows, by touch, through the same menu the Main Screen
uses. The controller still acts only on the Main Screen (owner, 2026-10-12).

## What the Companion Screen does

- Shows the Details of the item the Main Screen has focused, and keeps the last thing it showed
  when there is nothing new.
- Swap, and Resume while a game runs.
- In Settings, shows the focused setting's description and previews themes and colour schemes.
- During Search, shows the keyboard; the d-pad types and B hides it, then closes Search.
- On a device with one display, Swap shows it in place of the Main Screen; B, L3 or R3 returns.
- An **Options** button on its Details opens that item's menu on the Companion Screen. It shows
  whenever an item's Details are shown, while a game runs and in the single-display Swap view.

## The Companion's menu

- It is the same item menu the Main Screen shows for that item, from the same builder: the same
  sections, rows and names.
- Edit Layout is left out: Move and Move Column are crossbar actions on the Main Screen.
- Everything a row opens (confirms, pickers, text prompts) opens on the Companion Screen and is
  finished by touch there.
- The menu stays on the item it opened for when the Main Screen's focus moves; the Main Screen stays
  fully live under the controller.

## Consequences

- ADR 0001 still holds: the Main Screen behaves exactly as single-screen ECHO, and nothing leaves it.
- Game Info and the single-display Swap view stay two screens (owner, 2026-10-12).
