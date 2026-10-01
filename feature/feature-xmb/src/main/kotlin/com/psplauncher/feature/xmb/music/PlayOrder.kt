package com.psplauncher.feature.xmb.music

import kotlin.random.Random

enum class RepeatMode {
    OFF, ALL, ONE;

    fun next(): RepeatMode = entries[(ordinal + 1) % entries.size]
}

class PlayOrder(
    private val size: Int,
    start: Int,
    shuffle: Boolean = false,
    private val random: Random = Random.Default,
) {
    private var order: List<Int> = (0 until size).toList()
    private var pos = start.coerceIn(0, (size - 1).coerceAtLeast(0))

    var shuffled: Boolean = false
        private set

    init {
        if (shuffle) setShuffle(true)
    }

    val current: Int get() = order.getOrElse(pos) { 0 }

    fun setShuffle(on: Boolean) {
        val playing = current
        shuffled = on
        order = if (on) listOf(playing) + (0 until size).filter { it != playing }.shuffled(random)
        else (0 until size).toList()
        pos = order.indexOf(playing).coerceAtLeast(0)
    }

    fun advance(auto: Boolean, repeat: RepeatMode): Int? {
        if (size == 0) return null
        if (auto && repeat == RepeatMode.ONE) return current
        if (pos < order.lastIndex) { pos++; return current }
        if (repeat == RepeatMode.OFF) return null
        pos = 0
        return current
    }

    fun back(): Int? {
        if (pos == 0) return null
        pos--
        return current
    }

    fun upcoming(count: Int, repeat: RepeatMode): List<Int> {
        if (size <= 1 || count <= 0) return emptyList()
        val rest = order.drop(pos + 1)
        val wrapped = if (repeat == RepeatMode.OFF) rest else rest + order.take(pos)
        return wrapped.take(count)
    }
}
