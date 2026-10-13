# ECHO

ECHO is a home-screen launcher for handhelds, driven by a game controller and by touch. This
glossary names the things a player sees and does.

## Language

### Screens

**Main Screen**:
The screen where ECHO runs as it does on a single-screen device, and where the controller acts.
_Avoid_: top screen, primary screen

**Companion Screen**:
The screen that shows more about what the Main Screen has focused, and takes only touch, except
for typing on the keyboard. With nothing new to show, it keeps the last thing it showed. Its
Options button opens the item's menu there (ADR 0002).
_Avoid_: bottom screen, second screen, remote

**Swap**:
Exchanging which display shows the Main Screen and which shows the Companion Screen; on a device
with one display, showing the Companion Screen in place of the Main Screen.
_Avoid_: flip, switch screens

**Launch Screen**:
The display a game or app opens on: the one ECHO last launched it on, or the Main Screen's display
if it has never been launched. DS and 3DS emulators always open on the Main Screen's display.
_Avoid_: launch display, preferred screen

### Navigation

**Crossbar**:
ECHO's home layout: a row of columns, each a list of items, moved through with the controller.
_Avoid_: XMB, cross media bar

**Side Rail**:
The vertical rail of letters or groups at a screen's edge, slid with a finger to jump through a
list.
_Avoid_: letter rail, filter rail, scrubber

**Trigger Filter**:
The word that leads the footer and changes what a screen lists, such as Recent's Games, Apps and
All; tap it or use the triggers to change it.
_Avoid_: filter row, filter rail, tabs, chips

### Item actions

**Details**:
Everything ECHO knows about an item: for a game its metadata and achievements, for an app its
info, for a file its tags and properties.
_Avoid_: Info, Information, View Information, Game Info (Game Info is the screen, not the action)

**Hide**:
Keeping an item out of view without removing it; undone from Hidden Items.
_Avoid_: Remove (when the item stays), Exclude

**Remove**:
Taking an item out of a list or the library; nothing is deleted from the device.
_Avoid_: Delete (when the file stays), Remove permanently

**Delete**:
Erasing an item's file from the device.
_Avoid_: Remove (when the file goes)

**Uninstall**:
Removing an app from the device through Android.
_Avoid_: Delete (for apps)

**Pin to Recent**:
Keeping an item in the list under Recent until it is unpinned.
_Avoid_: Pin (alone)

**Pin to Top**:
Keeping an item first in its column until it is unpinned.
_Avoid_: Pin (alone), Pin to Category

**Favorites**:
The one set of items the player has marked as favourite, for every kind of item.
_Avoid_: Favorites shelf (as a separate set)
