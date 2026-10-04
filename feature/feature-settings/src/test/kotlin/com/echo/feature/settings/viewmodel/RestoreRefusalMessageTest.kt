package com.echo.feature.settings.viewmodel

import androidx.work.workDataOf
import com.echo.feature.backup.RestoreWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestoreRefusalMessageTest {
    @Test fun `a restore that refused items must say so`() {
        val output = workDataOf(
            RestoreWorker.KEY_REFUSALS to arrayOf(
                "Refused emulator profile 'Hostile': launches this app",
                "Refused 'datastore/x': not inside a restorable folder",
            )
        )
        assertEquals(
            "a partial restore must not read as complete",
            "2 items were not restored: Refused emulator profile 'Hostile': launches this app; " +
                "Refused 'datastore/x': not inside a restorable folder",
            restoreRefusalMessage(output),
        )
    }

    @Test fun `one refusal is counted in the singular`() {
        val output = workDataOf(RestoreWorker.KEY_REFUSALS to arrayOf("Refused emulator profiles: file could not be parsed"))
        assertEquals("1 item was not restored: Refused emulator profiles: file could not be parsed", restoreRefusalMessage(output))
    }

    @Test fun `a complete restore shows no message`() {
        assertNull(restoreRefusalMessage(workDataOf(RestoreWorker.KEY_REFUSALS to emptyArray<String>())))
        assertNull(restoreRefusalMessage(workDataOf()))
    }
}
