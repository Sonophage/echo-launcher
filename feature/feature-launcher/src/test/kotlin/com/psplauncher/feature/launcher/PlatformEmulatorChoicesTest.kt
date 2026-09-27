package com.psplauncher.feature.launcher

import com.psplauncher.core.domain.model.EmulatorProfile
import com.psplauncher.core.domain.model.IntentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformEmulatorChoicesTest {
    private fun profile(
        id: String,
        packageName: String = id,
        platforms: List<String> = listOf("psx"),
        available: Boolean = true,
    ) = EmulatorProfile(
        id                   = id,
        name                 = id,
        packageName          = packageName,
        intentType           = IntentType.ACTION_VIEW,
        supportedPlatformIds = platforms,
        isAvailable          = available,
    )

    private fun choices(
        installed: List<EmulatorProfile>,
        stored: String? = null,
        platformDefault: String? = null,
        rememberedCore: String? = null,
    ) = platformEmulatorChoices(
        platformId = "psx",
        installedProfiles = installed,
        rememberedCoreId = rememberedCore,
        memoryCardEmulatorId = stored,
        platformDefaultPackage = platformDefault,
    )

    @Test
    fun `only emulators that are installed and claim the platform are offered`() {
        val result = choices(
            listOf(
                profile("duckstation"),
                profile("dolphin", platforms = listOf("gc", "wii")),
                profile("uninstalled", available = false),
            ),
        )

        assertEquals(
            "offering an emulator that cannot run the console, or is not installed, gives the " +
                "owner a choice that fails at launch",
            listOf("duckstation"),
            result.choices.map { it.profileId },
        )
    }

    @Test
    fun `no stored emulator means the console is on Automatic`() {
        val result = choices(listOf(profile("duckstation")))

        assertTrue("an unset console follows the recommendation and must read that way", result.isAutomatic)
        assertTrue(
            "Automatic is not a choice in the list, so nothing in it may claim to be current",
            result.choices.none { it.isCurrent },
        )
    }

    @Test
    fun `a stored emulator marks exactly that one current, and leaves Automatic`() {
        val result = choices(
            listOf(profile("duckstation"), profile("retroarch-psx")),
            stored = "retroarch-psx",
        )

        assertFalse(result.isAutomatic)
        assertEquals(
            listOf("retroarch-psx"),
            result.choices.filter { it.isCurrent }.map { it.profileId },
        )
    }

    @Test
    fun `a stored emulator recorded by package still matches its profile`() {
        val result = choices(
            listOf(profile("ds-profile", packageName = "com.duckstation")),
            stored = "com.duckstation",
        )

        assertTrue(
            "setEmulator has written both ids and packages over time, so a row keyed only by id " +
                "would silently show the console as Automatic while it is not",
            result.choices.single().isCurrent,
        )
    }

    @Test
    fun `the recommendation is the first by launch preference, and only one row carries it`() {
        val result = choices(listOf(profile("a"), profile("b"), profile("c")))

        assertEquals(1, result.choices.count { it.isRecommended })
        assertEquals(
            "the recommended row must be the one the resolver would pick on its own",
            result.choices.first().profileId,
            result.choices.single { it.isRecommended }.profileId,
        )
    }

    @Test
    fun `a console with nothing installed offers nothing and is still Automatic`() {
        val result = choices(listOf(profile("dolphin", platforms = listOf("gc"))))

        assertTrue(result.choices.isEmpty())
        assertTrue("with no candidates there is nothing to be non-automatic about", result.isAutomatic)
        assertEquals(null, result.resolvedName)
    }
}
