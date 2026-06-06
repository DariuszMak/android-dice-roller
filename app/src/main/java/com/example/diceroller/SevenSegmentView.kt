package com.example.diceroller

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class SevenSegmentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    fun getSegmentBits(): Int {
        return segmentBits
    }
    private val DIGIT_SEGMENTS = intArrayOf(
        0x3F, 0x06, 0x5B, 0x4F, 0x66,
        0x6D, 0x7D, 0x07, 0x7F, 0x6F
    )

    private val paintOn = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4400")
        style = Paint.Style.FILL
    }

    private val paintOff = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3A0F00")
        style = Paint.Style.FILL
    }


    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FF4400")
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }


    private val segmentPath = Path()

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


        val t = dw * 0.14f


        paintGlow.strokeWidth = t * 0.25f


        val gap = t * 0.08f


        val left = offsetX + t / 2f
        val right = offsetX + dw - t / 2f
        val top = offsetY + t / 2f
        val bottom = offsetY + dh - t / 2f
        val midY = (top + bottom) / 2f

        fun drawH(x1: Float, x2: Float, y: Float, on: Boolean) {
            segmentPath.reset()
            segmentPath.moveTo(x1 + gap, y)
            segmentPath.lineTo(x1 + gap + t / 2, y - t / 2)
            segmentPath.lineTo(x2 - gap - t / 2, y - t / 2)
            segmentPath.lineTo(x2 - gap, y)
            segmentPath.lineTo(x2 - gap - t / 2, y + t / 2)
            segmentPath.lineTo(x1 + gap + t / 2, y + t / 2)
            segmentPath.close()

            if (on) {
                canvas.drawPath(segmentPath, paintGlow)
                canvas.drawPath(segmentPath, paintOn)
            } else {
                canvas.drawPath(segmentPath, paintOff)
            }
        }

        fun drawV(x: Float, y1: Float, y2: Float, on: Boolean) {
            segmentPath.reset()
            segmentPath.moveTo(x, y1 + gap)
            segmentPath.lineTo(x + t / 2, y1 + gap + t / 2)
            segmentPath.lineTo(x + t / 2, y2 - gap - t / 2)
            segmentPath.lineTo(x, y2 - gap)
            segmentPath.lineTo(x - t / 2, y2 - gap - t / 2)
            segmentPath.lineTo(x - t / 2, y1 + gap + t / 2)
            segmentPath.close()

            if (on) {
                canvas.drawPath(segmentPath, paintGlow)
                canvas.drawPath(segmentPath, paintOn)
            } else {
                canvas.drawPath(segmentPath, paintOff)
            }
        }

        // A (Top)
        drawH(left, right, top, isOn(0))
        // B (Top Right)
        drawV(right, top, midY, isOn(1))
        // C (Bottom Right)
        drawV(right, midY, bottom, isOn(2))
        // D (Bottom)
        drawH(left, right, bottom, isOn(3))
        // E (Bottom Left)
        drawV(left, midY, bottom, isOn(4))
        // F (Top Left)
        drawV(left, top, midY, isOn(5))
        // G (Middle)
        drawH(left, right, midY, isOn(6))

        // Decimal Point
        val dpR = t * 0.45f
        val dpX = right + dpR * 1.6f
        val dpY = bottom
        canvas.drawCircle(dpX, dpY, dpR, if (showDp) paintOn else paintOff)
    }
}