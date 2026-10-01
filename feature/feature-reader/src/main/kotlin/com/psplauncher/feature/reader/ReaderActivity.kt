package com.psplauncher.feature.reader

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import com.psplauncher.core.data.book.BuiltInReader
import com.psplauncher.core.data.repository.ControllerLayoutRepository
import com.psplauncher.core.domain.model.GamepadMappings
import com.psplauncher.core.domain.model.gamepadMappingsFor
import com.psplauncher.core.ui.theme.PFPTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.css.ColCount
import org.readium.r2.navigator.epub.css.Length
import org.readium.r2.navigator.epub.css.RsProperties
import org.readium.r2.navigator.image.ImageNavigatorFragment
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.util.DirectionalNavigationAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalReadiumApi::class)
@AndroidEntryPoint
class ReaderActivity : FragmentActivity() {
    @Inject lateinit var controllerLayout: ControllerLayoutRepository

    private val ui = ReaderUiState()
    private lateinit var store: ReaderStore
    private var publication: Publication? = null
    private var navigator: OverflowableNavigator? = null
    private var mappings = GamepadMappings()
    private val containerId = View.generateViewId()
    private var rootView: View? = null
    private var attachedLayout: ReaderLayout? = null

    private val bookId: String by lazy { intent.getStringExtra(BuiltInReader.EXTRA_BOOK_ID).orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        store = ReaderStore(applicationContext)
        ui.title = intent.getStringExtra(BuiltInReader.EXTRA_BOOK_TITLE).orEmpty()
        ui.author = intent.getStringExtra(BuiltInReader.EXTRA_BOOK_AUTHOR)
        savedInstanceState?.let {
            ui.optionsOpen = it.getBoolean(STATE_OPTIONS_OPEN)
            ui.tab = OptionsTab.entries[it.getInt(STATE_TAB)]
            ui.cursor = it.getInt(STATE_CURSOR)
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val root = FrameLayout(this)
        rootView = root
        val dp = resources.displayMetrics.density
        root.addView(
            FragmentContainerView(this).apply {
                id = containerId
                // Keys are read by the activity; a focused WebView draws a ring around the book's links.
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            },
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                topMargin = (READER_TOP_DP * dp).toInt()
                bottomMargin = (READER_BOTTOM_DP * dp).toInt()
            },
        )
        root.addView(ComposeView(this).apply {
            setContent { PFPTheme { ReaderChrome(ui, ::onCommand) } }
        })
        setContentView(root)

        lifecycleScope.launch { controllerLayout.prefs.collect { mappings = gamepadMappingsFor(it.confirmBackLayout, it.xyLayout) } }
        lifecycleScope.launch { store.display.collect { d -> ui.display = d; applyDisplay(d) } }
        lifecycleScope.launch { store.bookmarks(bookId).collect { ui.bookmarks = it } }
        lifecycleScope.launch { openBook() }
    }

    private suspend fun openBook() {
        val uri = intent.getStringExtra(BuiltInReader.EXTRA_BOOK_URI)
        val url = uri?.let { AbsoluteUrl(it) } ?: return fail("This book has no file.")
        val http = DefaultHttpClient()
        val retriever = AssetRetriever(contentResolver, http)
        val opener = PublicationOpener(DefaultPublicationParser(this, http, retriever, PdfiumDocumentFactory(this)))
        val asset = retriever.retrieve(url).getOrElse { return fail("The book could not be read (${it.message}).") }
        val pub = opener.open(asset, allowUserInteraction = false).getOrElse {
            asset.close()
            return fail("This book format is not supported (${it.message}).")
        }
        publication = pub

        val order = pub.readingOrder.map { it.url().toString() }
        ui.readingOrder = order
        // A PDF or comic without a table of contents has no chapters; its reading order is files or pages.
        ui.chapters = flatten(pub.tableOfContents, 0)
        ui.positions = runCatching { pub.positions() }.getOrDefault(emptyList()).mapNotNull { p ->
            p.locations.position?.let { PagePosition(hrefPath(p.href.toString()), it, p.locations.totalProgression ?: 0.0) }
        }
        ui.isFixedLayout = !pub.conformsTo(Publication.Profile.EPUB)

        ui.display = store.display.first()
        val initial = store.position(bookId)?.let { runCatching { Locator.fromJSON(JSONObject(it)) }.getOrNull() }
        attachNavigator(pub, initial)
        ui.loading = false
    }

    private fun attachNavigator(pub: Publication, initial: Locator?) {
        val factory = when {
            pub.conformsTo(Publication.Profile.EPUB) -> EpubNavigatorFactory(pub).createFragmentFactory(
                initialLocator = initial,
                initialPreferences = epubPreferences(ui.display),
                listener = object : EpubNavigatorFragment.Listener {
                    override fun onExternalLinkActivated(url: AbsoluteUrl) = Unit
                },
                configuration = EpubNavigatorFragment.Configuration {
                    servedAssets = listOf("fonts/.*")
                    addFontFamilyDeclaration(LITERATA) {
                        addFontFace { addSource("fonts/Literata.ttf") }
                    }
                    // ReadiumCSS ignores the reader's column count below a 60em viewport, and the
                    // handheld is about 821 CSS px wide, so the spread is set on the base properties.
                    // Those are fixed for the fragment's life, which is why a layout change recreates
                    // the activity.
                    if (ui.display.layout == ReaderLayout.TWO_PAGES) {
                        readiumCssRsProperties = RsProperties(colCount = ColCount.TWO, colWidth = Length.Em(15.0))
                    }
                },
            )
            pub.conformsTo(Publication.Profile.PDF) ->
                PdfNavigatorFactory(pub, PdfiumEngineProvider()).createFragmentFactory(initialLocator = initial)
            else -> ImageNavigatorFragment.createFactory(pub, initial, null)
        }
        supportFragmentManager.fragmentFactory = factory
        val fragmentClass = when {
            pub.conformsTo(Publication.Profile.EPUB) -> EpubNavigatorFragment::class.java
            pub.conformsTo(Publication.Profile.PDF) -> org.readium.r2.navigator.pdf.PdfNavigatorFragment::class.java
            else -> ImageNavigatorFragment::class.java
        }
        supportFragmentManager.beginTransaction().replace(containerId, fragmentClass, null, NAVIGATOR_TAG).commitNow()
        val nav = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as OverflowableNavigator
        navigator = nav
        attachedLayout = ui.display.layout
        nav.addInputListener(DirectionalNavigationAdapter(nav, animatedTransition = true))

        lifecycleScope.launch {
            nav.currentLocator.collectLatest { locator ->
                ui.locator = locator
                delay(SAVE_DEBOUNCE_MS)
                store.savePosition(bookId, locator.toJSON().toString())
            }
        }
    }

    private fun fail(message: String) {
        Timber.w("Reader: $message")
        ui.loading = false
        ui.error = message
    }

    private fun applyDisplay(d: ReaderDisplay) {
        rootView?.setBackgroundColor(d.page.background)
        // Replacing only the fragment loses the place within the chapter, so the activity is
        // recreated from the saved position, the same path that reopening the book takes.
        if (navigator is EpubNavigatorFragment && attachedLayout != d.layout) {
            lifecycleScope.launch {
                ui.locator?.let { store.savePosition(bookId, it.toJSON().toString()) }
                recreate()
            }
            return
        }
        (navigator as? EpubNavigatorFragment)?.submitPreferences(epubPreferences(d))
    }

    private fun epubPreferences(d: ReaderDisplay) = EpubPreferences(
        fontFamily = if (d.typeface == ReaderTypeface.SERIF) LITERATA else FontFamily.SANS_SERIF,
        fontSize = d.textScale.toDouble(),
        backgroundColor = Color(d.page.background),
        textColor = Color(d.page.text),
        columnCount = if (d.layout == ReaderLayout.TWO_PAGES) ColumnCount.TWO else ColumnCount.ONE,
        publisherStyles = false,
    )

    private fun flatten(links: List<Link>, depth: Int): List<ChapterEntry> = links.flatMap { l ->
        listOf(ChapterEntry(l.title ?: l.url().toString(), l.url().toString(), depth)) + flatten(l.children, depth + 1)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = mappings.actionFor(event.keyCode) ?: return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN) {
            val command = if (ui.optionsOpen) optionsCommand(action, ui.tab) else readingCommand(action)
            command?.let(::onCommand)
        }
        return true
    }

    private fun onCommand(command: ReaderCommand) {
        val nav = navigator
        when (command) {
            ReaderCommand.PageForward -> nav?.goForward(animated = true)
            ReaderCommand.PageBackward -> nav?.goBackward(animated = true)
            ReaderCommand.NextChapter, ReaderCommand.PreviousChapter -> {
                val href = ui.locator?.href?.toString() ?: return
                val delta = if (command == ReaderCommand.NextChapter) 1 else -1
                adjacentChapterHref(ui.chapters, ui.readingOrder, href, delta)?.let { goToHref(it) }
            }
            ReaderCommand.OpenOptions -> { ui.optionsOpen = true; ui.cursor = currentCursorFor(ui.tab) }
            ReaderCommand.CloseOptions -> ui.optionsOpen = false
            ReaderCommand.Close -> finish()
            ReaderCommand.ToggleBookmark -> toggleBookmark()
            is ReaderCommand.MoveCursor -> ui.cursor = (ui.cursor + command.delta).coerceIn(0, (rowCount(ui.tab) - 1).coerceAtLeast(0))
            is ReaderCommand.SwitchTab -> {
                ui.tab = OptionsTab.entries[(ui.tab.ordinal + command.delta).mod(OptionsTab.entries.size)]
                ui.cursor = currentCursorFor(ui.tab)
            }
            is ReaderCommand.Adjust -> DisplayRow.entries.getOrNull(ui.cursor)?.let { row ->
                val next = adjustDisplay(ui.display, row, command.delta)
                lifecycleScope.launch { store.saveDisplay(next) }
            }
            ReaderCommand.Activate -> when (ui.tab) {
                OptionsTab.CONTENTS -> ui.chapters.getOrNull(ui.cursor)?.let { goToHref(it.href); ui.optionsOpen = false }
                OptionsTab.BOOKMARKS -> ui.bookmarks.getOrNull(ui.cursor)?.let { mark ->
                    runCatching { Locator.fromJSON(JSONObject(mark.locatorJson)) }.getOrNull()?.let(::goToLocator)
                    ui.optionsOpen = false
                }
                OptionsTab.DISPLAY -> onCommand(ReaderCommand.Adjust(1))
            }
        }
    }

    private fun goToLocator(target: Locator) {
        val nav = navigator ?: return
        val crossesChapter = ui.locator?.href != target.href
        nav.go(target)
        // Readium 3.4 opens another chapter at its top and drops the progression; once that chapter
        // is current, the same go lands on the page, as it does within a chapter.
        if (crossesChapter) lifecycleScope.launch {
            nav.currentLocator.first { it.href == target.href }
            nav.go(target)
        }
    }

    private fun goToHref(href: String) {
        val pub = publication ?: return
        val link = pub.readingOrder.firstOrNull { hrefPath(it.url().toString()) == hrefPath(href) } ?: return
        navigator?.go(link, animated = false)
    }

    private fun currentCursorFor(tab: OptionsTab): Int = when (tab) {
        OptionsTab.CONTENTS -> currentChapterIndex(ui.chapters, ui.readingOrder, ui.locator?.href?.toString().orEmpty()).coerceAtLeast(0)
        else -> 0
    }

    private fun rowCount(tab: OptionsTab): Int = when (tab) {
        OptionsTab.CONTENTS -> ui.chapters.size
        OptionsTab.BOOKMARKS -> ui.bookmarks.size
        OptionsTab.DISPLAY -> if (ui.isFixedLayout) 0 else DisplayRow.entries.size
    }

    private fun toggleBookmark() {
        val locator = ui.locator ?: return
        val chapter = ui.chapters.getOrNull(currentChapterIndex(ui.chapters, ui.readingOrder, locator.href.toString()))
        val percent = ((locator.locations.totalProgression ?: 0.0) * 100).toInt()
        val candidate = StoredBookmark(
            locatorJson = locator.toJSON().toString(),
            label = listOfNotNull(chapter?.title, "$percent%").joinToString("  ·  "),
            createdAt = System.currentTimeMillis(),
        )
        val position = locator.locations.position
        val next = toggleBookmark(ui.bookmarks, candidate) { mark ->
            runCatching { Locator.fromJSON(JSONObject(mark.locatorJson))?.locations?.position }.getOrNull() == position && position != null
        }
        lifecycleScope.launch { store.saveBookmarks(bookId, next) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_OPTIONS_OPEN, ui.optionsOpen)
        outState.putInt(STATE_TAB, ui.tab.ordinal)
        outState.putInt(STATE_CURSOR, ui.cursor)
    }

    override fun onDestroy() {
        publication?.close()
        super.onDestroy()
    }

    private companion object {
        const val NAVIGATOR_TAG = "reader_navigator"
        const val STATE_OPTIONS_OPEN = "options_open"
        const val STATE_TAB = "options_tab"
        const val STATE_CURSOR = "options_cursor"
        const val SAVE_DEBOUNCE_MS = 500L
        const val READER_TOP_DP = 40f
        const val READER_BOTTOM_DP = 52f
        val LITERATA = FontFamily("Literata")
    }
}

class ReaderUiState {
    var loading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var title by mutableStateOf("")
    var author by mutableStateOf<String?>(null)
    var chapters by mutableStateOf<List<ChapterEntry>>(emptyList())
    var readingOrder by mutableStateOf<List<String>>(emptyList())
    var positions by mutableStateOf<List<PagePosition>>(emptyList())
    var locator by mutableStateOf<Locator?>(null)
    var display by mutableStateOf(ReaderDisplay())
    var bookmarks by mutableStateOf<List<StoredBookmark>>(emptyList())
    var optionsOpen by mutableStateOf(false)
    var tab by mutableStateOf(OptionsTab.CONTENTS)
    var cursor by mutableIntStateOf(0)
    var isFixedLayout by mutableStateOf(false)
}
