package com.echo.feature.photos

// owner, 2026-10-08: a photo is moved and zoomed to frame it before it becomes the wallpaper, and what the screen
// shows is what is kept. The preview draws the photo as the wallpaper does (filling the screen, cropped), scaled
// by zoom about the centre, then rotated, then moved by pan, in screen pixels
data class WallpaperFrame(
    val imageW: Int,
    val imageH: Int,
    val rotationDegrees: Int,
    val viewW: Float,
    val viewH: Float,
) {
    private val quarterTurned get() = ((rotationDegrees % 360) + 360) % 180 == 90

    // the photo as it stands on screen, after the rotation
    private val rotatedW get() = if (quarterTurned) imageH else imageW
    private val rotatedH get() = if (quarterTurned) imageW else imageH

    // the turned photo fills the screen at zoom 1, as the saved wallpaper will
    fun scale(zoom: Float): Float = maxOf(viewW / rotatedW, viewH / rotatedH) * zoom

    // the image is laid out filling the screen unturned, so the drawing scales it on by this to match scale()
    fun layerScale(zoom: Float): Float = scale(zoom) / maxOf(viewW / imageW, viewH / imageH)

    // how far the photo can move before its edge comes into the frame
    fun panLimitX(zoom: Float): Float = maxOf(0f, (rotatedW * scale(zoom) - viewW) / 2f)
    fun panLimitY(zoom: Float): Float = maxOf(0f, (rotatedH * scale(zoom) - viewH) / 2f)

    // the part of the rotated photo the screen shows, as fractions of its width and height: left, top, right, bottom
    fun crop(zoom: Float, panX: Float, panY: Float): FloatArray {
        val s = scale(zoom)
        val cx = rotatedW / 2f - panX / s
        val cy = rotatedH / 2f - panY / s
        val halfW = viewW / s / 2f
        val halfH = viewH / s / 2f
        return floatArrayOf(
            ((cx - halfW) / rotatedW).coerceIn(0f, 1f),
            ((cy - halfH) / rotatedH).coerceIn(0f, 1f),
            ((cx + halfW) / rotatedW).coerceIn(0f, 1f),
            ((cy + halfH) / rotatedH).coerceIn(0f, 1f),
        )
    }
}
