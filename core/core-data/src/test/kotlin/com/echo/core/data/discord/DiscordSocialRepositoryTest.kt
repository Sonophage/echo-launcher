package com.echo.core.data.discord

import com.echo.core.domain.discord.DiscordFriend
import com.echo.core.domain.discord.DiscordPresence
import com.echo.core.domain.discord.DiscordSessionActivator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordSocialRepositoryTest {

    private val friend = DiscordFriend("1", "ada", "Ada", "", DiscordPresence.ONLINE, "Doom II")

    @Test
    fun `signed out shows no friends and never wakes the SDK, signed in reads it`() = runTest {
        val signedIn = MutableStateFlow(false)
        val store = mockk<DiscordTokenStore> { every { hasSession } returns signedIn }
        val activator = mockk<DiscordSessionActivator> { coEvery { friends() } returns listOf(friend) }
        val repo = DiscordSocialRepository(store, activator)

        assertEquals(emptyList(), repo.friends.first())
        coVerify(exactly = 0) { activator.friends() }

        signedIn.value = true
        assertEquals(listOf(friend), repo.friends.first())
    }
}
