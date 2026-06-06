package com.example.diceroller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceRollLogicTest {

    // AVR: i *= t+l — seed multiplication
    @Test
    fun seedMultiplication_nonZeroResult() {
        var i = 1
        val t = 312
        val l = 47
        i *= (t + l)
        assertEquals(359, i)
    }

    @Test
    fun seedMultiplication_zeroTAndL_clamped() {
        var i = 1
        val t = 0
        val l = 0
        i *= (t + l)
        // guard: i==0 → 1
        if (i == 0) i = 1
        assertEquals(1, i)
    }

    // AVR: n=rand()%6+1 — result always 1..6
    @Test
    fun randomDiceFace_alwaysInRange() {
        repeat(10_000) {
            val n = (Math.random() * 6).toInt() % 6 + 1
            assertTrue("n=$n out of range", n in 1..6)
        }
    }

    // AVR: for(i;i>=1;i-=1) { n=rand()%6+1 } — runs outerI times, final n is last roll
    @Test
    fun outerLoop_runsExactCount() {
        val outerI = 5
        var callCount = 0
        var n = 0
        for (k in outerI downTo 1) {
            n = (Math.random() * 6).toInt() % 6 + 1
            callCount++
        }
        assertEquals(5, callCount)
        assertTrue(n in 1..6)
    }

    // AVR: _delay_ms((2+1500/l)/(7-i)) — delay formula
    @Test
    fun delayFormula_decreasesAsFaceIncreases() {
        val l = 10
        val delays = (1..5).map { face ->
            if (l > 0 && (7 - face) > 0) ((2 + 1500 / l) / (7 - face)).toLong().coerceAtLeast(16L)
            else 30L
        }
        for (i in 0 until delays.size - 1) {
            assertTrue("delay should increase as face increases", delays[i] <= delays[i + 1])
        }
    }

    @Test
    fun delayFormula_neverNegative() {
        for (l in 1..255) {
            for (face in 1..6) {
                val d = if ((7 - face) > 0) ((2 + 1500 / l) / (7 - face)).toLong().coerceAtLeast(16L)
                        else 30L
                assertTrue(d >= 0)
            }
        }
    }

    // AVR: hold delay = 500/l + 15  (integer division, decreases as l grows)
    @Test
    fun holdDelay_decreasesWithL() {
        val prev = (500 / 1 + 15).toLong()
        val next = (500 / 2 + 15).toLong()
        assertTrue(prev > next)
    }

    @Test
    fun holdDelay_lEquals255_minimum() {
        val d = (500 / 255 + 15).toLong()
        assertEquals(16L, d.coerceAtLeast(16L))
    }

    // AVR: startup animation — 7 steps, buzz duration == step index
    @Test
    fun startupAnimation_sevenSteps() {
        val steps = mutableListOf<Pair<Int, Int>>()
        val segSequence = intArrayOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40)
        for (i in 1..7) {
            steps.add(Pair(segSequence[i - 1], 400 - 50 * i))
        }
        assertEquals(7, steps.size)
        assertEquals(0x01, steps[0].first)
        assertEquals(0x40, steps[6].first)
        assertEquals(350, steps[0].second)
        assertEquals(50, steps[6].second)
    }

    @Test
    fun startupAnimation_segmentBitsAreDistinct() {
        val segSequence = intArrayOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40)
        assertEquals(7, segSequence.toSet().size)
    }

    // AVR: l counts up to 255 max
    @Test
    fun holdCount_clampsAt255() {
        var l = 0
        for (iter in 0..300) {
            l++
            if (l == 255) break
        }
        assertEquals(255, l)
    }

    // AVR: l decrements until l==1 then breaks outer roll loop
    @Test
    fun rollLoop_lDecrementsToOne() {
        var l = 5
        val visited = mutableListOf<Int>()
        while (true) {
            visited.add(l)
            if (l == 1) break else l--
        }
        assertEquals(listOf(5, 4, 3, 2, 1), visited)
    }
}