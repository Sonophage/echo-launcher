package com.psplauncher.feature.achievements.provider.steam

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `a failed store call resolves to nothing`() = runTest {
        coEvery { storeApi.search(any(), any(), any()) } throws RuntimeException("boom")
        assertNull(resolver.resolveAppId("Portal"))
    }
}
