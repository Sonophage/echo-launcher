package com.echo.feature.achievements.match

class ChdSectorSource private constructor(
    private val chd: ChdReader,
    private val tracks: List<Track>,
    private val framesPerHunk: Int,

    val firstTrackSector: Int,
) : DiscImage.SectorSource {
    private class Track(
        val startFad: Int,
        val endFad: Int,
        val chdFrameStart: Int,
        val userOffset: Int,
        val isData: Boolean,
    )

    override fun readSector(lba: Int, count: Int): ByteArray {
        val fad = lba + PREGAP_FRAMES
        val track = tracks.firstOrNull { it.isData && fad >= it.startFad && fad <= it.endFad }
            ?: return ByteArray(0)
        val chdFrame = fad - track.startFad + track.chdFrameStart
        val hunk = runCatching { chd.readHunk(chdFrame / framesPerHunk) }.getOrNull() ?: return ByteArray(0)
        val frameOffset = (chdFrame % framesPerHunk) * ChdReader.CD_FRAME_SIZE + track.userOffset

        val out = ByteArray(count)
        val avail = minOf(count, hunk.size - frameOffset).coerceAtLeast(0)
        if (avail > 0) System.arraycopy(hunk, frameOffset, out, 0, avail)
        return if (avail == count) out else out.copyOf(avail)
    }

    override fun close() = chd.close()

    companion object {
        private const val PREGAP_FRAMES = 150
        private const val CD_TRACK_PADDING = 4
        private const val HIGH_DENSITY_FAD = 45000

        private val TYPE = Regex("""TYPE:(\S+)""")
        private val FRAMES = Regex("""FRAMES:(\d+)""")

        fun of(chd: ChdReader): ChdSectorSource? {
            val hunkBytes = chd.header.hunkBytes
            if (hunkBytes == 0 || hunkBytes % ChdReader.CD_FRAME_SIZE != 0) return null
            if (chd.header.compressed && chd.header.compressors.none {
                    it == ChdReader.CODEC_CD_ZLIB || it == ChdReader.CODEC_CD_LZMA ||
                        it == ChdReader.CODEC_ZLIB || it == ChdReader.CODEC_LZMA || it == ChdReader.CODEC_NONE
                }
            ) {
                return null
            }
            val tracks = parseTracks(chd)
            val dataTracks = tracks.filter { it.isData }
            if (dataTracks.isEmpty()) return null

            val isoTrack = dataTracks.firstOrNull { it.startFad >= HIGH_DENSITY_FAD } ?: dataTracks.first()
            return ChdSectorSource(
                chd, tracks, hunkBytes / ChdReader.CD_FRAME_SIZE, isoTrack.startFad - PREGAP_FRAMES,
            )
        }

        private fun parseTracks(chd: ChdReader): List<Track> {
            val tracks = mutableListOf<Track>()
            var totalFrames = PREGAP_FRAMES
            var chdOffset = 0
            var index = 0
            while (true) {
                val text = chd.metadata(ChdReader.CDROM_TRACK_METADATA2_TAG, index)
                    ?: chd.metadata(ChdReader.CDROM_TRACK_METADATA_TAG, index)
                    ?: chd.metadata(ChdReader.GDROM_TRACK_METADATA_TAG, index)
                    ?: chd.metadata(ChdReader.GDROM_OLD_METADATA_TAG, index)
                    ?: break
                val type = TYPE.find(text)?.groupValues?.get(1) ?: break
                val frames = FRAMES.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: break

                val startFad = totalFrames
                totalFrames += frames
                tracks += Track(
                    startFad = startFad,
                    endFad = totalFrames - 1,
                    chdFrameStart = chdOffset,
                    userOffset = userDataOffset(type),
                    isData = type != "AUDIO",
                )
                chdOffset += ((frames + CD_TRACK_PADDING - 1) / CD_TRACK_PADDING) * CD_TRACK_PADDING
                index++
            }
            return tracks
        }

        private fun userDataOffset(type: String): Int = when {
            type.startsWith("MODE1") && !type.contains("RAW") && !type.contains("2352") -> 0
            type.startsWith("MODE1") -> 16
            type.contains("2336") -> 8
            type.startsWith("MODE2") || type.startsWith("CDI") -> 24
            else -> 16
        }
    }
}
