package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BookLibrary
import com.echo.core.domain.model.MusicFolder
import com.echo.core.domain.model.MusicTrack
import com.echo.core.domain.model.PhotoLibrary
import com.echo.core.domain.model.VideoLibrary
import com.echo.feature.crossbar.music.MusicPlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaColumnsTest {
    private fun musicFolder(tracks: Int) = MusicFolder(
        id = "f$tracks", displayName = "Music", treeUri = "content://f",
        trackCount = tracks, createdAt = 0L, updatedAt = 0L,
    )

    private fun videoLibrary(videos: Int) = VideoLibrary(
        id = "v$videos", displayName = "Films", treeUri = "content://v",
        videoCount = videos, createdAt = 0L, updatedAt = 0L,
    )

    private fun photoLibrary(photos: Int) = PhotoLibrary(
        id = "p$photos", displayName = "Camera Roll", treeUri = "content://p",
        photoCount = photos, createdAt = 0L, updatedAt = 0L,
    )

    private fun bookLibrary(books: Int) = BookLibrary(
        id = "b$books", displayName = "Shelf", treeUri = "content://b",
        bookCount = books, createdAt = 0L, updatedAt = 0L,
    )

    private fun ids(items: List<CrossbarItem>) = items.map { it.id }

    private fun subtitleOf(items: List<CrossbarItem>, id: String) = items.first { it.id == id }.subtitle

    @Test
    fun `music root is songs, artists, albums, genres, playlists, in that order`() {
        assertEquals(
            listOf("all_music", "music_artists", "music_albums", "music_genres", "playlists", "media_folders_MUSIC"),
            ids(CrossbarUiState().musicRootSections()),
        )
    }

    @Test
    fun `now playing appears only while a track is loaded, and leads`() {
        val playing = CrossbarUiState(
            musicPlayback = MusicPlaybackState(
                track = MusicTrack(
                    id = "t1", folderId = "f1", uri = "content://t1",
                    displayName = "01 Those Who Fight.mp3",
                    title = "Those Who Fight", artist = "Nobuo Uematsu",
                ),
            ),
        )
        assertEquals("now_playing", ids(playing.musicRootSections()).first())
        assertFalse("now_playing" in ids(CrossbarUiState().musicRootSections()))
    }

    @Test
    fun `the music card counts every folder's tracks, not the folders`() {
        val state = CrossbarUiState(musicFolders = listOf(musicFolder(12), musicFolder(30)))
        assertEquals("42 tracks", subtitleOf(state.musicRootSections(), "all_music"))
    }

    @Test
    fun `video root leads with every video, the same order music reads in`() {
        assertEquals(
            listOf("all_videos", "video_collections", "video_libraries", "media_folders_VIDEO"),
            ids(CrossbarUiState().videoRootSections()),
        )
    }

    @Test
    fun `video counts the libraries on one row and their videos on the other`() {
        val state = CrossbarUiState(videoLibraries = listOf(videoLibrary(3), videoLibrary(4)))
        val rows = state.videoRootSections()
        assertEquals("2 libraries", subtitleOf(rows, "video_libraries"))
        assertEquals("7 videos", subtitleOf(rows, "all_videos"))
    }

    @Test
    fun `camera shows only when the device has a camera app`() {
        assertEquals("photo_camera", ids(CrossbarUiState().photoRootSections(true)).first())
        assertFalse("photo_camera" in ids(CrossbarUiState().photoRootSections(false)))
    }

    @Test
    fun `photo root is all photos then albums once the camera is out of the way`() {
        assertEquals(
            listOf("all_photos", "photo_albums", "media_folders_PHOTO"),
            ids(CrossbarUiState().photoRootSections(false)),
        )
    }

    @Test
    fun `the reader row appears only when a reader is set, and leads`() {
        val withReader = CrossbarUiState(defaultReader = "org.readera", defaultReaderLabel = "ReadEra")
        assertEquals("library_open_reader", ids(withReader.booksRootSections()).first())
        assertEquals("ReadEra", withReader.booksRootSections().first().title)
        assertFalse("library_open_reader" in ids(CrossbarUiState().booksRootSections()))
    }

    @Test
    fun `shelves is a row only once there is more than one shelf`() {
        val one = CrossbarUiState(bookLibraries = listOf(bookLibrary(5)))
        val two = CrossbarUiState(bookLibraries = listOf(bookLibrary(5), bookLibrary(6)))
        assertFalse("library_shelves" in ids(one.booksRootSections()))
        assertTrue("library_shelves" in ids(two.booksRootSections()))
        assertEquals("2 shelves", subtitleOf(two.booksRootSections(), "library_shelves"))
    }

    @Test
    fun `series is a row only once something declares one`() {
        val none = CrossbarUiState(bookLibraries = listOf(bookLibrary(5)))
        val some = none.copy(bookSeries = listOf(BookSeries("Discworld", 41, null)))
        assertFalse("library_series" in ids(none.booksRootSections()))
        assertEquals("1 series", subtitleOf(some.booksRootSections(), "library_series"))
    }

    @Test
    // owner, 2026-10-08: Library Settings sat between Series and Books; it ends the column, as elsewhere
    fun `books comes just before the settings row, which is last`() {
        val full = CrossbarUiState(
            bookLibraries = listOf(bookLibrary(5), bookLibrary(6)),
            bookSeries = listOf(BookSeries("Discworld", 41, null)),
            defaultReader = "org.readera",
        )
        val folders = CrossbarViewModel.mediaFoldersItemId(com.echo.core.data.repository.MediaRootKind.BOOK)
        assertEquals(listOf("all_books", folders), ids(full.booksRootSections()).takeLast(2))
        assertEquals(listOf("all_books", folders), ids(CrossbarUiState().booksRootSections()).takeLast(2))
        assertEquals("11 books", subtitleOf(full.booksRootSections(), "all_books"))
    }

    @Test
    fun `a media column leads with its apps and keeps the add rows last`() {
        val sections = listOf(CrossbarItem(id = "all_music", title = "Songs"))
        val apps = listOf(CrossbarItem(id = "app_spotify", title = "Spotify"))
        val addRows = listOf(
            CrossbarItem(id = "add_music_folder", title = "Add Music Folder"),
            CrossbarItem(id = "add_music_apps", title = "Add Music Apps"),
        )

        assertEquals(
            listOf("app_spotify", "all_music", "add_menu"),
            ids(mediaColumn(sections, apps, addRows)),
        )
    }

    // owner, 2026-10-05: with no folder the category stays, but only its apps (and Add Apps) remain
    @Test
    fun `a media category with no folder keeps only its apps and Add Apps`() {
        val apps = listOf(CrossbarItem(id = "app_spotify", title = "Spotify"))
        val addRows = CrossbarViewModel.run {
            listOf(
                CrossbarItem(id = ADD_MUSIC_FOLDER_ITEM_ID, title = "Add Music Folder"),
                CrossbarItem(id = ADD_VIDEOS_ITEM_ID, title = "Add Videos"),
                CrossbarItem(id = ADD_PHOTO_LIBRARY_ITEM_ID, title = "Add Photo Library"),
                CrossbarItem(id = ADD_BOOK_FOLDER_ITEM_ID, title = "Add Book Folder"),
                CrossbarItem(id = ADD_MUSIC_APPS_ITEM_ID, title = "Add Music Apps"),
            )
        }

        assertEquals(listOf("app_spotify", CrossbarViewModel.ADD_MUSIC_APPS_ITEM_ID), ids(folderlessColumn(apps, addRows)))
    }

    @Test
    fun `a column with no apps still reads sections then add`() {
        val sections = listOf(CrossbarItem(id = "all_music", title = "Songs"))
        val addRows = listOf(CrossbarItem(id = "add_music_apps", title = "Add Music Apps"))

        assertEquals(
            listOf("all_music", "add_music_apps"),
            ids(mediaColumn(sections, emptyList(), addRows)),
        )
    }

    @Test
    fun `one add row is shown as itself and several collapse into one menu`() {
        val folder = CrossbarItem(id = "add_music_folder", title = "Add Music Folder")
        val apps = CrossbarItem(id = "add_music_apps", title = "Add Music Apps")
        assertEquals(emptyList<CrossbarItem>(), collapseAddRows(emptyList()))
        assertEquals(listOf(folder), collapseAddRows(listOf(folder)))

        val collapsed = collapseAddRows(listOf(folder, apps))
        assertEquals(listOf("add_menu"), ids(collapsed))

        assertEquals("Music Folder  ·  Music Apps", collapsed.single().subtitle)
    }

    private fun track(
        id: String, title: String, artist: String? = null, album: String? = null,
        artUri: String? = null, albumArtist: String? = null,
    ) = MusicTrack(
        id = id, folderId = "f1", uri = "content://$id", displayName = "$id.mp3",
        title = title, artist = artist, album = album, artUri = artUri,
        albumArtist = albumArtist,
    )

    @Test
    fun `an artist is one row however many tracks it has`() {
        val groups = listOf(
            track("1", "Aerith's Theme", artist = "Nobuo Uematsu"),
            track("2", "One-Winged Angel", artist = "Nobuo Uematsu"),
            track("3", "Snake Eater", artist = "Norihiko Hibino"),
        ).artistGroups()
        assertEquals(listOf("Nobuo Uematsu", "Norihiko Hibino"), groups.map { it.name })
        assertEquals(listOf(2, 1), groups.map { it.trackCount })
        assertEquals("2 tracks", groups.first().subtitle)
    }

    @Test
    fun `case and whitespace do not split an artist in two`() {
        val groups = listOf(
            track("1", "Come Together", artist = "The Beatles"),
            track("2", "Something", artist = " the beatles "),
        ).artistGroups()
        assertEquals(1, groups.size)
        assertEquals("The Beatles", groups.single().name)
        assertEquals(2, groups.single().trackCount)
    }

    @Test
    fun `a missing tag, a blank one and an empty one are the same bucket, and it sorts last`() {
        val groups = listOf(
            track("1", "Untitled"),
            track("2", "Untitled II", artist = ""),
            track("3", "Untitled III", artist = "   "),
            track("4", "Zoo Station", artist = "U2"),
        ).artistGroups()
        assertEquals(listOf("U2", "Unknown Artist"), groups.map { it.name })
        assertEquals(3, groups.last().trackCount)
    }

    @Test
    fun `an album keeps its compilation together and says so`() {
        val groups = listOf(
            track("1", "Song A", artist = "Artist A", album = "Now That's What I Call Music"),
            track("2", "Song B", artist = "Artist B", album = "Now That's What I Call Music"),
            track("3", "Song C", artist = "Solo", album = "Just Mine"),
        ).albumGroups()
        assertEquals(listOf("Just Mine", "Now That's What I Call Music"), groups.map { it.name })
        assertEquals("Solo  ·  1 track", groups.first().subtitle)
        assertEquals("Various Artists  ·  2 tracks", groups.last().subtitle)
    }

    @Test
    fun `a group carries the first cover any of its tracks has`() {
        val groups = listOf(
            track("1", "Intro", album = "Kid A"),
            track("2", "Idioteque", album = "Kid A", artUri = "file:///art/kida.png"),
        ).albumGroups()
        assertEquals("file:///art/kida.png", groups.single().artUri)
    }

    @Test
    fun `the group key is what the drill-in filters by, not the display name`() {
        val group = listOf(
            track("1", "Come Together", artist = "The Beatles"),
            track("2", "Something", artist = "the beatles"),
        ).artistGroups().single()
        assertEquals("the beatles", group.key)
        val tracks = listOf(
            track("1", "Come Together", artist = "The Beatles"),
            track("2", "Something", artist = "the beatles"),
            track("3", "Kashmir", artist = "Led Zeppelin"),
        ).tracksByArtistKey(group.key)
        assertEquals(listOf("1", "2"), tracks.map { it.id })
    }

    @Test
    fun `a joint credit splits on names the library has seen alone`() {
        val groups = listOf(
            track("1", "All the Stars", artist = "Kendrick Lamar, SZA"),
            track("2", "HUMBLE.",      artist = "Kendrick Lamar"),
            track("3", "Good Days",    artist = "SZA"),
            track("4", "EARFQUAKE",    artist = "Tyler, The Creator"),
        ).artistGroups()

        assertEquals(listOf("Kendrick Lamar", "SZA", "Tyler, The Creator"), groups.map { it.name })

        assertEquals(listOf(2, 2, 1), groups.map { it.trackCount })
    }

    @Test
    fun `a name the library has never seen alone still gets its own row`() {
        val groups = listOf(
            track("1", "Family Ties", artist = "Kendrick Lamar, Baby Keem"),
            track("2", "HUMBLE.",     artist = "Kendrick Lamar"),
        ).artistGroups()
        assertEquals(listOf("Baby Keem", "Kendrick Lamar"), groups.map { it.name })
        assertEquals(listOf(1, 2), groups.map { it.trackCount })
    }

    @Test
    fun `unproven names next to each other are read as the one name they are`() {
        val groups = listOf(
            track("1", "See You Again", artist = "Tyler, The Creator, Kali Uchis"),
            track("2", "Telepatia",     artist = "Kali Uchis"),
        ).artistGroups()
        assertEquals(listOf("Kali Uchis", "Tyler, The Creator"), groups.map { it.name })
    }

    @Test
    fun `an act whose own name has a comma survives when no part of it stands alone`() {
        val ordinary = listOf(
            track("1", "September", artist = "Earth, Wind & Fire"),
            track("2", "Solo",      artist = "Chic"),
        ).artistGroups()
        assertEquals(listOf("Chic", "Earth, Wind & Fire"), ordinary.map { it.name })

        val coincidence = listOf(
            track("1", "September", artist = "Earth, Wind & Fire"),
            track("2", "Solo",      artist = "Earth"),
        ).artistGroups()
        assertEquals(listOf("Earth", "Wind & Fire"), coincidence.map { it.name })
    }

    @Test
    fun `an ampersand is never a separator`() {
        val groups = listOf(
            track("1", "The Boxer", artist = "Simon & Garfunkel"),
            track("2", "Sound",     artist = "Simon"),
            track("3", "Angel",     artist = "Garfunkel"),
        ).artistGroups()
        assertEquals(listOf("Garfunkel", "Simon", "Simon & Garfunkel"), groups.map { it.name })
    }

    @Test
    fun `every artist row opens onto exactly the tracks it counted`() {
        val library = listOf(
            track("1", "All the Stars", artist = "Kendrick Lamar, SZA"),
            track("2", "HUMBLE.",       artist = "Kendrick Lamar"),
            track("3", "Good Days",     artist = "SZA"),
            track("4", "EARFQUAKE",     artist = "Tyler, The Creator"),
            track("5", "Untitled",      artist = "   "),
        )
        library.artistGroups().forEach { group ->
            assertEquals(
                "row \"${group.name}\" counts ${group.trackCount} but opens onto a different set",
                group.trackCount,
                library.tracksByArtistKey(group.key).size,
            )
        }
    }

    @Test
    fun `no tracks means no groups at all`() {
        assertEquals(emptyList<MusicGroup>(), emptyList<MusicTrack>().artistGroups())
        assertEquals(emptyList<MusicGroup>(), emptyList<MusicTrack>().albumGroups())
    }

    private fun played(
        id: String, title: String, album: String? = null, at: Long, artUri: String? = null,
    ) = MusicTrack(
        id = id, folderId = "f1", uri = "content://$id", displayName = "$id.mp3",
        title = title, album = album, artUri = artUri, lastPlayedAt = at,
    )

    @Test
    fun `a run of one album becomes one album row`() {
        val rows = listOf(
            played("1", "Everything In Its Right Place", album = "Kid A", at = 900),
            played("2", "Kid A", album = "Kid A", at = 800),
            played("3", "The National Anthem", album = "Kid A", at = 700),
        ).recentMusicRows()
        assertEquals(1, rows.size)
        assertEquals("Kid A", rows.single().second.title)
        assertEquals("3 tracks", rows.single().second.subtitle)
        assertEquals(CrossbarItemType.MUSIC_GROUP, rows.single().second.type)

        assertEquals(900L, rows.single().first)
    }

    // owner, 2026-10-08: the Thor crashed when one album was played in two separate runs: two rows, one id
    @Test
    fun `an album played in two separate runs makes two rows with their own ids`() {
        val rows = listOf(
            played("1", "Aerial", album = "Paper Radio", at = 900),
            played("2", "Static", album = "Paper Radio", at = 890),
            played("3", "Foghorn", album = "Longitudes", at = 800),
            played("4", "Kite", album = "Paper Radio", at = 700),
            played("5", "Signal", album = "Paper Radio", at = 690),
        ).recentMusicRows()
        assertEquals(listOf("Paper Radio", "Foghorn", "Paper Radio"), rows.map { it.second.title })
        assertEquals("every row's id is its own", rows.size, rows.map { it.second.id }.toSet().size)
        assertTrue("both are still recent albums", rows.filter { it.second.type == CrossbarItemType.MUSIC_GROUP }.all { it.second.isRecentAlbum })
    }

    @Test
    fun `only CONSECUTIVE tracks collapse`() {
        val rows = listOf(
            played("1", "Idioteque", album = "Kid A", at = 900),
            played("2", "Song B", album = "Other", at = 800),
            played("3", "Optimistic", album = "Kid A", at = 700),
        ).recentMusicRows()
        assertEquals(listOf("Idioteque", "Song B", "Optimistic"), rows.map { it.second.title })
        assertTrue(rows.all { it.second.type == CrossbarItemType.MUSIC_TRACK })
    }

    @Test
    fun `a run of one stays a track`() {
        val rows = listOf(played("1", "Idioteque", album = "Kid A", at = 900)).recentMusicRows()
        assertEquals("Idioteque", rows.single().second.title)
        assertEquals(CrossbarItemType.MUSIC_TRACK, rows.single().second.type)
    }

    @Test
    fun `untagged tracks never collapse into each other`() {
        val rows = listOf(
            played("1", "Untitled", at = 900),
            played("2", "Untitled II", album = "", at = 800),
            played("3", "Untitled III", album = "   ", at = 700),
        ).recentMusicRows()
        assertEquals(3, rows.size)
        assertTrue(rows.all { it.second.type == CrossbarItemType.MUSIC_TRACK })
    }

    @Test
    fun `the album row carries the key the drill-in filters by, and a cover`() {
        val rows = listOf(
            played("1", "Intro", album = "Kid A", at = 900),
            played("2", "Idioteque", album = "Kid A", at = 800, artUri = "file:///art/kida.png"),
        ).recentMusicRows()
        assertEquals("kid a", rows.single().second.musicGroupKey)
        assertEquals("file:///art/kida.png", rows.single().second.coverUri)
    }

    @Test
    fun `an unstarted or unmeasurable video has no progress at all`() {
        assertEquals(null, videoProgressFraction(0L, 60_000L))
        assertEquals(null, videoProgressFraction(30_000L, null))
        assertEquals(null, videoProgressFraction(30_000L, 0L))
        assertEquals(null, videoProgressLabel(0L, 60_000L))
    }

    @Test
    fun `a resume point past the end is a stale stamp, not a finished video`() {
        assertEquals(null, videoProgressFraction(120_000L, 60_000L))
    }

    @Test
    fun `the words say what is left, not what is done`() {
        assertEquals("30 min left", videoProgressLabel(30 * 60_000L, 60 * 60_000L))
        assertEquals("1 hr left", videoProgressLabel(60 * 60_000L, 120 * 60_000L))
        assertEquals("1 hr 30 min left", videoProgressLabel(30 * 60_000L, 120 * 60_000L))
        assertEquals("Almost finished", videoProgressLabel(119 * 60_000L + 59_000L, 120 * 60_000L))
    }

    @Test
    fun `artists group on the album artist, not the credit line`() {
        val groups = listOf(
            track("1", "Bloody Waters", artist = "Ab-Soul, Anderson .Paak, James Blake",
                  albumArtist = "Kendrick Lamar", album = "Black Panther"),
            track("2", "King's Dead", artist = "Jay Rock, Kendrick Lamar, Future",
                  albumArtist = "Kendrick Lamar", album = "Black Panther"),
        ).artistGroups()
        assertEquals(listOf("Kendrick Lamar"), groups.map { it.name })
        assertEquals(2, groups.single().trackCount)
    }

    @Test
    fun `a file with no album artist keeps grouping by what it does have`() {
        val groups = listOf(
            track("1", "Zoo Station", artist = "U2"),
            track("2", "One", artist = "U2"),
        ).artistGroups()
        assertEquals(listOf("U2"), groups.map { it.name })
        assertEquals(2, groups.single().trackCount)
    }

    @Test
    fun `a blank album artist is no album artist`() {
        val groups = listOf(track("1", "Song", artist = "Real Band", albumArtist = "   ")).artistGroups()
        assertEquals(listOf("Real Band"), groups.map { it.name })
    }

    @Test
    fun `an album is Various Artists only when its ACTS differ, not its credits`() {
        val oneAct = listOf(
            track("1", "A", artist = "Kendrick Lamar, SZA", albumArtist = "Kendrick Lamar", album = "Black Panther"),
            track("2", "B", artist = "Kendrick Lamar, Future", albumArtist = "Kendrick Lamar", album = "Black Panther"),
        ).albumGroups()
        assertEquals("Kendrick Lamar  ·  2 tracks", oneAct.single().subtitle)

        val manyActs = listOf(
            track("1", "A", artist = "Artist A", albumArtist = "Artist A", album = "Now 42"),
            track("2", "B", artist = "Artist B", albumArtist = "Artist B", album = "Now 42"),
        ).albumGroups()
        assertEquals("Various Artists  ·  2 tracks", manyActs.single().subtitle)
    }

    private fun video(resume: Long, duration: Long? = 600_000L, thumb: String? = "thumb://v1") =
        com.echo.core.domain.model.Video(
            id = "v1", libraryId = "lib", uri = "content://v1", displayName = "A Film",
            durationMs = duration, resumePositionMs = resume, thumbnailUri = thumb,
        )

    @Test
    fun `the playing row carries no frozen position, because the live row draws the moving one`() {
        val track = MusicTrack(
            id = "t1", folderId = "f", uri = "content://t1", displayName = "Song",
            title = "Dracula's Castle", artist = "Michiru Yamane",
        )
        val playing = CrossbarUiState(
            musicPlayback = MusicPlaybackState(track = track, isPlaying = true, durationMs = 214_000),
        ).musicRootSections().first()
        assertEquals("now_playing", playing.id)
        assertNull("a snapshot position would go stale between refreshes", playing.progressFraction)
        assertNull(playing.progressLabel)
    }

    @Test
    fun `no track playing means no Now Playing row at all`() {
        val rows = CrossbarUiState().musicRootSections()
        assertTrue("Songs should lead when nothing is playing", rows.first().title == "Songs")
        assertTrue(rows.none { it.progressFraction != null })
    }

    @Test
    fun `a part-watched video leads the Video column with its progress`() {
        val rows = CrossbarUiState(resumeVideo = video(resume = 150_000L)).videoRootSections()
        val resume = rows.first()
        assertEquals("A Film", resume.title)
        assertTrue("the resume row says so", resume.subtitle!!.startsWith("Resume"))
        assertEquals(0.25f, resume.progressFraction!!, 0.005f)
    }

    @Test
    fun `nothing part-watched means no resume row`() {
        val rows = CrossbarUiState().videoRootSections()
        assertEquals("Videos", rows.first().title)
        assertTrue(rows.none { it.subtitle?.startsWith("Resume") == true })
    }

    @Test
    fun `the Books column gets no scrubber, because nothing reports a page`() {
        val rows = CrossbarUiState(bookLibraries = listOf(bookLibrary(12))).booksRootSections()
        assertTrue("books cannot know a page", rows.none { it.progressFraction != null })
    }

    @Test
    fun `a row about one thing keeps its own art, whatever its type`() {
        val covers = MediaCovers(video = (1..12).map { "pool$it" })

        val resume = CrossbarUiState(resumeVideo = video(resume = 150_000L), mediaCovers = covers)
            .videoRootSections().first()
        assertTrue("the resume row keeps its thumbnail", resume.insideCovers.isEmpty())
        assertNotNull(resume.coverUri)

        val artless = CrossbarUiState(resumeVideo = video(resume = 150_000L, thumb = null), mediaCovers = covers)
            .videoRootSections().first()
        assertTrue("a thumbnail-less resume row gets no grid either", artless.insideCovers.isEmpty())

        val track = MusicTrack(
            id = "t1", folderId = "f", uri = "content://t1", displayName = "Song", artUri = "art://1",
        )
        val nowPlaying = CrossbarUiState(
            musicPlayback = MusicPlaybackState(track = track, isPlaying = true, durationMs = 2),
            mediaCovers = MediaCovers(music = (1..12).map { "pool$it" }),
        ).musicRootSections().first()
        assertTrue("the playing track keeps its album art", nowPlaying.insideCovers.isEmpty())
    }

    @Test
    fun `an art-bearing row does not consume a grid slot`() {
        val covers = MediaCovers(video = (1..12).map { "pool$it" })
        val without = CrossbarUiState(mediaCovers = covers).videoRootSections()
        val with = CrossbarUiState(resumeVideo = video(resume = 150_000L), mediaCovers = covers)
            .videoRootSections()
        assertEquals(
            "the first grid row keeps the same covers either way",
            without.first { it.insideCovers.isNotEmpty() }.insideCovers,
            with.first { it.insideCovers.isNotEmpty() }.insideCovers,
        )
    }

    @Test
    fun `the last book opened leads the Library column, with no page numbers`() {
        val book = com.echo.core.domain.model.Book(
            id = "b1", libraryId = "lib", uri = "content://b1",
            displayName = "Faceless", title = "Lord of Mysteries Volume 2: Faceless",
            coverUri = "cover://b1", lastOpenedAt = 1_700_000_000_000L,
        )
        val row = CrossbarUiState(continueBook = book, bookLibraries = listOf(bookLibrary(80)))
            .booksRootSections().first()
        assertEquals("Lord of Mysteries Volume 2: Faceless", row.title)
        assertTrue(row.subtitle!!.startsWith("Continue reading"))
        assertEquals("cover://b1", row.coverUri)

        assertNull("a book cannot report a page", row.progressFraction)
        assertTrue(row.insideCovers.isEmpty())
    }

    @Test
    fun `nothing opened means no Continue reading row`() {
        val rows = CrossbarUiState(bookLibraries = listOf(bookLibrary(80))).booksRootSections()
        assertTrue(rows.none { it.subtitle?.startsWith("Continue reading") == true })
    }
}
