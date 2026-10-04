package com.echo.core.data.discord

import com.echo.core.data.network.NetworkMonitor
import com.echo.core.domain.discord.DeviceAuthChallenge
import com.echo.core.domain.discord.DeviceLoginState
import com.echo.core.domain.discord.DeviceTokens
import com.echo.core.domain.discord.DiscordSession
import com.echo.core.domain.discord.DiscordSessionActivator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DiscordAuthRepositoryTest {

    private fun activator(): DiscordSessionActivator = mockk(relaxed = true) {
        coEvery { activate(any()) } returns true
    }

    private fun onlineMonitor(online: Boolean = true) =
        mockk<NetworkMonitor> { every { isOnline() } returns online }

    private fun repo(
        client: DiscordDeviceAuthClient,
        store: DiscordTokenStore = mockk(relaxed = true),
        activator: DiscordSessionActivator = activator(),
        monitor: NetworkMonitor = onlineMonitor(),
    ) = DiscordAuthRepository(client, store, activator, monitor)

    private val challenge = DeviceAuthChallenge(
        userCode = "WXYZ-1234",
        verificationUri = "https://discord.com/activate",
        verificationUriComplete = "https://discord.com/activate?user_code=WXYZ-1234",
        deviceCode = "DEV123",
        expiresInSeconds = 300,
        pollIntervalSeconds = 1,
    )
    private val tokens = DeviceTokens("AT-abc", "RT-xyz", 604800, "openid sdk.social_layer")

    @Test
    fun `approval after pending emits Success, persists tokens and activates the session`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        val store = mockk<DiscordTokenStore>(relaxed = true)
        val activator = activator()
        coEvery { client.requestDeviceCode(any()) } returns challenge
        coEvery { client.pollForToken("DEV123") } returnsMany
            listOf(TokenPollResult.Pending, TokenPollResult.Approved(tokens))

        val states = repo(client, store, activator).loginWithDeviceQr().toList()

        assertEquals(DeviceLoginState.Requesting, states.first())
        assertIs<DeviceLoginState.AwaitingApproval>(states[1])
        assertIs<DeviceLoginState.Success>(states.last())
        coVerify { activator.activate("AT-abc") }
        coVerify { store.save(tokens, any()) }
    }

    @Test
    fun `denied poll emits Denied and never persists`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        val store = mockk<DiscordTokenStore>(relaxed = true)
        coEvery { client.requestDeviceCode(any()) } returns challenge
        coEvery { client.pollForToken(any()) } returns TokenPollResult.Denied

        val states = repo(client, store).loginWithDeviceQr().toList()

        assertEquals(DeviceLoginState.Denied, states.last())
        coVerify(exactly = 0) { store.save(any(), any()) }
    }

    @Test
    fun `expired poll emits Expired`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        coEvery { client.requestDeviceCode(any()) } returns challenge
        coEvery { client.pollForToken(any()) } returns TokenPollResult.Expired

        val states = repo(client).loginWithDeviceQr().toList()

        assertEquals(DeviceLoginState.Expired, states.last())
    }

    @Test
    fun `code lifetime elapsing while pending emits Expired`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        coEvery { client.requestDeviceCode(any()) } returns challenge.copy(expiresInSeconds = 2, pollIntervalSeconds = 1)
        coEvery { client.pollForToken(any()) } returns TokenPollResult.Pending

        val states = repo(client).loginWithDeviceQr().toList()

        assertEquals(DeviceLoginState.Expired, states.last())
    }

    @Test
    fun `failure requesting the device code emits Error`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        coEvery { client.requestDeviceCode(any()) } throws RuntimeException("boom")

        val states = repo(client).loginWithDeviceQr().toList()

        assertEquals(DeviceLoginState.Requesting, states.first())
        val error = assertIs<DeviceLoginState.Error>(states.last())
        assertEquals("boom", error.message)
    }

    @Test
    fun `offline emits an offline Error without touching the network`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()

        val states = repo(client, monitor = onlineMonitor(online = false)).loginWithDeviceQr().toList()

        assertIs<DeviceLoginState.Error>(states.last())
        coVerify(exactly = 0) { client.requestDeviceCode(any()) }
    }

    @Test
    fun `restoreSession without a stored session never wakes the SDK`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        val store = mockk<DiscordTokenStore>(relaxed = true)
        val activator = activator()
        coEvery { store.load() } returns null

        assertFalse(repo(client, store, activator).restoreSession())
        coVerify(exactly = 0) { activator.activate(any()) }
    }

    @Test
    fun `restoreSession activates a still-valid token without refreshing`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        val store = mockk<DiscordTokenStore>(relaxed = true)
        val activator = activator()
        coEvery { store.load() } returns
            DiscordSession("AT-live", "RT", System.currentTimeMillis() + 3_600_000, "openid")

        assertTrue(repo(client, store, activator).restoreSession())
        coVerify { activator.activate("AT-live") }
        coVerify(exactly = 0) { client.refreshTokens(any()) }
    }

    @Test
    fun `restoreSession refreshes an expired token, persists and activates the new one`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        val store = mockk<DiscordTokenStore>(relaxed = true)
        val activator = activator()
        coEvery { store.load() } returns DiscordSession("AT-old", "RT-xyz", 0L, "openid")
        val fresh = DeviceTokens("AT-new", "RT-new", 604800, "openid")
        coEvery { client.refreshTokens("RT-xyz") } returns TokenPollResult.Approved(fresh)

        assertTrue(repo(client, store, activator).restoreSession())
        coVerify { activator.activate("AT-new") }
        coVerify { store.save(fresh, any()) }
    }

    @Test
    fun `restoreSession leaves the session in place when the refresh fails for a non auth reason`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        val store = mockk<DiscordTokenStore>(relaxed = true)
        coEvery { store.load() } returns DiscordSession("AT-old", "RT", 0L, "openid")
        coEvery { client.refreshTokens(any()) } returns TokenPollResult.Error("refresh_failed (503)")

        assertFalse(repo(client, store).restoreSession())
        coVerify(exactly = 0) { store.save(any(), any()) }
        coVerify(exactly = 0) { store.clear() }
    }

    @Test
    fun `a revoked refresh token clears the session so the app shows signed out, not signed in with no friends`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        val store = mockk<DiscordTokenStore>(relaxed = true)
        coEvery { store.load() } returns DiscordSession("AT-old", "RT", 0L, "openid")
        coEvery { client.refreshTokens(any()) } returns TokenPollResult.Error("invalid_grant")

        assertFalse(repo(client, store).restoreSession())
        coVerify { store.clear() }
    }

    @Test
    fun `a token that expires within minutes is refreshed before it lapses`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        val store = mockk<DiscordTokenStore>(relaxed = true)
        coEvery { store.load() } returns
            DiscordSession("AT-old", "RT-xyz", System.currentTimeMillis() + 60_000, "openid")
        val fresh = DeviceTokens("AT-new", "RT-new", 604800, "openid")
        coEvery { client.refreshTokens("RT-xyz") } returns TokenPollResult.Approved(fresh)

        repo(client, store).refreshIfExpiring()

        coVerify { store.save(fresh, any()) }
    }

    @Test
    fun `refreshIfExpiring leaves a live session alone so a resume does not reconnect the SDK`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        val store = mockk<DiscordTokenStore>(relaxed = true)
        val activator = activator()
        coEvery { store.load() } returns
            DiscordSession("AT-live", "RT", System.currentTimeMillis() + 3_600_000, "openid")

        repo(client, store, activator).refreshIfExpiring()

        coVerify(exactly = 0) { client.refreshTokens(any()) }
        coVerify(exactly = 0) { activator.activate(any()) }
    }

    @Test
    fun `restoreSession does not attempt a refresh while offline`() = runTest {
        val client = mockk<DiscordDeviceAuthClient>()
        val store = mockk<DiscordTokenStore>(relaxed = true)
        coEvery { store.load() } returns DiscordSession("AT-old", "RT", 0L, "openid")

        assertFalse(repo(client, store, monitor = onlineMonitor(online = false)).restoreSession())
        coVerify(exactly = 0) { client.refreshTokens(any()) }
    }
}
