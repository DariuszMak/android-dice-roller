package com.example.diceroller

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
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

    // After startup animation, hint "Hold to roll" must be visible
    @Test
    fun idleState_hintVisible() {
        Thread.sleep(3500)  // wait for startup animation (~350+300+250+200+150+100+50 ms)
        onView(withId(R.id.tvHint)).check(matches(isDisplayed()))
        onView(withId(R.id.tvHint)).check(matches(withText("Hold to roll")))
    }

    // Roll button is visible and clickable
    @Test
    fun rollButton_isDisplayed() {
        onView(withId(R.id.btnRoll)).check(matches(isDisplayed()))
    }

    // SevenSegmentView is visible
    @Test
    fun sevenSegmentView_isDisplayed() {
        onView(withId(R.id.sevenSegment)).check(matches(isDisplayed()))
    }

    // Short tap triggers roll sequence: hint changes away from "Hold to roll"
    @Test
    fun shortPress_triggersRoll() {
        Thread.sleep(3500)
        onView(withId(R.id.btnRoll)).perform(click())
        Thread.sleep(4000)
        onView(withId(R.id.tvHint)).check(matches(withText("Hold to roll again")))
    }

    // Long press shows "Release!" hint while held
    @Test
    fun longPress_showsReleaseHint() {
        Thread.sleep(3500)
        scenario.onActivity { activity ->
            activity.findViewById<android.widget.Button>(R.id.btnRoll)
                .dispatchTouchEvent(
                    android.view.MotionEvent.obtain(
                        0, 0, android.view.MotionEvent.ACTION_DOWN, 0f, 0f, 0
                    )
                )
        }
        Thread.sleep(300)
        onView(withId(R.id.tvHint)).check(matches(withText("Release!")))
        scenario.onActivity { activity ->
            activity.findViewById<android.widget.Button>(R.id.btnRoll)
                .dispatchTouchEvent(
                    android.view.MotionEvent.obtain(
                        0, 0, android.view.MotionEvent.ACTION_UP, 0f, 0f, 0
                    )
                )
        }
    }

    // After roll completes, hint updates to "Hold to roll again"
    @Test
    fun afterRoll_hintUpdatesToRollAgain() {
        Thread.sleep(3500)
        scenario.onActivity { activity ->
            val btn = activity.findViewById<android.widget.Button>(R.id.btnRoll)
            val downEvent = android.view.MotionEvent.obtain(
                0, 500, android.view.MotionEvent.ACTION_DOWN, 0f, 0f, 0
            )
            val upEvent = android.view.MotionEvent.obtain(
                0, 500, android.view.MotionEvent.ACTION_UP, 0f, 0f, 0
            )
            btn.dispatchTouchEvent(downEvent)
            Thread.sleep(800)
            btn.dispatchTouchEvent(upEvent)
        }
        Thread.sleep(6000)
        onView(withId(R.id.tvHint)).check(matches(withText("Hold to roll again")))
    }

    // SevenSegmentView stays visible throughout a roll
    @Test
    fun sevenSegmentView_remainsVisibleDuringRoll() {
        Thread.sleep(3500)
        onView(withId(R.id.btnRoll)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.sevenSegment)).check(matches(isDisplayed()))
    }
}