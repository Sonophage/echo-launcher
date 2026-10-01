package com.psplauncher.core.domain.model

enum class TouchSensitivity(val stepScale: Float) {
    VERY_LOW(1.8f),
    LOW(1.35f),
    NORMAL(1f),
    HIGH(0.72f);

    companion object {
        fun fromName(value: String?): TouchSensitivity =
            entries.firstOrNull { it.name == value } ?: NORMAL
    }
}
