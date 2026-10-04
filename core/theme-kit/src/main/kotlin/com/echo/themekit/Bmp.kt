package com.echo.themekit

class BmpImage(val width: Int, val height: Int, val argb: IntArray) {
    init {
        require(argb.size == width * height) { "pixel buffer ${argb.size} != ${width}x$height" }
    }

    operator fun get(x: Int, y: Int): Int = argb[y * width + x]
}
