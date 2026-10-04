package com.echo.feature.library.scanner

import android.content.Context
import com.echo.core.data.repository.InterfacePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Singleton
class LibraryRescanCoordinator internal constructor(
    libraryScanner: LibraryScanner,
    romRootDiscoveryScanner: RomRootDiscoveryScanner,
    private val scope: CoroutineScope,
    private val rescanOnReturn: suspend () -> Boolean,
) {
    @Inject constructor(
        @ApplicationContext context: Context,
        libraryScanner: LibraryScanner,
        romRootDiscoveryScanner: RomRootDiscoveryScanner,
        @RescanApplicationScope scope: CoroutineScope,
    ) : this(
        libraryScanner,
        romRootDiscoveryScanner,
        scope,
        { InterfacePreferences.current(context).rescanOnReturn },
    )

    private val bus = RescanTriggerBus(libraryScanner, romRootDiscoveryScanner, scope)

    fun onResume() {
        scope.launch { if (rescanOnReturn()) bus.submit(RescanTrigger.AppResumed) }
    }

    fun onMediaMounted() = bus.submit(RescanTrigger.MediaMounted)
}
