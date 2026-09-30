package com.psplauncher.feature.settings.permissions

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPermissionsTest {
    @Test
    fun `every row says what the permission is for`() {
        // A row that cannot say what it buys the user is a row nobody can act on.
        AppPermissions.ALL.forEach {
            assertTrue("${it.id} has no reason", it.why.isNotBlank())
            assertTrue("${it.id} has no label", it.label.isNotBlank())
        }
    }

    @Test
    fun `ids are unique, or two rows fight over one focus key`() {
        val ids = AppPermissions.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `a requestable permission names the manifest permission it asks for`() {
        AppPermissions.ALL.filter { it.route == GrantRoute.REQUEST }.forEach {
            assertTrue(
                "${it.id} is requestable but names no permission to request",
                it.manifestName?.startsWith("android.permission.") == true,
            )
        }
    }

    @Test
    fun `the two special accesses are not requestable, because only settings can grant them`() {
        listOf(AppPermissions.USAGE_ACCESS, AppPermissions.NOTIFICATION_LISTENER).forEach { id ->
            val row = AppPermissions.ALL.first { it.id == id }
            assertEquals(GrantRoute.SYSTEM_SCREEN, row.route)
        }
    }

    @Test
    fun `the media permissions are hidden on the Android versions that do not have them`() {
        val onTiramisu = AppPermissions.forSdk(Build.VERSION_CODES.TIRAMISU).map { it.id }
        val onAndroid10 = AppPermissions.forSdk(Build.VERSION_CODES.Q).map { it.id }

        assertTrue("read_media_audio" in onTiramisu)
        assertFalse("split media permissions did not exist before 13", "read_media_audio" in onAndroid10)

        assertTrue("storage is how it was done before 13", "read_external_storage" in onAndroid10)
        assertFalse("and is no longer how it is done", "read_external_storage" in onTiramisu)
    }

    @Test
    fun `an install-time row reads as unavailable rather than pretending it can be granted`() {
        assertEquals("Granted", permissionStateLabel(true, GrantRoute.INSTALL_TIME))
        assertEquals("Unavailable", permissionStateLabel(false, GrantRoute.INSTALL_TIME))
        assertEquals("Not granted", permissionStateLabel(false, GrantRoute.REQUEST))
        assertEquals("Not granted", permissionStateLabel(false, GrantRoute.SYSTEM_SCREEN))
    }

    @Test
    fun `every row the manifest backs is actually declared in the manifest`() {
        // A row for a permission the app never declares would sit at Not granted for
        // ever, and tapping it would ask for something the system will refuse.
        val manifest = java.io.File("../../app/src/main/AndroidManifest.xml")
            .takeIf { it.exists() }
            ?: java.io.File("app/src/main/AndroidManifest.xml")
        if (!manifest.exists()) return
        val text = manifest.readText()
        AppPermissions.ALL.mapNotNull { it.manifestName }.forEach {
            assertTrue("$it is on the permissions screen but not in the manifest", text.contains(it))
        }
    }
}
