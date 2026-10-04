package com.echo.core.domain.model

import android.view.KeyEvent
import kotlinx.serialization.Serializable

@Serializable
data class GamepadBinding(
    val keyCode: Int,
    val action: GamepadAction,
)

@Serializable
data class GamepadMappings(
    val bindings: List<GamepadBinding> = DEFAULT_BINDINGS,
) {
    fun actionFor(keyCode: Int): GamepadAction? =
        bindings.firstOrNull { it.keyCode == keyCode }?.action

    fun iconFor(action: GamepadAction): ControllerIcon? =
        bindings.asSequence()
            .filter { it.action == action }
            .mapNotNull { it.keyCode.toControllerIcon() }
            .firstOrNull()

    fun iconsFor(actions: List<GamepadAction>): List<ControllerIcon> =
        actions.mapNotNull { iconFor(it) }.distinct()
}

val DEFAULT_BINDINGS = listOf(
    GamepadBinding(KeyEvent.KEYCODE_BUTTON_A,      GamepadAction.SELECT),
    GamepadBinding(KeyEvent.KEYCODE_BUTTON_B,      GamepadAction.BACK),
    GamepadBinding(KeyEvent.KEYCODE_BUTTON_X,      GamepadAction.CHANGE_SORT),
    GamepadBinding(KeyEvent.KEYCODE_BUTTON_Y,      GamepadAction.OPEN_SEARCH),
    GamepadBinding(KeyEvent.KEYCODE_DPAD_UP,       GamepadAction.NAVIGATE_UP),
    GamepadBinding(KeyEvent.KEYCODE_DPAD_DOWN,     GamepadAction.NAVIGATE_DOWN),
    GamepadBinding(KeyEvent.KEYCODE_DPAD_LEFT,     GamepadAction.NAVIGATE_LEFT),
    GamepadBinding(KeyEvent.KEYCODE_DPAD_RIGHT,    GamepadAction.NAVIGATE_RIGHT),
    // owner, 2026-10-04: tabs and filters on the triggers; the bumpers take what the triggers did
    // (paging, seeking), and at the crossbar LB is Search and RB is Apps
    GamepadBinding(KeyEvent.KEYCODE_BUTTON_L2,     GamepadAction.PREV_CATEGORY),
    GamepadBinding(KeyEvent.KEYCODE_BUTTON_R2,     GamepadAction.NEXT_CATEGORY),
    GamepadBinding(KeyEvent.KEYCODE_BUTTON_L1,     GamepadAction.PREV_PAGE),
    GamepadBinding(KeyEvent.KEYCODE_BUTTON_R1,     GamepadAction.NEXT_PAGE),
    // the kit's buttons (owner, 2026-10-04): the guide button is Home, the menu button Options, Y search.
    // MODE comes before SELECT so the hint draws Home with the guide glyph, the Echo mark.
    GamepadBinding(KeyEvent.KEYCODE_BUTTON_MODE,   GamepadAction.HOME),
    GamepadBinding(KeyEvent.KEYCODE_BUTTON_START,  GamepadAction.OPEN_CONTEXT_MENU),

    // View is Home too, for systems that keep the guide button for themselves
    GamepadBinding(KeyEvent.KEYCODE_BUTTON_SELECT, GamepadAction.HOME),
    GamepadBinding(KeyEvent.KEYCODE_ENTER,         GamepadAction.SELECT),
    GamepadBinding(KeyEvent.KEYCODE_BACK,          GamepadAction.BACK),
    GamepadBinding(KeyEvent.KEYCODE_DPAD_CENTER,   GamepadAction.SELECT),

    GamepadBinding(KeyEvent.KEYCODE_ESCAPE,        GamepadAction.BACK),
    GamepadBinding(KeyEvent.KEYCODE_TAB,           GamepadAction.OPEN_SEARCH),
    GamepadBinding(KeyEvent.KEYCODE_F2,            GamepadAction.CHANGE_SORT),
    GamepadBinding(KeyEvent.KEYCODE_F3,            GamepadAction.OPEN_CONTEXT_MENU),
    GamepadBinding(KeyEvent.KEYCODE_PAGE_UP,       GamepadAction.PREV_CATEGORY),
    GamepadBinding(KeyEvent.KEYCODE_PAGE_DOWN,     GamepadAction.NEXT_CATEGORY),
)

fun gamepadMappingsFor(
    confirmBack: ConfirmBackLayout,
    xy: XYLayout,
): GamepadMappings {
    val confirmKey = when (confirmBack) {
        ConfirmBackLayout.STANDARD -> KeyEvent.KEYCODE_BUTTON_A
        ConfirmBackLayout.REVERSED -> KeyEvent.KEYCODE_BUTTON_B
    }
    val searchKey = when (xy) {
        XYLayout.STANDARD -> KeyEvent.KEYCODE_BUTTON_Y
        XYLayout.SWAPPED -> KeyEvent.KEYCODE_BUTTON_X
    }
    return GamepadMappings(
        DEFAULT_BINDINGS.map { binding ->
            when (binding.keyCode) {
                KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B -> GamepadBinding(
                    binding.keyCode,
                    if (binding.keyCode == confirmKey) GamepadAction.SELECT else GamepadAction.BACK,
                )
                KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y -> GamepadBinding(
                    binding.keyCode,
                    if (binding.keyCode == searchKey) {
                        GamepadAction.OPEN_SEARCH
                    } else {
                        GamepadAction.CHANGE_SORT
                    },
                )
                else -> binding
            }
        },
    )
}

// mappings saved before the kit's buttons have Options on a face button and Home on Start. Moves
// those three roles to where DEFAULT_BINDINGS has them and keeps every other choice (A/B and X/Y swaps,
// remapped keys). A mapping that already binds the guide button is taken as current.
fun GamepadMappings.withKitButtons(): GamepadMappings = withKitFaceButtons().withTriggerTabs()

// mappings saved before the triggers took the tabs: the bumpers and triggers trade places
private fun GamepadMappings.withTriggerTabs(): GamepadMappings {
    val l1 = actionFor(KeyEvent.KEYCODE_BUTTON_L1)
    if (l1 != GamepadAction.PREV_CATEGORY) return this
    val swap = mapOf(
        KeyEvent.KEYCODE_BUTTON_L1 to KeyEvent.KEYCODE_BUTTON_L2,
        KeyEvent.KEYCODE_BUTTON_R1 to KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_BUTTON_L2 to KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R2 to KeyEvent.KEYCODE_BUTTON_R1,
    )
    return GamepadMappings(bindings.map { b -> swap[b.keyCode]?.let { b.copy(keyCode = it) } ?: b })
}

private fun GamepadMappings.withKitFaceButtons(): GamepadMappings {
    if (bindings.any { it.keyCode == KeyEvent.KEYCODE_BUTTON_MODE }) return this
    val moved = bindings.map { b ->
        when {
            b.keyCode == KeyEvent.KEYCODE_BUTTON_START && b.action == GamepadAction.HOME -> b.copy(action = GamepadAction.OPEN_CONTEXT_MENU)
            b.keyCode == KeyEvent.KEYCODE_BUTTON_SELECT && b.action == GamepadAction.OPEN_SEARCH -> b.copy(action = GamepadAction.HOME)
            (b.keyCode == KeyEvent.KEYCODE_BUTTON_X || b.keyCode == KeyEvent.KEYCODE_BUTTON_Y) &&
                b.action == GamepadAction.OPEN_CONTEXT_MENU -> b.copy(action = GamepadAction.OPEN_SEARCH)
            else -> b
        }
    }
    return GamepadMappings(listOf(GamepadBinding(KeyEvent.KEYCODE_BUTTON_MODE, GamepadAction.HOME)) + moved)
}

fun GamepadAction.displayLabel(): String = when (this) {
    GamepadAction.NAVIGATE_UP       -> "Navigate Up"
    GamepadAction.NAVIGATE_DOWN     -> "Navigate Down"
    GamepadAction.NAVIGATE_LEFT     -> "Navigate Left (Previous Category)"
    GamepadAction.NAVIGATE_RIGHT    -> "Navigate Right (Next Category)"
    GamepadAction.SELECT            -> "Select / Launch"
    GamepadAction.BACK              -> "Back / Close"
    GamepadAction.OPEN_CONTEXT_MENU -> "Options / Context Menu"
    GamepadAction.CHANGE_SORT       -> "Change Sort Order"
    GamepadAction.OPEN_SEARCH       -> "Search Your Libraries"
    GamepadAction.PREV_CATEGORY     -> "Previous Tab / Filter"
    GamepadAction.NEXT_CATEGORY     -> "Next Tab / Filter"
    GamepadAction.PREV_PAGE         -> "Previous Page (Search on the crossbar)"
    GamepadAction.NEXT_PAGE         -> "Next Page (Apps on the crossbar)"
    GamepadAction.HOME              -> "Home (Confirm in pickers)"
}

fun Int.keycodeDisplayName(): String = when (this) {
    KeyEvent.KEYCODE_BUTTON_A      -> "A / Cross"
    KeyEvent.KEYCODE_BUTTON_B      -> "B / Circle"
    KeyEvent.KEYCODE_BUTTON_X      -> "X / Square"
    KeyEvent.KEYCODE_BUTTON_Y      -> "Y / Triangle"
    KeyEvent.KEYCODE_DPAD_UP       -> "D-Pad Up"
    KeyEvent.KEYCODE_DPAD_DOWN     -> "D-Pad Down"
    KeyEvent.KEYCODE_DPAD_LEFT     -> "D-Pad Left"
    KeyEvent.KEYCODE_DPAD_RIGHT    -> "D-Pad Right"
    KeyEvent.KEYCODE_DPAD_CENTER   -> "D-Pad Center"
    KeyEvent.KEYCODE_BUTTON_L1     -> "L1"
    KeyEvent.KEYCODE_BUTTON_R1     -> "R1"
    KeyEvent.KEYCODE_BUTTON_L2     -> "L2"
    KeyEvent.KEYCODE_BUTTON_R2     -> "R2"
    KeyEvent.KEYCODE_BUTTON_START  -> "Start"
    KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
    KeyEvent.KEYCODE_ENTER         -> "Enter"
    KeyEvent.KEYCODE_BACK          -> "Back"
    else                           -> "Key $this"
}
