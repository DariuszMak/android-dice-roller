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

    // Segment bitmask constants matching AVR 7-segment encoding (active-low, so inverted)
    // Segments: a=bit0, b=bit1, c=bit2, d=bit3, e=bit4, f=bit5, g=bit6, dp=bit7
    // AVR used ~value on PORTA, so we store the un-inverted values here
    private val DIGIT_SEGMENTS = intArrayOf(
        0x3F, // 0: a,b,c,d,e,f
        0x06, // 1: b,c
        0x5B, // 2: a,b,d,e,g
        0x4F, // 3: a,b,c,d,g
        0x66, // 4: b,c,f,g
        0x6D, // 5: a,c,d,f,g
        0x7D, // 6: a,c,d,e,f,g
        0x07, // 7: a,b,c
        0x7F, // 8: all
        0x6F  // 9: a,b,c,d,f,g
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
            field = value; invalidate()
        }

    fun showDigit(n: Int) {
        segmentBits = if (n in 0..9) DIGIT_SEGMENTS[n] else 0
        invalidate()
    }

    fun showRaw(bits: Int) {
        segmentBits = bits
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

        val thickness = w * 0.10f
        val gap = thickness * 0.15f
        val hLen = w * 0.60f
        val vLen = h * 0.42f
        val cx = w / 2f

        // Segment layout helpers
        fun segOn(bit: Int) = (segmentBits and (1 shl bit)) != 0

        fun drawH(canvas: Canvas, cx: Float, cy: Float, on: Boolean) {
            val paint = if (on) paintOn else paintOff
            val gx = gap * 1.5f
            val left = cx - hLen / 2 + gx
            val right = cx + hLen / 2 - gx
            val top = cy - thickness / 2
            val bot = cy + thickness / 2
            val r = thickness / 2
            canvas.drawRoundRect(
                RectF(left, top, right, bot),
                r,
                r,
                if (on) paintGlow.apply { this.color = 0x22FF4400.toInt() } else paintOff)
            canvas.drawRoundRect(RectF(left + 1, top + 1, right - 1, bot - 1), r, r, paint)
        }

        fun drawV(canvas: Canvas, x: Float, topY: Float, on: Boolean) {
            val paint = if (on) paintOn else paintOff
            val gy = gap * 1.5f
            val left = x - thickness / 2
            val right = x + thickness / 2
            val top = topY + gy
            val bot = topY + vLen - gy
            val r = thickness / 2
            canvas.drawRoundRect(
                RectF(left, top, right, bot),
                r,
                r,
                if (on) paintGlow.apply { this.color = 0x22FF4400.toInt() } else paintOff)
            canvas.drawRoundRect(RectF(left + 1, top + 1, right - 1, bot - 1), r, r, paint)
        }

        val topY = h * 0.04f
        val midY = h / 2f
        val botY = h * 0.96f - thickness
        val leftX = cx - hLen / 2
        val rightX = cx + hLen / 2

        // a - top horizontal
        drawH(canvas, cx, topY + thickness / 2, segOn(0))
        // b - top-right vertical
        drawV(canvas, rightX - thickness / 2, topY, segOn(1))
        // c - bottom-right vertical
        drawV(canvas, rightX - thickness / 2, midY, segOn(2))
        // d - bottom horizontal
        drawH(canvas, cx, botY + thickness / 2, segOn(3))
        // e - bottom-left vertical
        drawV(canvas, leftX + thickness / 2, midY, segOn(4))
        // f - top-left vertical
        drawV(canvas, leftX + thickness / 2, topY, segOn(5))
        // g - middle horizontal
        drawH(canvas, cx, midY + thickness / 2, segOn(6))

        // dp - decimal point
        val dpR = thickness * 0.55f
        val dpX = rightX + dpR * 1.2f
        val dpY = botY + thickness / 2
        val dpPaint = if (showDp) paintOn else paintOff
        canvas.drawCircle(dpX, dpY, dpR, dpPaint)
    }
}
