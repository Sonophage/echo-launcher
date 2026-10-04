package com.echo.feature.achievements.match

import android.content.Context
import android.net.Uri
import com.echo.core.domain.model.Game
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RomBytesReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun read(game: Game): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val identifier = game.romPath ?: game.romUri ?: return@runCatching null
            val isZip = identifier.endsWith(".zip", ignoreCase = true)
            openStream(game)?.use { stream ->
                if (isZip) readFirstZipEntry(stream) else capped(stream.readBytes())
            }
        }.getOrNull()
    }

    private fun openStream(game: Game): InputStream? {
        game.romPath?.let { File(it).takeIf(File::exists) }?.let { return it.inputStream() }
        game.romUri?.let { return context.contentResolver.openInputStream(Uri.parse(it)) }
        return null
    }

    private fun readFirstZipEntry(stream: InputStream): ByteArray? {
        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) return capped(zip.readBytes())
                entry = zip.nextEntry
            }
        }
        return null
    }

    private fun capped(bytes: ByteArray): ByteArray? = bytes.takeIf { it.size <= MAX_BYTES }

    private companion object {
        const val MAX_BYTES = 256 * 1024 * 1024
    }
}
