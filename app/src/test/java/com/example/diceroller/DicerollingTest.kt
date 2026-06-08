package com.example.diceroller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceRollLogicTest {

    @Test
    fun calculateHoldDelay_decreasesAsHoldLevelIncreases() {
        val delayLowHold = DiceLogic.calculateHoldDelay(1)
        val delayHighHold = DiceLogic.calculateHoldDelay(63)
        assertTrue("Higher hold levels should yield shorter delays", delayLowHold > delayHighHold)
    }

    @Test
    fun calculateHoldDelay_clampsTo16msMinimum() {

        val extremeHold = DiceLogic.calculateHoldDelay(1000)
        assertEquals(16L, extremeHold)
    }

    @Test
    fun calculateSegmentIndex_mapsCorrectlyAcrossRange() {
        val animSeqSize = 8
        val maxHoldLevel = 63


        assertEquals(0, DiceLogic.calculateSegmentIndex(1, maxHoldLevel, animSeqSize))

        val midIndex = DiceLogic.calculateSegmentIndex(32, maxHoldLevel, animSeqSize)
        assertTrue(midIndex in 1..6)

        assertEquals(7, DiceLogic.calculateSegmentIndex(63, maxHoldLevel, animSeqSize))

        assertEquals(7, DiceLogic.calculateSegmentIndex(100, maxHoldLevel, animSeqSize))
    }

    @Test
    fun calculateSeed_clampsToMinimum1() {
        assertEquals(50, DiceLogic.calculateSeed(25, 25))
        assertEquals(1, DiceLogic.calculateSeed(0, 0))
        assertEquals(1, DiceLogic.calculateSeed(-5, -10))
    }

    @Test
    fun calculateNextI_staysWithin1to20() {
        val iterations = 1000
        for (i in 0..iterations) {
            val seed = (Math.random() * 1000).toInt() + 1
            val currentI = (Math.random() * 20).toInt() + 1
            val nextI = DiceLogic.calculateNextI(currentI, seed)

            assertTrue("nextI=$nextI out of bounds", nextI in 1..20)
        }
    }

    @Test
    fun calculateFaceDelay_decreasesAsFaceApproachesMax() {
        val holdLevel = 5
        val maxFaces = 6

        val delayFace1 = DiceLogic.calculateFaceDelay(holdLevel, 1, maxFaces)
        val delayFace5 = DiceLogic.calculateFaceDelay(holdLevel, 5, maxFaces)

        assertTrue("Delay should increase as denominator shrinks", delayFace1 < delayFace5)
    }

    @Test
    fun calculateFaceDelay_preventsDivideByZeroAndClamps() {
        val holdLevel = 5
        val maxFaces = 6

        val overFaceDelay = DiceLogic.calculateFaceDelay(holdLevel, 7, maxFaces)
        assertEquals(16L, overFaceDelay)
    }
}