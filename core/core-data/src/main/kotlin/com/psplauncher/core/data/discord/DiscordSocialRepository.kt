package com.psplauncher.core.data.discord

import com.psplauncher.core.domain.discord.DiscordFriend
import com.psplauncher.core.domain.discord.DiscordSessionActivator
import com.psplauncher.core.domain.discord.DiscordUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DiscordSocialRepository @Inject constructor(
    tokenStore: DiscordTokenStore,
    private val sessionActivator: DiscordSessionActivator,
) {
    val signedIn: Flow<Boolean> = tokenStore.hasSession

    val user: Flow<DiscordUser?> = whileSignedIn(null) { sessionActivator.currentUser() }

    val friends: Flow<List<DiscordFriend>> = whileSignedIn(emptyList()) { sessionActivator.friends() }

    private fun <T> whileSignedIn(signedOut: T, read: suspend () -> T): Flow<T> =
        signedIn.flatMapLatest { on ->
            if (!on) flowOf(signedOut) else flow {
                while (true) {
                    emit(read())
                    delay(POLL_MS)
                }
            }
        }.distinctUntilChanged()

    private companion object {
        const val POLL_MS = 5_000L
    }
}
