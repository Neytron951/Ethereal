package com.neytron.ethereal

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neytron.ethereal.data.BackupRepository
import com.neytron.ethereal.data.SettingsRepository
import org.json.JSONArray
import org.json.JSONObject

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.neytron.ethereal", appContext.packageName)
    }

    @Test
    fun malformedBackupDoesNotPartiallyApplySettings() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val originalPalette = SettingsRepository.getPaletteId(appContext)
        val malformedBackup = JSONObject()
            .put("version", 2)
            .put("settings", JSONObject().put("palette_id", "must_not_be_applied"))
            .put("spaces", JSONArray())
            .put("layouts", JSONObject())

        assertFalse(BackupRepository.importFromJson(appContext, malformedBackup.toString()))
        assertEquals(originalPalette, SettingsRepository.getPaletteId(appContext))
    }
}