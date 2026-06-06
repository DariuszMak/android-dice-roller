package com.example.diceroller

import android.annotation.SuppressLint
import android.content.Context
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

    private var activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile private var buttonPressed = false

    private lateinit var vibrator: Vibrator

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dice)

        segmentView = findViewById(R.id.sevenSegment)
        btnRoll     = findViewById(R.id.btnRoll)
        tvHint      = findViewById(R.id.tvHint)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        activityScope.launch {
            startupAnimation()
            mainLoop()
        }

        btnRoll.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN                           -> { buttonPressed = true;  true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { buttonPressed = false; true }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    // AVR: for(i=1;i<=7;i++) { buzz i ms; delay(400-50*i) ms; shift display }
    private suspend fun startupAnimation() {
        val segSequence = intArrayOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40)
        for (i in 1..7) {
            segmentView.showRaw(segSequence[i - 1])
            buzz(i.toLong())
            delay((400 - 50 * i).toLong())
        }
    }

    private suspend fun mainLoop() {
        var i = 1

        while (currentCoroutineContext().isActive) {

            val w = segmentView.getCurrentBits()
            var d = 1
            var l = 0
            var t = 0

            tvHint.visibility = View.VISIBLE

            var buttonCaught = false
            tLoop@ for (tVal in 0..750) {
                t = tVal

                if (buttonPressed) {
                    buttonCaught = true

                    segmentView.clear()
                    tvHint.text = "Release!"
                    l = 0

                    while (currentCoroutineContext().isActive) {
                        buzz(1L)
                        l++
                        val holdDelay = (500 / l + 15).toLong().coerceAtLeast(16L)
                        val deadline = System.currentTimeMillis() + holdDelay

                        while (System.currentTimeMillis() < deadline) {
                            delay(4)
                        }

                        if (!buttonPressed || l == 255) {
                            break
                        }
                    }

                    break@tLoop
                }

                if (d == 1) {
                    segmentView.showRaw(0x7F)
                } else {
                    segmentView.showRaw(w)
                }

                delay(1)

                if (d == 1) {
                    segmentView.showRaw(w)
                } else {
                    segmentView.showRaw(0x7F)
                }

                delay(1)

                if (t == 750) {
                    t = 0
                    d = if (d == 1) 0 else 1
                }
            }

            if (!buttonCaught) {
                continue
            }

            val seed = (t + l).coerceAtLeast(1)
            i = ((i.toLong() * seed) % 20 + 1).toInt()

            if (l < 1) {
                l = 1
            }

            tvHint.text = ""

            var outerI = i

            while (currentCoroutineContext().isActive) {
                var n = 0

                for (k in outerI downTo 1) {
                    n = Random.nextInt(1, 7)
                }

                for (face in 1..n) {
                    val delayMs =
                        if ((7 - face) > 0) {
                            ((2 + 1500 / l) / (7 - face)).toLong().coerceAtLeast(16L)
                        } else {
                            16L
                        }

                    delay(delayMs)
                    segmentView.showDigit(face)
                    buzz(1L)
                }

                if (l == 1) {
                    break
                } else {
                    l--
                }
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
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}