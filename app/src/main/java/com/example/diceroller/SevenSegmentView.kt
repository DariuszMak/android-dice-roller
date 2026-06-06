package com.example.diceroller

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SevenSegmentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val DIGIT_SEGMENTS = intArrayOf(
        0x3F, 0x06, 0x5B, 0x4F, 0x66,
        0x6D, 0x7D, 0x07, 0x7F, 0x6F
    )

    private val paintOn = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4400")
        style = Paint.Style.FILL
    }

    private val paintOff = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#140600")
        style = Paint.Style.FILL
    }

    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22FF4400")
        style = Paint.Style.FILL
    }

    private var segmentBits: Int = 0

    var showDp: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    fun showDigit(n: Int) {
        segmentBits = if (n in 0..9) DIGIT_SEGMENTS[n] else 0
        invalidate()
    }

    fun showRaw(bits: Int) {
        segmentBits = bits and 0x7F
        invalidate()
    }

    fun clear() {
        segmentBits = 0
        invalidate()
    }

    private fun isOn(bit: Int) = (segmentBits and (1 shl bit)) != 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        
        val widthScale = 0.72f
        val heightScale = 0.90f

        val dw = w * widthScale
        val dh = h * heightScale

        val offsetX = (w - dw) / 2f
        val offsetY = (h - dh) / 2f

        
        val t = dw * 0.12f

        
        val gap = t * 0.35f

        val left = offsetX + gap
        val right = offsetX + dw - gap
        val top = offsetY + gap
        val bottom = offsetY + dh - gap

        val midX = (left + right) / 2f
        val midY = (top + bottom) / 2f

        fun drawH(x1: Float, x2: Float, y: Float, on: Boolean) {
            val fill = if (on) paintOn else paintOff
            val glow = if (on) paintGlow else paintOff

            val r = RectF(x1, y - t / 2, x2, y + t / 2)

            canvas.drawRoundRect(r, t / 2, t / 2, glow)
            canvas.drawRoundRect(
                RectF(r.left + 2, r.top + 2, r.right - 2, r.bottom - 2),
                t / 2,
                t / 2,
                fill
            )
        }

        fun drawV(x: Float, y1: Float, y2: Float, on: Boolean) {
            val fill = if (on) paintOn else paintOff
            val glow = if (on) paintGlow else paintOff

            val r = RectF(x - t / 2, y1, x + t / 2, y2)

            canvas.drawRoundRect(r, t / 2, t / 2, glow)
            canvas.drawRoundRect(
                RectF(r.left + 2, r.top + 2, r.right - 2, r.bottom - 2),
                t / 2,
                t / 2,
                fill
            )
        }

        // A
        drawH(left, right, top, isOn(0))
        // B
        drawV(right, top, midY, isOn(1))
        // C
        drawV(right, midY, bottom, isOn(2))
        // D
        drawH(left, right, bottom, isOn(3))
        // E
        drawV(left, midY, bottom, isOn(4))
        // F
        drawV(left, top, midY, isOn(5))
        // G
        drawH(left, right, midY, isOn(6))

        
        val dpR = t * 0.45f
        val dpX = right + dpR * 1.6f
        val dpY = bottom
        canvas.drawCircle(dpX, dpY, dpR, if (showDp) paintOn else paintOff)
    }
}