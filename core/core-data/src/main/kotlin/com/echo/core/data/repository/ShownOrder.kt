package com.echo.core.data.repository

// The whole stored order after the owner rearranges the ids he can see (owner, 2026-10-07: Move on the
// crossbar). Each shown id takes one of the slots the shown ids held, in the new order; an id he cannot
// see (a disabled system, the Settings column on the panel) keeps its own slot.
internal fun reorderedKeepingHidden(all: List<String>, shown: List<String>): List<String> {
    val shownHere = shown.filter { it in all }
    val moved = shownHere.toSet()
    val next = shownHere.iterator()
    return all.map { if (it in moved) next.next() else it }
}
