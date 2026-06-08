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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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


    private fun waitForCondition(timeoutMs: Long = 30000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(50)
        }
        assertTrue("Condition not met within timeout", condition())
    }

    private fun waitForStartup() = Thread.sleep(3500)

    private fun dispatchTouch(action: Int) {
        scenario.onActivity { activity ->
            val btn = activity.findViewById<Button>(R.id.btnRoll)
            val now = android.os.SystemClock.uptimeMillis()
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now, action, 0f, 0f, 0))
        }
    }

    private fun press() = dispatchTouch(MotionEvent.ACTION_DOWN)
    private fun release() = dispatchTouch(MotionEvent.ACTION_UP)
    private fun cancelTouch() = dispatchTouch(MotionEvent.ACTION_CANCEL)

    private fun hintText(): String {
        var text = ""
        scenario.onActivity { activity ->
            text = activity.findViewById<TextView>(R.id.tvHint).text.toString()
        }
        return text
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
    fun rollButton_isDisabledDuringRoll() {
        waitForStartup()
        press()
        Thread.sleep(200)
        release()

        var isEnabled = true
        waitForCondition(timeoutMs = 2000) {
            scenario.onActivity { activity ->
                isEnabled = activity.findViewById<Button>(R.id.btnRoll).isEnabled
            }
            !isEnabled
        }

        assertFalse(isEnabled)
    }

    @Test
    fun actionCancel_actsLikeReleaseAndTriggersRoll() {
        waitForStartup()
        press()
        Thread.sleep(200)
        cancelTouch()

        waitForCondition { hintText() == "Hold to roll again" }
        assertEquals("Hold to roll again", hintText())
    }

    @Test
    fun multipleRolls_executeSuccessfully() {
        waitForStartup()


        press()
        Thread.sleep(200)
        release()
        waitForCondition { hintText() == "Hold to roll again" }


        press()
        Thread.sleep(200)
        release()
        waitForCondition { hintText() == "Hold to roll again" }

        var bits = -1
        scenario.onActivity { activity ->
            bits = activity.findViewById<SevenSegmentView>(R.id.sevenSegment).getSegmentBits()
        }
        assertNotEquals(0, bits)
    }

    @Test
    fun decimalPoint_blinksAfterRoll() {
        waitForStartup()
        press()
        Thread.sleep(200)
        release()


        var sawDpTrue = false
        waitForCondition {
            scenario.onActivity { activity ->
                if (activity.findViewById<SevenSegmentView>(R.id.sevenSegment).showDp) {
                    sawDpTrue = true
                }
            }
            sawDpTrue || hintText() == "Hold to roll again"
        }

        assertTrue("Decimal point should have blinked to true during resolution phase", sawDpTrue)


        waitForCondition { hintText() == "Hold to roll again" }
        scenario.onActivity { activity ->
            assertFalse(activity.findViewById<SevenSegmentView>(R.id.sevenSegment).showDp)
        }
    }
}