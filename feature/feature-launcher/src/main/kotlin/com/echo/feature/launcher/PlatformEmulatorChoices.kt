package com.echo.feature.launcher

import com.echo.core.domain.model.EmulatorProfile

data class EmulatorChoice(
    val profileId: String,
    val name: String,
    val packageName: String,
    val isRecommended: Boolean,
    val isCurrent: Boolean,
)

data class PlatformEmulatorChoices(
    val platformId: String,
    val choices: List<EmulatorChoice>,

    val isAutomatic: Boolean,

    val resolvedName: String?,
    val coreName: String?,
    val isMissingCore: Boolean,
)

fun platformEmulatorChoices(
    platformId: String,
    installedProfiles: List<EmulatorProfile>,
    rememberedCoreId: String?,
    memoryCardEmulatorId: String?,
    platformDefaultPackage: String?,
): PlatformEmulatorChoices {
    val platformProfiles = installedProfiles
        .filter { it.isAvailable && it.supportsPlatform(platformId) }
        .byLaunchPreference()
        .stabilizeCore(rememberedCoreId)

    val stored = memoryCardEmulatorId?.takeIf { it.isNotBlank() }
    val resolved = EmulatorLaunchResolver.resolve(
        platformId = platformId,
        installedProfiles = installedProfiles,
        platformProfiles = platformProfiles,
        memoryCardEmulatorId = stored,
        platformDefault = platformDefaultPackage?.takeIf { it.isNotBlank() },
    ).getOrNull()

    val recommendedId = platformProfiles.firstOrNull()?.id

    return PlatformEmulatorChoices(
        platformId = platformId,
        isAutomatic = stored == null,
        resolvedName = resolved?.profile?.name,
        coreName = resolved?.coreName,
        isMissingCore = resolved?.isMissingCore == true,
        choices = platformProfiles.map { profile ->
            EmulatorChoice(
                profileId = profile.id,
                name = profile.name,
                packageName = profile.packageName,
                isRecommended = profile.id == recommendedId,

                isCurrent = stored != null &&
                    (stored == profile.id || stored == profile.packageName),
            )
        },
    )
}
