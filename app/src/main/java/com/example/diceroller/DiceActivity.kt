package com.example.diceroller

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class DiceActivity : AppCompatActivity() {

    private lateinit var segmentView: SevenSegmentView
    private lateinit var btnRoll: Button
    private lateinit var tvHint: TextView

    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile
    private var buttonPressed = false

    @Volatile
    private var rollRequested = false

    private lateinit var vibrator: Vibrator
    private var toneGenerator: ToneGenerator? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dice)

        segmentView = findViewById(R.id.sevenSegment)
        btnRoll = findViewById(R.id.btnRoll)
        tvHint = findViewById(R.id.tvHint)

        // Initialize Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Initialize ToneGenerator for speaker sounds
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)

        btnRoll.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    buttonPressed = true
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (buttonPressed) {
                        rollRequested = true
                    }
                    buttonPressed = false
                    true
                }

                else -> false
            }
        }

        activityScope.launch {
            startupAnimation()
            mainLoop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
        // Release the ToneGenerator to free up audio resources
        toneGenerator?.release()
        toneGenerator = null
    }

    private suspend fun startupAnimation() {
        val segSequence = intArrayOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40)
        for (i in 1..7) {
            segmentView.showRaw(segSequence[i - 1])
            buzz(i.toLong())
            delay(400L - 50L * i)
        }
    }

    private suspend fun mainLoop() {
        var i = 1

        while (currentCoroutineContext().isActive) {

            tvHint.visibility = View.VISIBLE

            var t = 0
            var buttonCaught = false
            var holdLevel = 0

            tLoop@ for (tVal in 0..750) {
                t = tVal

                if (buttonPressed) {
                    buttonCaught = true
                    segmentView.clear()
                    tvHint.text = "Release!"
                    holdLevel = 0

                    while (currentCoroutineContext().isActive) {
                        buzz(1L)
                        holdLevel++

                        val holdDelay = (500 / holdLevel + 15).toLong().coerceAtLeast(16L)
                        val deadline = System.currentTimeMillis() + holdDelay

                        while (System.currentTimeMillis() < deadline) {
                            delay(4)
                        }

                        if (!buttonPressed || holdLevel == 255) break
                    }

                    break@tLoop
                }

                // Replacing the manual 0x7F / delay(1) toggling to fix the flicker.
                // A simple delay(2) keeps your total loop time and randomness seed intact
                // without refreshing the screen unnecessarily.
                delay(2)
            }


            if (rollRequested) {
                rollRequested = false
                buttonCaught = true
            }

            if (!buttonCaught) continue

            val seed = (t + holdLevel).coerceAtLeast(1)
            i = ((i.toLong() * seed) % 20 + 1).toInt()

            tvHint.text = ""

            var outerI = i
            var n = 0

            while (currentCoroutineContext().isActive) {

                for (k in outerI downTo 1) {
                    n = Random.nextInt(1, 7)
                }

                for (face in 1..n) {
                    val delayMs =
                        if ((7 - face) > 0) {
                            ((2 + 1500 / holdLevel) / (7 - face)).toLong().coerceAtLeast(16L)
                        } else 16L

                    delay(delayMs)
                    segmentView.showDigit(face)
                    buzz(1L)
                }

                if (holdLevel == 1) break else holdLevel--
            }

            delay(30L)

            segmentView.showDp = !segmentView.showDp
            buzz(1L)

            delay(750)

            segmentView.showDp = false

            tvHint.text = "Hold to roll again"
            tvHint.visibility = View.VISIBLE
        }
    }

    private fun buzz(durationMs: Long) {
        // 1. Play Sound
        // TONE_CDMA_PRESS_CLICK or TONE_PROP_BEEP are good options for quick "ticks/buzzes"
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs.toInt())

        // 2. Play Haptic Vibration
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}