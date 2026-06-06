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

    // Mirrors PINC & 0x01 — true when button NOT pressed (pin high = released)
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
                MotionEvent.ACTION_DOWN                      -> { buttonPressed = true;  true }
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

    // AVR: main while(1) loop
    private suspend fun mainLoop() {
        // AVR: char n,w,d,l; int t,i;  — i implicitly 1 after startup for loop
        var i = 1

        while (currentCoroutineContext().isActive) {

            // AVR: w=PORTA; d=1; l=0;
            val w = segmentView.getCurrentBits()
            var d = 1
            var l = 0
            var t = 0

            tvHint.visibility = View.VISIBLE
            tvHint.text = "Hold to roll"

            // AVR: for(t=0;t<=750;t+=1) — idle brightness sweep, ~563 ms per sweep pass
            // Each iteration: _delay_us(t) + _delay_us(750-t) = 750µs total
            // We scale to ms-level but preserve the 0..750 range and d/w toggling
            var buttonCaught = false
            tLoop@ for (tVal in 0..750) {
                t = tVal

                // AVR: if(!(PINC & 0x01)) — button pressed (active low)
                if (buttonPressed) {
                    buttonCaught = true

                    // AVR: PORTA=0x00; while(1) { buzz; ++l; delay(500/l+15); if(released || l==255) break }
                    segmentView.clear()
                    tvHint.text = "Release!"
                    l = 0

                    while (currentCoroutineContext().isActive) {
                        buzz(1L)
                        l++
                        // AVR: _delay_ms(500/l + 15) — integer division
                        val holdDelay = (500 / l + 15).toLong().coerceAtLeast(16L)
                        // poll for release within the hold delay window
                        val deadline = System.currentTimeMillis() + holdDelay
                        while (System.currentTimeMillis() < deadline) {
                            delay(4)
                        }
                        if (!buttonPressed || l == 255) break
                    }

                    // AVR: break — exits the for(t) loop
                    break@tLoop
                }

                // AVR brightness pulse using d and w
                // d==1: show 0xFF briefly (t µs), then w for (750-t) µs
                // d==0: show w briefly (t µs), then 0xFF for (750-t) µs
                if (d == 1) segmentView.showRaw(0x7F) else segmentView.showRaw(w)
                delay(1)   // compressed from _delay_us(t)
                if (d == 1) segmentView.showRaw(w) else segmentView.showRaw(0x7F)
                delay(1)   // compressed from _delay_us(750-t)

                // AVR: if(t==750) { t=0; d ^= 1; }  — t=0 then loop t++ → 1 next iter
                if (t == 750) {
                    t = 0
                    d = if (d == 1) 0 else 1
                }
            }

            if (!buttonCaught) continue

            // AVR: i *= t+l
            i *= (t + l)
            if (i == 0) i = 1     // guard: Android Random can't handle 0-count loop

            tvHint.text = ""

            // AVR: while(1) { for(i;i>=1;i--) { n=rand()%6+1; }  inner 1..n; if(l==1) break else --l; }
            var outerI = i
            while (currentCoroutineContext().isActive) {
                var n = 0
                // AVR: for(i;i>=1;i-=1) { n=rand()%6+1; } — runs outerI times, keeps last n
                for (k in outerI downTo 1) {
                    n = Random.nextInt(1, 7)
                }

                // AVR: for(i=1;i<=n;i++) — reuses i as inner loop var
                for (face in 1..n) {
                    // AVR: _delay_ms((2+1500/l)/(7-i))
                    val delayMs = if (l > 0 && (7 - face) > 0) {
                        ((2 + 1500 / l) / (7 - face)).toLong().coerceAtLeast(16L)
                    } else {
                        30L
                    }
                    delay(delayMs)
                    segmentView.showDigit(face)
                    buzz(1L)
                }

                if (l == 1) break else l--
            }

            // AVR: _delay_ms((2+1500/l)/(7-i))  — i is n here (last inner loop value = n, up to 6)
            val postDelay = if (l > 0 && (7 - i % 7) > 0) {
                ((2 + 1500 / l) / (7 - (i % 6).coerceAtLeast(1))).toLong().coerceAtLeast(16L)
            } else {
                30L
            }
            delay(postDelay)

            // AVR: PORTA ^= 0x80  — toggle dp bit
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