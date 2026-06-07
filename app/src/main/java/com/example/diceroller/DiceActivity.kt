package com.example.diceroller

import android.annotation.SuppressLint
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

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

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

    private fun resetButtonState() {
        buttonPressed = false
        rollRequested = false
        val bg = btnRoll.background
        btnRoll.background = null
        btnRoll.background = bg
        btnRoll.isPressed = false
        btnRoll.isEnabled = false
        btnRoll.refreshDrawableState()
    }

    private suspend fun mainLoop() {
        val maxFaces = 6

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

                        val animSeq = intArrayOf(0x40, 0x41, 0x43, 0x47, 0x4F, 0x5F, 0x7F, 0x7F)

                        val maxHoldLevel = 63
                        val segmentIndex =
                            (((holdLevel - 1).toFloat() / (maxHoldLevel - 1)) * animSeq.size)
                                .toInt()
                                .coerceIn(0, animSeq.lastIndex)

                        segmentView.showRaw(animSeq[segmentIndex])

                        val holdDelay = (500 / holdLevel + 15).toLong().coerceAtLeast(16L)
                        val deadline = System.currentTimeMillis() + holdDelay

                        while (System.currentTimeMillis() < deadline) {
                            delay(4)
                        }

                        if (!buttonPressed || holdLevel == 63) break
                    }

                    break@tLoop
                }
                delay(2)
            }

            if (rollRequested) {
                rollRequested = false
                buttonCaught = true
            }

            if (!buttonCaught) continue

            holdLevel = holdLevel.coerceAtLeast(1)

            val seed = (t + holdLevel).coerceAtLeast(1)
            i = ((i.toLong() * seed) % 20 + 1).toInt()

            resetButtonState()
            tvHint.text = ""

            var outerI = i
            var n = 0
            var face = 1

            while (currentCoroutineContext().isActive) {

                for (k in outerI downTo 1) {
                    n = Random.nextInt(1, maxFaces + 1)
                }

                face = 1
                while (face <= n) {
                    val denominator = (maxFaces + 1) - face
                    val delayMs = if (denominator > 0) {
                        ((2 + 1500 / holdLevel) / denominator).toLong().coerceAtLeast(16L)
                    } else 16L

                    delay(delayMs)
                    segmentView.showDigit(face)
                    buzz(1L)

                    face++
                }

                if (holdLevel == 1) break else holdLevel--
            }

            val suspenseDenominator = (maxFaces + 1) - face

            if (suspenseDenominator > 0) {
                val suspenseDelayMs =
                    ((2 + 1500 / holdLevel) / suspenseDenominator).toLong().coerceAtLeast(16L)
                delay(suspenseDelayMs)
            }

            delay(30L)

            segmentView.showDp = !segmentView.showDp
            buzz(1L)

            delay(750)

            segmentView.showDp = false

            btnRoll.clearFocus()
            btnRoll.isPressed = false
            btnRoll.isEnabled = true
            tvHint.text = "Hold to roll again"
            tvHint.visibility = View.VISIBLE
        }
    }

    private fun buzz(durationMs: Long) {
        val prolonged_duration = durationMs * 100

        toneGenerator?.stopTone()
        toneGenerator?.startTone(
            ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE,
            prolonged_duration.toInt()
        )

        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(prolonged_duration, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(prolonged_duration)
        }
    }
}