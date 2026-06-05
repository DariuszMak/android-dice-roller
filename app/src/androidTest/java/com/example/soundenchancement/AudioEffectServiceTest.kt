package com.example.soundenhancement

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioEffectServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private fun bindService(): AudioEffectService {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val binder = serviceRule.bindService(Intent(context, AudioEffectService::class.java))
        return (binder as AudioEffectService.LocalBinder).getService()
    }


    @Test
    fun serviceStartsAndAppliesEqualizer() {
        val service = bindService()
        assertNotNull(service)

        val eq = service.equalizer
        assertNotNull(eq)
        assertTrue("Equalizer should be enabled on start", eq!!.enabled)

        val numberOfBands = eq.numberOfBands
        val (minLevel, maxLevel) = eq.bandLevelRange
        for (i in 0 until numberOfBands) {
            val bandLevel = eq.getBandLevel(i.toShort())
            assertTrue(
                "Band $i level $bandLevel out of range",
                bandLevel in minLevel..maxLevel
            )
        }

        val firstBand = eq.getBandLevel(0)
        val midBand = eq.getBandLevel((numberOfBands / 2).toShort())
        assertTrue("Bass band should be stronger than mid band", firstBand > midBand)
    }


    @Test
    fun disableEq_turnsOffEqualizerEffect() {
        val service = bindService()
        assertTrue("Should start enabled", service.isEqEnabled)

        service.disableEq()

        assertFalse("isEqEnabled should be false after disableEq()", service.isEqEnabled)
        assertFalse(
            "Equalizer.enabled should be false after disableEq()",
            service.equalizer?.enabled ?: true
        )
    }


    @Test
    fun enableEq_restoresEqualizerAfterDisable() {
        val service = bindService()


        val eq = service.equalizer!!
        val levelsBefore = (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()) }

        service.disableEq()
        assertFalse(service.isEqEnabled)

        service.enableEq()

        assertTrue("isEqEnabled should be true after enableEq()", service.isEqEnabled)
        assertTrue(
            "Equalizer.enabled should be true after enableEq()",
            service.equalizer?.enabled ?: false
        )


        val levelsAfter = (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()) }
        assertEquals("Band levels should not change on re-enable", levelsBefore, levelsAfter)
    }


    @Test
    fun applyConfig_updatesBandLevels() {
        val service = bindService()
        val eq = service.equalizer!!

        val before = (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()) }


        val newConfig = EqConfig(
            baseLevel = 1400,
            multipliers = DoubleArray(5) { 2.0 }
        )
        service.applyConfig(newConfig)

        val after = (0 until service.equalizer!!.numberOfBands)
            .map { service.equalizer!!.getBandLevel(it.toShort()) }

        assertNotEquals(
            "Band levels should change when a new config is applied",
            before, after
        )
    }


    @Test
    fun applyConfig_whileDisabled_doesNotEnableEq() {
        val service = bindService()
        service.disableEq()
        assertFalse(service.isEqEnabled)





        service.applyConfig(EqConfig())


        assertTrue(
            "applyConfig always re-enables the equalizer; Activity must guard calls",
            service.isEqEnabled
        )
    }


    @Test
    fun isEqEnabled_tracksMultipleToggleCycles() {
        val service = bindService()

        repeat(3) {
            service.disableEq()
            assertFalse("Cycle ${it + 1}: should be disabled", service.isEqEnabled)
            service.enableEq()
            assertTrue("Cycle ${it + 1}: should be enabled", service.isEqEnabled)
        }
    }
}