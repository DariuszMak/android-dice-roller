package com.example.diceroller

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SevenSegmentViewTest {

    private lateinit var scenario: ActivityScenario<DiceActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(DiceActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun showDigit_mapsCorrectBits() {
        
        val expectedBits = mapOf(
            0 to 0x3F, 1 to 0x06, 2 to 0x5B, 3 to 0x4F, 4 to 0x66,
            5 to 0x6D, 6 to 0x7D, 7 to 0x07, 8 to 0x7F, 9 to 0x6F
        )

        scenario.onActivity { activity ->
            val view = activity.findViewById<SevenSegmentView>(R.id.sevenSegment)

            expectedBits.forEach { (digit, expectedHex) ->
                view.showDigit(digit)
                assertEquals("Digit $digit failed", expectedHex, view.getSegmentBits())
            }

            
            view.showDigit(15)
            assertEquals(0, view.getSegmentBits())
        }
    }

    @Test
    fun showRaw_masksAndAppliesCorrectly() {
        scenario.onActivity { activity ->
            val view = activity.findViewById<SevenSegmentView>(R.id.sevenSegment)

            
            view.showRaw(0x7F)
            assertEquals(0x7F, view.getSegmentBits())

            
            
            view.showRaw(0xFF)
            assertEquals(0x7F, view.getSegmentBits())
        }
    }

    @Test
    fun clear_resetsBitsToZero() {
        scenario.onActivity { activity ->
            val view = activity.findViewById<SevenSegmentView>(R.id.sevenSegment)
            view.showDigit(8) 
            assertEquals(0x7F, view.getSegmentBits())

            view.clear()
            assertEquals(0, view.getSegmentBits())
        }
    }

    @Test
    fun showDp_togglesBooleanFlag() {
        scenario.onActivity { activity ->
            val view = activity.findViewById<SevenSegmentView>(R.id.sevenSegment)

            assertFalse(view.showDp)

            view.showDp = true
            assertTrue(view.showDp)
        }
    }
}