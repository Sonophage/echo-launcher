package com.psplauncher.feature.achievements.match

import com.psplauncher.core.domain.model.Game
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.fail

class RaHashVerification {
    private data class Spec(val platformId: String, val path: String, val expected: String?)

    private data class Outcome(val spec: Spec, val actual: String?, val note: String)

    @Test
    fun `verify RA hashes against real files when a manifest is supplied`() {
        val specs = readSpecs() ?: return
        if (specs.isEmpty()) return

        val outcomes = specs.map { evaluate(it) }
        val report = buildReport(outcomes)

        println(report)
        val out = outputFile()
        runCatching { out.apply { parentFile?.mkdirs() }.writeText(report) }
            .onSuccess { println("Wrote report to ${out.absolutePath}") }

        val failures = outcomes.filter { it.spec.expected != null && !it.note.startsWith("MATCH") }
        if (failures.isNotEmpty()) {
            fail("${failures.size} hash verification(s) failed:\n" +
                failures.joinToString("\n") { "  ${it.spec.platformId}: ${it.note} (${it.spec.path})" })
        }
    }

    private fun evaluate(spec: Spec): Outcome {
        val file = File(spec.path)
        if (!file.exists()) return Outcome(spec, null, "MISSING FILE")
        val actual = runCatching { computeHash(spec.platformId, file) }
            .getOrElse { return Outcome(spec, null, "ERROR: ${it.message}") }
            ?: return Outcome(spec, null, "NOT HASHABLE (unsupported platform or unidentified image)")

        val note = when {
            spec.expected == null -> "computed"
            spec.expected.equals(actual, ignoreCase = true) -> "MATCH"
            else -> "MISMATCH (expected ${spec.expected.lowercase()})"
        }
        return Outcome(spec, actual, note)
    }

    private fun computeHash(platformId: String, file: File): String? {
        if (file.extension.equals("chd", ignoreCase = true)) {
            val chd = ChdReader.open(DiscImage.rawSource(file)) ?: return null
            val sectors = ChdSectorSource.of(chd) ?: run { chd.close(); return null }
            return DiscImage.openTracks(sectors, sectors.firstTrackSector).use { img ->
                when {
                    RaSegaDiscHasher.isSupported(platformId) -> RaSegaDiscHasher.hash(img)
                    RaDreamcastHasher.isSupported(platformId) -> RaDreamcastHasher.hash(img)
                    RaDiscHasher.isSupported(platformId) -> RaDiscHasher.hash(platformId, img)
                    else -> null
                }
            }
        }
        return when {
        RaRomHasher.isSupported(platformId) ->

            if (platformId == "nds") DiscImage.rawSource(file).use { RaRomHasher.hashNds(it) }
            else RaRomHasher.hash(platformId, file.readBytes())

        RaNintendoDiscHasher.isSupported(platformId) ->
            DiscImage.rawSource(file).use { RaNintendoDiscHasher.hash(platformId, it) }

        RaSegaDiscHasher.isSupported(platformId) ->
            DiscImage.openRawCd(DiscImage.rawSource(file)).use { RaSegaDiscHasher.hash(it) }

        RaDreamcastHasher.isSupported(platformId) -> {
            val game = Game(id = 0, title = file.name, platformId = platformId, romPath = file.absolutePath)
            runBlocking { DiscImageOpener(mockk(relaxed = true)).openGdi(game) }
                ?.use { RaDreamcastHasher.hash(it) }
        }

        RaDiscHasher.isSupported(platformId) ->
            DiscImage.open(file)?.use { RaDiscHasher.hash(platformId, it) }

        else -> null
        }
    }

    private fun config(sysKey: String, envKey: String): String? =
        System.getenv(envKey)?.takeIf { it.isNotBlank() } ?: System.getProperty(sysKey)?.takeIf { it.isNotBlank() }

    private fun readSpecs(): List<Spec>? {
        val manifest = config("ra.hash.manifest", "RA_HASH_MANIFEST")?.let(::File)?.takeIf(File::exists)
        val lines: List<String> = when {
            manifest != null -> manifest.readLines()
            else -> config("ra.hash.spec", "RA_HASH_SPEC")?.split(';') ?: return null
        }
        return lines
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { parseLine(it) }
    }

    private fun parseLine(line: String): Spec? {
        val parts = line.split('|').map { it.trim() }
        if (parts.size < 2 || parts[0].isEmpty() || parts[1].isEmpty()) return null
        return Spec(parts[0], parts[1], parts.getOrNull(2)?.takeIf { it.isNotEmpty() })
    }

    private fun outputFile(): File =
        File(config("ra.hash.out", "RA_HASH_OUT") ?: "build/ra-hash-verification.txt")

    private fun buildReport(outcomes: List<Outcome>): String = buildString {
        appendLine("RetroAchievements hash verification")
        appendLine("=".repeat(72))
        outcomes.forEach { o ->
            appendLine("platform : ${o.spec.platformId}")
            appendLine("file     : ${o.spec.path}")
            appendLine("computed : ${o.actual ?: "-"}")
            if (o.spec.expected != null) appendLine("expected : ${o.spec.expected.lowercase()}")
            appendLine("result   : ${o.note}")
            appendLine("-".repeat(72))
        }
        val matched = outcomes.count { it.note == "MATCH" }
        val checked = outcomes.count { it.spec.expected != null }
        if (checked > 0) appendLine("Verified $matched/$checked against expected hashes.")
    }
}
