package com.echo.core.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = GamepadActionSerializer::class)
enum class GamepadAction {
    NAVIGATE_UP,
    NAVIGATE_DOWN,
    NAVIGATE_LEFT,
    NAVIGATE_RIGHT,
    SELECT,
    BACK,

    OPEN_CONTEXT_MENU,

    CHANGE_SORT,

    OPEN_SEARCH,
    PREV_CATEGORY,
    NEXT_CATEGORY,
    PREV_PAGE,
    NEXT_PAGE,
    HOME,

    // the island (now playing or last played) and the notifications; owner, 2026-10-07: Select opens the island,
    // Start the notifications (they traded places)
    OPEN_ISLAND,
    OPEN_NOTIFICATIONS,

    // owner, 2026-10-08: either stick click swaps the two screens on a device with a second one
    SWAP_SCREENS,
}

val GamepadAction.isDirectional: Boolean
    get() = this == GamepadAction.NAVIGATE_UP ||
        this == GamepadAction.NAVIGATE_DOWN ||
        this == GamepadAction.NAVIGATE_LEFT ||
        this == GamepadAction.NAVIGATE_RIGHT

private val LEGACY_ACTION_NAMES = mapOf(
    "BUTTON_Y" to GamepadAction.OPEN_CONTEXT_MENU,
    "LONG_PRESS" to GamepadAction.OPEN_CONTEXT_MENU,
    "BUTTON_X" to GamepadAction.CHANGE_SORT,
    "OPEN_TASK_TRAY" to GamepadAction.CHANGE_SORT,
)

fun gamepadActionFromPersistedName(raw: String): GamepadAction? =
    GamepadAction.entries.firstOrNull { it.name == raw } ?: LEGACY_ACTION_NAMES[raw]

object GamepadActionSerializer : KSerializer<GamepadAction> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.echo.GamepadAction", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: GamepadAction) =
        encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): GamepadAction {
        val raw = decoder.decodeString()
        return gamepadActionFromPersistedName(raw)
            ?: throw SerializationException("Unknown GamepadAction '$raw'")
    }
}
