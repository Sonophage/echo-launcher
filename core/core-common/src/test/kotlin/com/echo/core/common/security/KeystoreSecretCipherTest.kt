package com.echo.core.common.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KeystoreSecretCipherTest {
    @Test
    fun `successful seal reports the value as protected`() {
        val result = KeystoreSecretCipher.sealWith("hunter2") { "sealed:$it" }

        assertTrue(result is SealedSecret.Sealed)
        assertEquals("sealed:hunter2", result.stored)
        assertTrue(result.isProtected)
    }

    @Test
    fun `keystore failure reports unprotected rather than silently returning plaintext`() {
        val boom = IllegalStateException("keystore unavailable")

        val result = KeystoreSecretCipher.sealWith("hunter2") { throw boom }

        assertTrue(result is SealedSecret.Unprotected)

        assertEquals("hunter2", result.stored)
        assertEquals(false, result.isProtected)
        assertSame(boom, (result as SealedSecret.Unprotected).cause)
    }

    @Test
    fun `unprotected outcome survives being treated as a plain stored value`() {
        val result = KeystoreSecretCipher.sealWith("  spaced  ") { throw RuntimeException() }

        assertEquals("  spaced  ", result.stored)
    }

    // owner, 2026-10-05: SteamGridDB said "Invalid key format" because a key sealed by another install was sent as is
    @Test
    fun `a sealed secret this install cannot open reads as not set, not as the sealed text`() {
        assertEquals(null, KeystoreSecretCipher.readStored("c2VhbGVk", sealedShape = true) { error("wrong key") })
        assertEquals("plain", KeystoreSecretCipher.readStored("sealed", sealedShape = true) { "plain" })
        assertEquals("an old plain value is used as it is", "abc123", KeystoreSecretCipher.readStored("abc123", sealedShape = false) { error("unused") })
    }

    @Test
    fun `only a long base64 value looks sealed, so plain keys are not mistaken for one`() {
        assertEquals("a 32-character SteamGridDB key is plain", false, KeystoreSecretCipher.looksSealed("0123456789abcdef0123456789abcdef"))
        assertEquals("a token with dots is plain", false, KeystoreSecretCipher.looksSealed("eyJhbGciOi.eyJzdWIiOi.c2ln"))
        assertEquals(true, KeystoreSecretCipher.looksSealed(java.util.Base64.getEncoder().encodeToString(ByteArray(60) { it.toByte() })))
    }
}
