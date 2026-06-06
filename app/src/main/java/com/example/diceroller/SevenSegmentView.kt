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
        0x3F, // 0
        0x06, // 1
        0x5B, // 2
        0x4F, // 3
        0x66, // 4
        0x6D, // 5
        0x7D, // 6
        0x07, // 7
        0x7F, // 8
        0x6F  // 9
    )

    private val paintOn = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4400")
        style = Paint.Style.FILL
    }

    private val paintOff = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A0800")
        style = Paint.Style.FILL
    }

    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FF4400")
        style = Paint.Style.FILL
    }

    private var segmentBits: Int = 0

    var showDp: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    fun getCurrentBits(): Int = segmentBits

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val thickness = w * 0.12f
        val pad = thickness * 0.35f

        val left = pad
        val right = w - pad
        val top = pad
        val bottom = h - pad

        val midX = (left + right) / 2f
        val midY = (top + bottom) / 2f

        val segLenH = right - left
        val segLenV = midY - top

        fun segOn(bit: Int) = (segmentBits and (1 shl bit)) != 0

        fun drawH(x1: Float, x2: Float, y: Float, on: Boolean) {
            val paintFill = if (on) paintOn else paintOff
            val glow = if (on) paintGlow else paintOff

            val rect = RectF(
                x1,
                y - thickness / 2,
                x2,
                y + thickness / 2
            )

            canvas.drawRoundRect(rect, thickness / 2, thickness / 2, glow)
            canvas.drawRoundRect(
                RectF(rect.left + 2, rect.top + 2, rect.right - 2, rect.bottom - 2),
                thickness / 2,
                thickness / 2,
                paintFill
            )
        }

        fun drawV(x: Float, y1: Float, y2: Float, on: Boolean) {
            val paintFill = if (on) paintOn else paintOff
            val glow = if (on) paintGlow else paintOff

            val rect = RectF(
                x - thickness / 2,
                y1,
                x + thickness / 2,
                y2
            )

            canvas.drawRoundRect(rect, thickness / 2, thickness / 2, glow)
            canvas.drawRoundRect(
                RectF(rect.left + 2, rect.top + 2, rect.right - 2, rect.bottom - 2),
                thickness / 2,
                thickness / 2,
                paintFill
            )
        }

        // Segment layout (standard 7-seg)
        // A (top)
        drawH(left, right, top, segOn(0))

        // B (top-right)
        drawV(right, top, midY, segOn(1))

        // C (bottom-right)
        drawV(right, midY, bottom, segOn(2))

        // D (bottom)
        drawH(left, right, bottom, segOn(3))

        // E (bottom-left)
        drawV(left, midY, bottom, segOn(4))

        // F (top-left)
        drawV(left, top, midY, segOn(5))

        // G (middle)
        drawH(left, right, midY, segOn(6))

        // Decimal point
        val dpRadius = thickness * 0.45f
        val dpX = right + dpRadius * 1.4f
        val dpY = bottom
        canvas.drawCircle(dpX, dpY, dpRadius, if (showDp) paintOn else paintOff)
    }
}