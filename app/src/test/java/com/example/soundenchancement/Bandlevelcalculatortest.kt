package com.example.soundenhancement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class BandLevelCalculatorTest {


    private val minLevel = -1500
    private val maxLevel = 1500
    private val baseLevel = 700

    @Test
    fun result_neverExceedsMaxLevel() {
        val testFrequencies =
            listOf(30.0, 60.0, 120.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
        for (freq in testFrequencies) {
            val level = calculateBandLevel(freq, baseLevel, minLevel, maxLevel)
            assertTrue("Band at ${freq}Hz exceeded maxLevel: $level", level <= maxLevel)
        }
    }

    @Test
    fun result_neverBelowMinLevel() {
        val testFrequencies =
            listOf(30.0, 60.0, 120.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
        for (freq in testFrequencies) {
            val level = calculateBandLevel(freq, baseLevel, minLevel, maxLevel)
            assertTrue("Band at ${freq}Hz was below minLevel: $level", level >= minLevel)
        }
    }

    @Test
    fun subBass_strongerThan_midrange() {
        val subBass = calculateBandLevel(40.0, baseLevel, minLevel, maxLevel)
        val midrange = calculateBandLevel(1000.0, baseLevel, minLevel, maxLevel)
        assertTrue(
            "Sub-bass (40 Hz) should be louder than midrange (1 kHz): $subBass vs $midrange",
            subBass > midrange
        )
    }

    @Test
    fun bass_strongerThan_midrange() {
        val bass = calculateBandLevel(80.0, baseLevel, minLevel, maxLevel)
        val midrange = calculateBandLevel(500.0, baseLevel, minLevel, maxLevel)
        assertTrue(
            "Bass (80 Hz) should be louder than midrange (500 Hz): $bass vs $midrange",
            bass > midrange
        )
    }

    @Test
    fun exactBoundary_60Hz_usesSubBassMultiplier() {

        val at60 = calculateBandLevel(60.0, baseLevel, minLevel, maxLevel)
        val at61 = calculateBandLevel(61.0, baseLevel, minLevel, maxLevel)
        assertTrue(
            "60 Hz (sub-bass branch) should be >= 61 Hz (bass branch): $at60 vs $at61",
            at60 >= at61
        )
    }

    @Test
    fun zeroBaseLevel_returnsMinLevel() {

        val level = calculateBandLevel(1000.0, 0, minLevel, maxLevel)
        assertEquals("Zero baseLevel should yield minLevel", minLevel.toShort(), level)
    }

    @Test
    fun symmetricRange_minEqualsNegativeMax() {

        val level = calculateBandLevel(60.0, baseLevel, -1500, 1500)
        assertTrue(level in -1500..1500)
    }

    @Test
    fun highFreq_strongerThan_midrange() {
        val highFreq = calculateBandLevel(16000.0, baseLevel, minLevel, maxLevel)
        val midrange = calculateBandLevel(1000.0, baseLevel, minLevel, maxLevel)
        assertTrue(
            "High freq (16 kHz) should be louder than midrange (1 kHz): $highFreq vs $midrange",
            highFreq > midrange
        )
    }

    @Test
    fun sameInputs_alwaysReturnSameOutput() {
        val first = calculateBandLevel(440.0, baseLevel, minLevel, maxLevel)
        val second = calculateBandLevel(440.0, baseLevel, minLevel, maxLevel)
        assertEquals("calculateBandLevel must be deterministic", first, second)
    }

    @Test
    fun bass_different_values_for_same_frequency() {
        val midRangeLowerLevel = calculateBandLevel(500.0, baseLevel - 1, minLevel, maxLevel)
        val midRangeHigherLevel = calculateBandLevel(500.0, baseLevel, minLevel, maxLevel)
        assertTrue(
            "Lower mid range shold be more silent higher midrange (500 Hz): $midRangeLowerLevel vs $midRangeHigherLevel",
            midRangeLowerLevel < midRangeHigherLevel
        )
    }
}