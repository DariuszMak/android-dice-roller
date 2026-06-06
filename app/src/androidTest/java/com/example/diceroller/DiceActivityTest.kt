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

    private fun waitForStartup() = Thread.sleep(3500)

    private fun view(id: Int, block: (View) -> Unit) {
        scenario.onActivity { activity -> block(activity.findViewById(id)) }
    }

    @Test
    fun idleState_hintVisible() {
        waitForStartup()
        view(R.id.tvHint) { v ->
            assertEquals(View.VISIBLE, v.visibility)
            assertEquals("Hold to roll", (v as TextView).text.toString())
        }
    }

    @Test
    fun rollButton_isDisplayed() {
        view(R.id.btnRoll) { v ->
            assertEquals(View.VISIBLE, v.visibility)
        }
    }

    @Test
    fun sevenSegmentView_isDisplayed() {
        view(R.id.sevenSegment) { v ->
            assertEquals(View.VISIBLE, v.visibility)
        }
    }

    @Test
    fun shortPress_triggersRoll() {
        waitForStartup()
        scenario.onActivity { activity ->
            val btn = activity.findViewById<Button>(R.id.btnRoll)
            val now = android.os.SystemClock.uptimeMillis()
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0f, 0f, 0))
            Thread.sleep(100)
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now + 100, MotionEvent.ACTION_UP, 0f, 0f, 0))
        }
        Thread.sleep(6000)
        view(R.id.tvHint) { v ->
            assertEquals("Hold to roll again", (v as TextView).text.toString())
        }
    }

    @Test
    fun longPress_showsReleaseHint() {
        waitForStartup()
        scenario.onActivity { activity ->
            val btn = activity.findViewById<Button>(R.id.btnRoll)
            val now = android.os.SystemClock.uptimeMillis()
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0f, 0f, 0))
        }
        Thread.sleep(400)
        view(R.id.tvHint) { v ->
            assertEquals("Release!", (v as TextView).text.toString())
        }
        scenario.onActivity { activity ->
            val btn = activity.findViewById<Button>(R.id.btnRoll)
            val now = android.os.SystemClock.uptimeMillis()
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 0f, 0f, 0))
        }
    }

    @Test
    fun afterRoll_hintUpdatesToRollAgain() {
        waitForStartup()
        scenario.onActivity { activity ->
            val btn = activity.findViewById<Button>(R.id.btnRoll)
            val now = android.os.SystemClock.uptimeMillis()
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0f, 0f, 0))
        }
        Thread.sleep(800)
        scenario.onActivity { activity ->
            val btn = activity.findViewById<Button>(R.id.btnRoll)
            val now = android.os.SystemClock.uptimeMillis()
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 0f, 0f, 0))
        }
        Thread.sleep(6000)
        view(R.id.tvHint) { v ->
            assertEquals("Hold to roll again", (v as TextView).text.toString())
        }
    }

    @Test
    fun sevenSegmentView_remainsVisibleDuringRoll() {
        waitForStartup()
        scenario.onActivity { activity ->
            val btn = activity.findViewById<Button>(R.id.btnRoll)
            val now = android.os.SystemClock.uptimeMillis()
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0f, 0f, 0))
            Thread.sleep(100)
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now + 100, MotionEvent.ACTION_UP, 0f, 0f, 0))
        }
        Thread.sleep(1000)
        view(R.id.sevenSegment) { v ->
            assertEquals(View.VISIBLE, v.visibility)
        }
    }

    @Test
    fun afterRoll_segmentShowsResult() {
        waitForStartup()
        scenario.onActivity { activity ->
            val btn = activity.findViewById<Button>(R.id.btnRoll)
            val now = android.os.SystemClock.uptimeMillis()
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0f, 0f, 0))
        }
        Thread.sleep(500)
        scenario.onActivity { activity ->
            val btn = activity.findViewById<Button>(R.id.btnRoll)
            val now = android.os.SystemClock.uptimeMillis()
            btn.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 0f, 0f, 0))
        }
        Thread.sleep(6000)
        view(R.id.sevenSegment) { v ->
            assertNotEquals(0, (v as SevenSegmentView).getCurrentBits())
        }
    }
}