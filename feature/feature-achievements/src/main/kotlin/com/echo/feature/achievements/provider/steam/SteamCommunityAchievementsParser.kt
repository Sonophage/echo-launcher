package com.echo.feature.achievements.provider.steam

internal object SteamCommunityAchievementsParser {
    private val ROW = Regex("""class="achieveRow""", RegexOption.IGNORE_CASE)
    private val H3 = Regex("""<h3[^>]*>(.*?)</h3>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val H5 = Regex("""<h5[^>]*>(.*?)</h5>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val TAG = Regex("""<[^>]*>""")

    private const val MAX_DESCRIPTION_CHARS = 500
    private const val MAX_TITLE_CHARS = 200

    fun parse(html: String): Map<String, String> {
        val out = mutableMapOf<String, String>()
        val ambiguous = mutableSetOf<String>()

        val starts = ROW.findAll(html).map { it.range.first }.toList()
        starts.forEachIndexed { i, start ->
            val end = if (i + 1 < starts.size) starts[i + 1] else html.length
            val block = html.substring(start, end)
            val title = H3.find(block)?.groupValues?.get(1)?.let(::sanitize)?.take(MAX_TITLE_CHARS)
            val description = H5.find(block)?.groupValues?.get(1)?.let(::sanitize)?.take(MAX_DESCRIPTION_CHARS)
            if (title.isNullOrBlank() || description.isNullOrBlank()) return@forEachIndexed
            val key = normalizeTitle(title)
            val existing = out[key]
            when {
                key in ambiguous -> Unit
                existing == null -> out[key] = description
                existing != description -> { out.remove(key); ambiguous.add(key) }
            }
        }
        return out
    }

    fun normalizeTitle(title: String): String = title.trim().lowercase().replace(WHITESPACE, " ")

    private val WHITESPACE = Regex("""\s+""")

    private fun sanitize(raw: String): String = raw
        .replace(TAG, "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&#039;", "'")
        .replace("&nbsp;", " ")
        .replace(WHITESPACE, " ")
        .trim()
}
