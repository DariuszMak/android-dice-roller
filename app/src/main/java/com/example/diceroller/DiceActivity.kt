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

    // AVR state equivalents
    private var currentResult: Int = 0

    // Vibrator helper
    private lateinit var vibrator: Vibrator

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dice)

        segmentView = findViewById(R.id.sevenSegment)
        btnRoll = findViewById(R.id.btnRoll)
        tvHint = findViewById(R.id.tvHint)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Startup animation — mirrors the for(i=1;i<=7;i++) init loop in AVR
        // AVR: PORTA starts at 0xFE and shifts left each iteration, toggling one LED on/off
        // We animate segment display cycling through 7 positions
        activityScope.launch {
            runStartupAnimation()
            showIdleState()
        }

        // Touch listener mirrors AVR button logic:
        //   - On press: start counting l (hold duration) while buzzing with increasing frequency
        //   - On release: break out, compute random seed from t+l, run slot machine countdown
        btnRoll.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    activityScope.cancel()
                    activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
                    activityScope.launch { runButtonHold() }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Signal release is detected inside runButtonHold via isActive checks
                    // We cancel the hold coroutine and let it finalize
                    activityScope.cancel()
                    activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
                    activityScope.launch { runRollSequence() }
                    true
                }

                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    // AVR: for(i=1;i<=7;i++) { PORTB|=0x10; delay(i); PORTB&=~0x10; delay(400-50*i); PORTA<<=1; }
    // Each iteration: buzz for i ms, wait (400-50*i) ms, shift display segment
    private suspend fun runStartupAnimation() {
        val segSequence = intArrayOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40) // a through g
        for (i in 1..7) {
            segmentView.showRaw(segSequence[i - 1])
            buzz(i.toLong())
            delay((400 - 50 * i).toLong())
        }
    }

    // AVR idle: blink PORTA between 0xFF and w with increasing duty cycle
    // We show a pulsing "8" on the display
    private suspend fun showIdleState() {
        tvHint.text = "Hold to roll"
        tvHint.visibility = View.VISIBLE
        var bright = true
        while (currentCoroutineContext().isActive) {
            segmentView.showRaw(if (bright) 0x7F else 0x00)
            delay(500)
            bright = !bright
        }
    }

    // AVR: while button held, PORTA=0x00, buzz with period (500/l)+15 ms, l counts up
    // Maps to: display off, vibrate with decreasing interval as l grows
    private suspend fun runButtonHold() {
        segmentView.clear()
        tvHint.text = "Release!"
        var l = 0
        while (currentCoroutineContext().isActive) {
            buzz(1L)
            l++
            val waitMs = (500.0 / l + 15).toLong().coerceAtLeast(16L)
            delay(waitMs)
            if (l == 255) break
        }
    }

    // AVR slot machine:
    //   i *= t+l  (seed based on timing and hold duration)
    //   outer loop counts down i, picking random n each iteration
    //   inner loop shows digits 1..n with decreasing delay
    //   l decrements each outer pass until l==1
    //
    // We approximate: use System.nanoTime() as entropy source (replaces t+l),
    // run slot machine passes with shrinking delays
    private suspend fun runRollSequence() {
        tvHint.text = ""
        val entropy = (System.nanoTime() % 1000).toInt() + 1  // mirrors t+l
        var passes = (entropy % 6 + 3)                          // 3..8 passes (mirrors i countdown)
        var l = passes                                           // mirrors l (hold count proxy)

        // Roll slot machine
        while (passes > 0) {
            val n = Random.nextInt(1, 7)  // rand()%6+1
            currentResult = n

            for (face in 1..n) {
                val delayMs = ((2 + 1500.0 / l) / (7 - face)).toLong().coerceAtLeast(30L)
                delay(delayMs)
                segmentView.showDigit(face)
                buzz(1L)
            }

            if (l == 1) break
            l--
            passes--
        }

        // AVR final: PORTA ^= 0x80 (toggle dp), buzz, wait 750ms
        segmentView.showDigit(currentResult)
        segmentView.showDp = true
        buzz(2L)
        delay(750)
        segmentView.showDp = false
        tvHint.text = "Hold to roll again"
        tvHint.visibility = View.VISIBLE
    }

    // Maps to PORTB |= 0x10 / delay(ms) / PORTB &= ~0x10
    private fun buzz(durationMs: Long) {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}
