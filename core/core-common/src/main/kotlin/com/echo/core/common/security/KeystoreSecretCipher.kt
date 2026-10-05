package com.echo.core.common.security

import timber.log.Timber

object KeystoreSecretCipher {
    private val aesGcm = KeystoreAesGcm("pfp_secret_key_v1")

    fun seal(plain: String): SealedSecret = sealWith(plain, aesGcm::seal)

    internal fun sealWith(plain: String, seal: (String) -> String): SealedSecret {
        return try {
            SealedSecret.Sealed(seal(plain))
        } catch (e: Exception) {
            Timber.w(e, "Secret encryption failed; storing as-is")
            SealedSecret.Unprotected(plain, e)
        }
    }

    fun isUsableOnThisDevice(stored: String): Boolean = decryptOrLegacy(stored) != null

    // owner, 2026-10-05: a secret sealed by another install (a restored backup, a reinstall) cannot be opened here.
    // It used to be handed on as it was, so SteamGridDB was sent the sealed blob ("Invalid key format") and Twitch
    // an unreadable IGDB secret; now it reads as not set, so the account shows as needing its key again.
    // A value that is not sealed-shaped is an old plain one and is used as it is
    fun decryptOrLegacy(stored: String): String? = readStored(stored, looksSealed(stored)) { aesGcm.open(stored) }

    internal fun readStored(stored: String, sealedShape: Boolean, open: () -> String): String? =
        if (!sealedShape) stored else runCatching(open).getOrNull()

    // sealed values are base64 of an IV, the ciphertext and its tag
    internal fun looksSealed(stored: String): Boolean {
        val data = try {
            java.util.Base64.getDecoder().decode(stored)
        } catch (_: IllegalArgumentException) {
            return false
        }
        return data.size > KeystoreAesGcm.IV_BYTES + KeystoreAesGcm.TAG_BYTES
    }
}
