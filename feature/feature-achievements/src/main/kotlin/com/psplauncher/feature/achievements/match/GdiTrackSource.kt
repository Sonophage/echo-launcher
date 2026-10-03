package com.psplauncher.feature.achievements.match

import com.psplauncher.feature.achievements.match.DiscImage.SeekableSource

class GdiTrackSource(private val tracks: List<Track>) : DiscImage.SectorSource {
    class Track(
        val number: Int,
        val type: Int,
        val startLba: Int,
        val sectorCount: Int,
        val sectorSize: Int,
        val dataOffset: Int,
        val fileOffset: Long,
        val source: SeekableSource,
    )

    override fun readSector(lba: Int, count: Int): ByteArray {
        val t = tracks.firstOrNull { lba >= it.startLba && lba < it.startLba + it.sectorCount }
            ?: return ByteArray(0)
        val byte = t.fileOffset + (lba - t.startLba).toLong() * t.sectorSize + t.dataOffset
        val out = ByteArray(count)
        val read = t.source.readFully(byte, out, count)
        return if (read == count) out else out.copyOf(maxOf(read, 0))
    }

    fun ipBinTrackStart(): Int? {
        tracks.firstOrNull { it.number == 3 }?.let { if (hasKatanaHeader(it.startLba)) return it.startLba }
        tracks.filter { it.type == 4 }.minByOrNull { it.startLba }
            ?.let { if (hasKatanaHeader(it.startLba)) return it.startLba }
        return null
    }

    private fun hasKatanaHeader(startLba: Int): Boolean =
        RaDreamcastHasher.isDreamcastHeader(readSector(startLba, 16))

    override fun close() = tracks.forEach { runCatching { it.source.close() } }
}
