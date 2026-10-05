package com.echo.core.ui.icons

enum class GameIconStyle(val label: String) {
    PSP_RECTANGLE("PSP Rectangle"),
    CARTRIDGE("Cartridge"),

    // owner, 2026-10-05: a game row can show its cover art in place of its icon (Quick Settings)
    COVER_ART("Cover art"),
}
