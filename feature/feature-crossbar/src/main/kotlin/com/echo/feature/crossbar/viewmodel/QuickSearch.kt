package com.echo.feature.crossbar.viewmodel

sealed interface QuickSearchAction {
    data class Search(val query: String) : QuickSearchAction

    data class Open(val url: String) : QuickSearchAction

    data object None : QuickSearchAction
}

fun quickSearchActionFor(raw: String): QuickSearchAction {
    val text = raw.trim()
    if (text.isEmpty()) return QuickSearchAction.None
    if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) {
        return QuickSearchAction.Open(text)
    }
    if (looksLikeHost(text)) return QuickSearchAction.Open("https://$text")
    return QuickSearchAction.Search(text)
}

private fun looksLikeHost(text: String): Boolean {
    if (text.any { it.isWhitespace() }) return false

    val host = text.substringBefore('/').substringBefore('?')
    val labels = host.split('.')
    if (labels.size < 2) return false
    if (labels.any { it.isEmpty() }) return false
    val tld = labels.last()
    return tld.length >= 2 && tld.all { it.isLetter() }
}

// Quick Search's one result on the Search screen (owner, 2026-10-07: styled as Search, not a dialog);
// null until something is typed
internal fun quickSearchRow(raw: String): CrossbarItem? {
    val (title, detail) = when (val action = quickSearchActionFor(raw)) {
        QuickSearchAction.None -> return null
        is QuickSearchAction.Open -> action.url to "Open in your browser"
        is QuickSearchAction.Search -> action.query to "Search the web"
    }
    return CrossbarItem(id = CrossbarViewModel.QUICK_SEARCH_ITEM_ID, title = title, subtitle = "Web  ·  $detail", type = CrossbarItemType.SEARCH)
}
