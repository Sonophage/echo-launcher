package com.psplauncher.feature.achievements.provider.steam

import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Response

class SteamAppListResolverTest {
    private val storeApi = mockk<SteamStoreApi>()
    private val resolver = SteamAppListResolver(storeApi)

    private fun results(vararg items: StoreItem) {
        coEvery { storeApi.search(any(), any(), any()) } returns
            Response.success(StoreSearchResponse(items.toList()))
    }

    @Test
    fun `resolveAppId links only on an exact normalized name match`() = runTest {
        results(
            StoreItem(id = 620, name = "Portal 2", type = "app"),
            StoreItem(id = 400, name = "Portal", type = "app"),
        )
        assertEquals("400", resolver.resolveAppId("PORTAL"))
        assertEquals("620", resolver.resolveAppId("Portal  2"))
        assertNull(resolver.resolveAppId("Portal 3"))
    }

    @Test
    fun `a throttled store search is a failure, not a game that is missing from Steam`() = runTest {
        coEvery { storeApi.search(any(), any(), any()) } returns Response.error(429, "{}".toResponseBody())
        assertThrows(IOException::class.java) { runBlocking { resolver.resolveAppId("Portal") } }
    }
}
