package com.example.diceroller

import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class DiceActivityTest {

    private lateinit var scenario: ActivityScenario<DiceActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(DiceActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    private fun waitForStartup() = Thread.sleep(3500)

    private fun press() {
        scenario.onActivity { activity ->
            val btn = activity.findViewById<Button>(R.id.btnRoll)
            val now = android.os.SystemClock.uptimeMillis()
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0f, 0f, 0))
        }
    }

    private fun release() {
        scenario.onActivity { activity ->
            val btn = activity.findViewById<Button>(R.id.btnRoll)
            val now = android.os.SystemClock.uptimeMillis()
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 0f, 0f, 0))
        }
    }

    private fun hintText(): String {
        var text = ""
        scenario.onActivity { activity ->
            text = activity.findViewById<TextView>(R.id.tvHint).text.toString()
        }
        return text
    }

    private fun waitForHint(expected: String, timeoutMs: Long = 30000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (hintText() == expected) return
            Thread.sleep(200)
        }
        assertEquals(expected, hintText())
    }

    @Test
    fun idleState_hintVisible() {
        waitForStartup()
        var vis = -1
        scenario.onActivity { activity ->
            vis = activity.findViewById<TextView>(R.id.tvHint).visibility
        }
        assertEquals(View.VISIBLE, vis)
        assertEquals("Hold to roll", hintText())
    }

    @Test
    fun rollButton_isDisplayed() {
        var vis = -1
        scenario.onActivity { activity ->
            vis = activity.findViewById<Button>(R.id.btnRoll).visibility
        }
        assertEquals(View.VISIBLE, vis)
    }

    @Test
    fun sevenSegmentView_isDisplayed() {
        var vis = -1
        scenario.onActivity { activity ->
            vis = activity.findViewById<SevenSegmentView>(R.id.sevenSegment).visibility
        }
        assertEquals(View.VISIBLE, vis)
    }

    @Test
    fun shortPress_triggersRoll() {
        waitForStartup()
        press()
        Thread.sleep(200)
        release()
        waitForHint("Hold to roll again")
    }

    @Test
    fun longPress_showsReleaseHint() {
        waitForStartup()
        press()
        Thread.sleep(400)
        assertEquals("Release!", hintText())
        release()
    }

    @Test
    fun afterRoll_hintUpdatesToRollAgain() {
        waitForStartup()
        press()
        Thread.sleep(800)
        release()
        waitForHint("Hold to roll again")
    }

    @Test
    fun sevenSegmentView_remainsVisibleDuringRoll() {
        waitForStartup()
        press()
        Thread.sleep(200)
        release()
        Thread.sleep(1000)
        var vis = -1
        scenario.onActivity { activity ->
            vis = activity.findViewById<SevenSegmentView>(R.id.sevenSegment).visibility
        }
        assertEquals(View.VISIBLE, vis)
    }

    @Test
    fun afterRoll_segmentShowsResult() {
        waitForStartup()
        press()
        Thread.sleep(500)
        release()
        waitForHint("Hold to roll again")
        var bits = -1
        scenario.onActivity { activity ->
            bits = activity.findViewById<SevenSegmentView>(R.id.sevenSegment).getCurrentBits()
        }
        assertNotEquals(0, bits)
    }
}