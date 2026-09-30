package com.psplauncher.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.psplauncher.core.data.datastore.pfpDataStore
import com.psplauncher.core.domain.model.ConfirmBackLayout
import com.psplauncher.core.domain.model.ControllerDisplayType
import com.psplauncher.core.domain.model.ControllerLayoutPrefs
import com.psplauncher.core.domain.model.ScrollSpeed
import com.psplauncher.core.domain.model.ShoulderHoldTime
import com.psplauncher.core.domain.model.StickSensitivity
import com.psplauncher.core.domain.model.TriggerSensitivity
import com.psplauncher.core.domain.model.XYLayout
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ControllerResetTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var repository: ControllerLayoutRepository

    @Before
    fun setUp() {
        runBlocking { context.pfpDataStore.edit { it.clear() } }
        repository = ControllerLayoutRepository(context, mockk(relaxed = true))
    }

    @Test
    fun `Reset All Controller Settings puts every setting back to its default`() = runTest {
        repository.setConfirmBackLayout(ConfirmBackLayout.REVERSED)
        repository.setXYLayout(XYLayout.SWAPPED)
        repository.setDisplayType(ControllerDisplayType.NINTENDO)
        repository.setScrollSpeed(ScrollSpeed.FAST)
        repository.setStickSensitivity(StickSensitivity.LOW)
        repository.setTriggerSensitivity(TriggerSensitivity.HIGH)
        repository.setShoulderHoldTime(ShoulderHoldTime.LONG)
        repository.setLeftBacksOut(false)

        val changed = repository.prefs.first()
        val defaults = ControllerLayoutPrefs()
        ControllerLayoutPrefs::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .forEach { field ->
                field.isAccessible = true
                assertTrue(
                    field.get(changed) != field.get(defaults),
                    "${field.name} was not moved off its default, so this test cannot see whether reset clears it",
                )
            }

        repository.resetAllPrefs()

        assertEquals(defaults, repository.prefs.first(), "reset left a controller setting changed")
    }

    @Test
    fun `a new controller setting has to be added to reset and to this test`() {
        val fields = ControllerLayoutPrefs::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
        assertEquals(
            8,
            fields.size,
            "ControllerLayoutPrefs is now $fields. Clear the new key in resetAllPrefs and set it in the test above",
        )
    }
}
