package com.psplauncher.feature.achievements.provider.retro

import com.psplauncher.feature.achievements.api.ProviderSyncResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RaRemoteDataSourceTest {
    private val factory = mockk<RaClientFactory>()
    private val dataSource = RaRemoteDataSource(factory)

    @Test
    fun `fetch returns MissingCredentials when RA is not connected`() = runTest {
        coEvery { factory.session() } returns null
        assertEquals(ProviderSyncResult.MissingCredentials, dataSource.fetch("1234"))
    }

    @Test
    fun `hashMap returns null (not an empty map) when RA is not connected`() = runTest {
        coEvery { factory.session() } returns null
        assertEquals(null, dataSource.hashMap(7))
    }
}
